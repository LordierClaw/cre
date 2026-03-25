# Phase 3: Expand-on-demand — Research

**Purpose:** Answer *what you need to know to PLAN this phase well* — constraints, open design choices, API/schema split, bounding, semantics, determinism, failure behavior, likely code touchpoints, and test angles.

**Sources:** `.planning/phases/03-expand-on-demand/03-CONTEXT.md` (locked decisions), `.planning/REQUIREMENTS.md` (EXP-01), `.planning/ROADMAP.md` (success criteria), Phases 1–2 context, current `src/main/java` implementation, `.cursor/rules/CRE.md`.

---

## Summary

Phase 3 delivers **bounded `expand(node_id)`** so agents can **deepen** graph-backed context iteratively without token/work explosion. Locked direction: **same JSON shape as `get_context`**, **minimal MCP input (`node_id` only)** with **server-controlled bounds**, **depth as the primary external policy**, **fail-soft success** with **additional placeholders** when limits bite, **pure deterministic recompute** (no expansion session IDs in v1), **fail-soft unknown nodes** (`missing_node` placeholder), and **`missing_spring_mapping` not cleared by depth expansion alone**.

The main engineering work is: (1) **reuse or extract** the same traversal/slicing pipeline as `GetContextTool`, (2) define **exact expansion depth/budget policy** (defaults + internal guardrails), (3) resolve **merged slice semantics** (original center + expanded target) in a **stateless** model—likely requiring **either** a small **placeholder/schema extension** to carry an anchor **or** a **graph-derived** notion of “entry”/path to merge without session state, and (4) register a new MCP tool next to existing stdio tools.

---

## Current system recap (from context + code)

- **Stack:** Java 21, Spring Boot 3.x, Maven, MCP over **stdio** (see `.cursor/rules/CRE.md`).
- **Identity:** `NodeId` is stable, parseable `FQN::member::origin` (`NodeId.parse` / `toString()`).
- **`get_context` today (`GetContextTool`):**
  - Parses `node_id`, returns `GetContextResponse` with `slice_version`, `metadata` (includes `evidence` from `GraphEngine.evidenceSnapshot()`), `nodes`, `edges`, `sliced_code`, `placeholders`.
  - Traversal: BFS over **outgoing `CALLS`** edges up to integer `depth`; nodes collected in a `LinkedHashSet` with distance map (center at 0).
  - **Depth ≤ 0:** includes center only; emits `depth_limit` placeholders for each outgoing call via `Placeholder.expandCallee(target, "calls_out")`.
  - **Spring:** if `!graph.springSemanticsComplete()`, appends `missing_spring_mapping` placeholder (`likely_next_tool`: `expand`, `target_node_id` null).
  - **Unknown node:** returns empty graph lists + `missing_node` placeholder (not a thrown error).
- **Ordering:** `GraphEngine.sortedEdges()` and node iteration via `included.stream().sorted()` keep output deterministic.
- **Phase 2:** Spring semantics are **evidence-gated**; incomplete mapping stays visible via placeholders and `gated_fallback`-style evidence—Phase 3 must **not** treat that as “fixed by going deeper” on the call graph alone.

---

## Key design questions to resolve in planning

1. **Merged slice vs `node_id`-only input (D-05, discussion log)**  
   Locked: expansion is **not target-only**; it should **widen** prior context to include **original center + deeper content around the expansion target**.  
   **Tension:** Stateless `expand(node_id)` does not receive the prior **center** explicitly. Planning must choose one (or combine):
   - **Schema:** Add an optional or required field on **depth-limit placeholders** (or a parallel metadata channel) such as `context_center_node_id` / `anchor_node_id` emitted by `get_context`, so `expand` can union slices deterministically *without* server session state.
   - **Graph-derived anchor:** Infer a stable “entry” or “primary slice root” from Spring `ENTRY_POINT` / role edges (may not generalize to all graphs).
   - **Redefinition:** Treat “merge” as **client-side** union of consecutive tool responses (conflicts with D-05 wording unless clarified).

2. **What “expand” computes relative to `get_context` depth**  
   Is `expand(target)` equivalent to `get_context(target, D_expand)` for a **server default** `D_expand > 0`, or does it mean **“one more hop than whatever produced the placeholder”** (requires remembering prior depth or encoding it)? Locked: **no session**—so either **fixed default depth per expand** or **encode prior depth / desired delta in placeholder** (schema).

3. **Edge types in scope for expansion**  
   Today slicing follows **`CALLS` only**. Phase 2 adds Spring semantic edges (`ENTRY_POINT`, `SERVICE_LAYER`, `DEPENDS_ON`). Planning should decide whether `expand`:
   - stays **CALLS-first** (simplest, matches current slice), or
   - optionally **includes semantic edges** in the same bounded way (aligns with “deeper graph-backed content” for wiring).

4. **Placeholder observability (ROADMAP #3 / PLC-01 alignment)**  
   After expansion, **which** `depth_limit` placeholders disappear vs remain must be **explainable** from the response (nodes/edges present vs new placeholders). No `replaces[]` in v1 (D-06)—plan tests that compare **before/after** snapshots on fixtures.

5. **Relationship between `get_context(..., depth)` and `expand`**  
   Avoid two divergent implementations: prefer **one internal “build slice” function** parameterized by center, effective depth, and budgets.

---

## Proposed API + schema (locked vs discretionary)

### MCP tool: `expand`

| Aspect | Status | Notes |
|--------|--------|--------|
| Tool name `expand` | **Locked** (intent) | Same MCP server, stdio transport. |
| Input: `node_id` string | **Locked** | Minimal v1 surface (D-02 in 03-CONTEXT). |
| Input: budgets/depth from client | **Discretionary / deferred** | Advanced multi-dimensional API deferred; server controls defaults. |
| Output shape | **Locked** | Same as `get_context` → `GetContextResponse` / normalized slice contract (D-01). |
| Unknown `node_id` | **Locked** | Valid response + `missing_node` placeholder (D-08). |
| `missing_spring_mapping` handling | **Locked** | Not resolved by depth expansion semantics alone (D-09); placeholder may still appear. |
| Explicit `replaces[]` / patch payload | **Locked out for v1** | Client infers replacement (D-06). |

### JSON schema (MCP `inputSchema`)

- **Locked minimum:** `{ "type": "object", "properties": { "node_id": { "type": "string" } }, "required": ["node_id"] }`
- **Discretionary:** If planning adds anchor for merge (see above), either bump to optional `anchor_node_id` / `context_center_node_id` **or** keep tool input minimal and **encode anchor only in placeholders from `get_context`** (client passes anchor through—still minimal `node_id` only if anchor is not required).

### Response model

- **Locked:** Reuse `slice_version`, `metadata`, `nodes`, `edges`, `sliced_code`, `placeholders`.
- **Discretionary:** New `metadata` keys (e.g. `expansion_depth_used`, `expansion_node_budget_hit`) for observability—must not break consumers that ignore unknown keys.

---

## Bounding strategy (depth, node/edge budgets, token estimation)

### Locked / agreed

- **Primary bound:** **depth** along the expansion traversal (discussion log + D-03).
- **On limit:** **Succeed** with partial slice + **new** `depth_limit` (or equivalent) placeholders for **omitted** neighbors (D-04).
- **Goal:** Prevent explosion—**refuse or degrade gracefully** (ROADMAP success criterion #2) interpreted as **fail-soft**, not hard errors.

### Discretionary (implementation)

- **Hidden safeguards:** Max **nodes**, **edges**, or **BFS queue size** as a backstop even if depth is high—prevents pathological graphs from exhausting memory/CPU.
- **Token estimation:** Not required for correctness; optional **diagnostic**:
  - Sum of `sliced_code[].text` lengths,
  - Or approximate **character budget** check before including additional hops (could influence **degradation** order: prefer dropping farthest or lowest-priority neighbors first—only if planning introduces priority; otherwise deterministic truncation order must be documented).

### Policy clarity for PLAN.md

- Document **default expand depth** (integer), whether it is **absolute** from target or **relative** “+Δ hops”, and **interaction with `get_context` depth** when both exist.

---

## Placeholder replacement / merged slice semantics

### Locked

- **`depth_limit`:** Deeper traversal should **materialize** previously omitted callees (graph-backed snippets/edges/nodes) **up to bounds**.
- **Merged behavior:** Include **original center context** and **target expansion**, not an isolated subgraph around target only (D-05).
- **No server `replaces[]`:** Client tracks which placeholder prompted `expand` (D-06).
- **`missing_spring_mapping`:** Remains a **semantic completeness** signal; expansion does not imply Spring mapping is complete (D-09).

### Discretionary (must be specified in PLAN)

- **How to represent “what was omitted vs added”** without `replaces[]`:
  - Compare **node_id sets** and **placeholder sets** between responses on deterministic fixtures;
  - Optionally add **non-breaking** `metadata` flags like `expansion_of_target: "<node_id>"` (if useful and still stateless).

- **Placeholder evolution:** If merge requires anchor, extend `Placeholder` or `slice_boundary` conventions carefully—**backward compatibility** for Phase 1 consumers matters.

---

## Determinism + ordering guarantees

### Locked principles

- **No sessionful expansion ID** (D-07): Same graph + same `expand` inputs ⇒ same output.
- **Stable ordering:** Align with `GraphEngine` (sorted edges; sorted node ids in lists).
- **Identity:** Continue using `NodeId` string form everywhere tools accept ids.

### Planning notes

- Any **tie-breaking** when truncating (if budgets apply) must be **deterministic** (e.g., by `NodeId` order).
- Avoid **HashSet iteration order** without sorting (current code sorts for emission—keep that pattern).

---

## Failure modes and graceful degradation

| Situation | Expected behavior (locked where noted) |
|-----------|----------------------------------------|
| Unknown `node_id` | Valid slice response + `missing_node` (D-08); align with `get_context`. |
| Depth/budget reached | **Success** + additional **structured placeholders** for omitted neighbors (D-04). |
| `missing_spring_mapping` | Still emitted when mapping incomplete; **do not** treat as fixed by deeper `CALLS` alone (D-09). |
| Parse errors on `node_id` | Planning: align with `get_context`—either same fail-soft placeholder or documented exception mapping; **prefer consistency**. |
| Tool/MCP handler exceptions | Existing pattern: MCP `isError` path in `CreMcpServer`—decide if `expand` matches `get_context` error behavior exactly. |

---

## Implementation hooks (hypotheses — verify during execution)

| Area | Likely touchpoints |
|------|-------------------|
| MCP registration | `com.cre.mcp.CreMcpServer` — new `expand` tool + handler mirroring `handleGetContext`. |
| Core slice logic | `com.cre.tools.GetContextTool` — extract shared method e.g. `buildSlice(NodeId center, SliceParams params)` or new `ExpandTool` delegating to shared builder. |
| Models | `com.cre.tools.model.GetContextResponse`, `com.cre.tools.model.Placeholder` — possible optional fields for anchor / expansion metadata. |
| Graph | `com.cre.core.graph.GraphEngine` — optional helpers: bounded BFS, neighbor listing for placeholders, optional inclusion of non-`CALLS` edges. |
| Bootstrap / tests | `com.cre.core.bootstrap.CreContext` — fixtures for multi-step get→expand scenarios. |
| Phase 2 integration | Spring plugin / evidence: ensure `evidenceSnapshot()` rules stay coherent when expand widens slice. |

---

## Test strategy ideas (unit / integration)

### Unit

- **Determinism:** Two calls with same `node_id` produce byte-identical JSON (or stable logical equality) for `expand`.
- **Bounds:** Artificial graph or fixture with branching—when depth/budget small, assert **omitted** neighbors appear as **new** `depth_limit` placeholders with correct `target_node_id`.
- **Unknown id:** Response contains `missing_node` and empty/minimal graph lists—**no** thrown parse error surfacing as hard MCP failure if that is the chosen contract.
- **`missing_spring_mapping`:** With plugins/incomplete mapping fixture, `expand` does not flip Spring evidence to “complete” unless mapping actually becomes complete (likely unchanged from `get_context`).

### Integration (MCP or tool-level)

- **Scenario:** `get_context(center, depth=0)` → collect `depth_limit` placeholder → `expand(target)` → assert merged semantics **per chosen PLAN** (nodes/edges contain center ∪ expanded neighborhood).
- **Ordering:** Snapshot tests on sorted `nodes` / `edges` / `sliced_code` node order.
- **Regression:** Phase 1/2 schema tests extended for `expand` tool registration and response shape parity with `get_context`.

---

## What to carry into PLAN.md

1. **Resolved merge strategy** (anchor in placeholder vs graph-derived vs client-only) — single explicit decision.  
2. **Exact traversal spec** (edges included, default depths, internal budgets).  
3. **Observable acceptance tests** mapping to ROADMAP success criteria 1–3 and EXP-01.  
4. **Parity matrix:** `get_context` vs `expand` for unknown nodes, evidence metadata, placeholder kinds.

---

*Research for Phase 3 — Expand-on-demand. Suitable input for `/gsd-plan-phase`.*
