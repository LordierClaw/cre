# Phase 4: Ranking & Pruning — Research Notes

**Purpose:** What a planning agent needs to know to produce a solid `PLAN.md` for heuristic ranking + deterministic pruning, aligned with `04-CONTEXT.md` and the current codebase.

**Top constraint:** **Determinism first** — same graph + same inputs → identical JSON (ordering, counts, metadata). No `HashSet`/unordered iteration in final scoring paths; tie-break on `node_id` string (D-05); edges tie-break on `(from, to, type)` per existing `GraphEngine.sortedEdges()` ordering.

**API contract:** Preserve `GetContextResponse` top-level fields (`slice_version`, `metadata`, `nodes`, `edges`, `sliced_code`, `placeholders`). Add **only** compact keys under `metadata` (D-06) — do not rename/remove existing evidence keys; extend additively.

---

## Current pipeline (integration points)

| Stage | Location | Notes |
|-------|----------|--------|
| BFS over `CALLS` | `GetContextTool.buildSlice` | Produces `dist` (center → hop depth), depth/budget placeholders, Spring placeholder when incomplete. |
| Node list | `included = dist.keySet().stream().sorted()` | Already **sorted `NodeId`** — good baseline order pre-prune. |
| Edges | `graph.sortedEdges()` filtered by membership | Deterministic global edge order. |
| Sliced code | Skips `FIELD` | Only `METHOD`/`TYPE` with snippets today. |
| Expand | `mergeSlices` + `withExpansionMetadata` | Merged union must get **one** ranking pass on the **final** retained node set (see below). |

**Insertion point:** After the full candidate `included` set is known and **before** building `nodes` / `edges` / `sliced_code` lists — or as a **post-pass** that filters those lists from a full candidate set. Prefer a **single** `applyRankingPruning(Set<NodeId> candidates, NodeId center, …)` so `execute` and `expand` share logic; for `expand`, run pruning **after** `mergeSlices` on merged node ids (not on each half separately), so policy is consistent and telemetry (`retained_count`) matches what the client sees.

**Protected nodes (planner decision):** Lock **center** (query anchor / BFS root) into the retained set so pruning cannot drop the user’s entry. Optionally protect nodes referenced by non-optional placeholders; at minimum, do not drop nodes that would make placeholder `target_node_id` inconsistent with `nodes[]` (align with `shouldKeepPlaceholder` semantics).

---

## Signals available for weighted structural scoring (D-01, D-02)

1. **Depth from center** — Already in `dist`. Natural decay: higher score for smaller `dist` (or affine function with integer arithmetic).
2. **`EdgeType` importance** — Enum exists (`CALLS`, `USES_FIELD`, `BELONGS_TO`, `ENTRY_POINT`, `SERVICE_LAYER`, `DEPENDS_ON`). `GraphEngine` indexes **only** `CALLS` for traversal; other edges live in `sortedEdges()`. Use **filtered scans** over `sortedEdges()` (sorted, deterministic) to count or detect relationships involving candidate nodes — e.g. `SERVICE_LAYER` / `ENTRY_POINT` bonuses for Spring-relevant structure.
3. **Field / variable-style relevance** — `USES_FIELD` edges are emitted in `JavaAstIndexer` from method bodies to `field:…` nodes. Candidates are currently **call-graph methods**; use **outgoing `USES_FIELD` from each candidate method** (count or capped weight) as a static “uses state” signal without expanding BFS into fields unless Phase 4 explicitly adds that (out of scope if it changes slice membership rules).
4. **Graph-local degree** — `outgoingCalls` / `incomingCalls` are sorted; fan-in/fan-out can add bounded integer bonuses (watch double-counting; use deterministic iteration only).

**Implementation note:** Keep all weights as **fixed integers** or **fixed rational steps** compiled into code or `application` config loaded once — no client-supplied tuning in v1 (per `04-CONTEXT.md` specifics).

---

## Pruning policy (D-03, D-04)

- **Top-K + score floor:** Sort candidates by `(score desc, node_id asc)`, take prefix of length ≤ `top_k` where `score >= score_floor`, always including **protected** ids, then re-sort output lists by `node_id` as today.
- **Bounded:** Defaults chosen so worst-case payload stays predictable; reuse spirit of `MAX_EXPAND_NODES` style caps.
- **Fail-soft:** Do not strip `missing_node`, `missing_spring_mapping`, or depth/budget placeholders incorrectly; if a pruned-away node had a `depth_limit` placeholder, either drop that placeholder (node no longer “omitted by depth” in the visible set) or replace with a **single** aggregate policy — planner must document the rule so tests lock it.

---

## Metadata extension (D-06, D-07)

Add under `metadata` (names from context):

- `ranking_version` (string constant, e.g. `cre.rank.v1`)
- `prune_policy` (e.g. `top_k_floor`)
- `top_k`, `score_floor` (numbers; floor can be integer milliscore if avoiding floats)
- `pruned_count`, `retained_count`
- `score_components_used` — compact list or map of **component names** only (no per-node tables)

`expand` already adds `expansion_mode`, `derived_anchor`, `expansion_limit_reason` — ranking keys should coexist.

Avoid floats in JSON if you want bitwise-stable text across JVMs; use **integers** (scaled scores) internally and only expose aggregates.

---

## Likely file touchpoints

| Area | Files |
|------|--------|
| Core tool | `src/main/java/com/cre/tools/GetContextTool.java` — wire prune pass; attach metadata |
| New helper (recommended) | e.g. `com.cre.tools.rank.ContextRanker` or `…rank.RankingPruner` — pure functions, easy to unit test |
| Response | `GetContextResponse.java` — **optional** only if you add a typed sub-record; otherwise keep `Map<String,Object>` metadata (current pattern) |
| Graph | `GraphEngine.java` — optional small helpers (e.g. `edgesIncidentTo(NodeId)` filtered from `sortedEdges()`) to avoid ad-hoc scans; **do not** break `sortedEdges()` contract |
| Tests | `GetContextSchemaTest`, `ExpandToolContractTest`, `ExpandToolDeterminismTest` — extend for new metadata keys + ordering; add focused ranking tests |
| MCP | `CreMcpServer.java` — likely **no** change if tools stay the same |

---

## Risks

| Risk | Mitigation |
|------|------------|
| Non-deterministic ordering if scores tie without lexical break | Always apply D-05 after score sort |
| Pruning drops “important” callees | Conservative defaults; protect center; document floor vs K tradeoff |
| `USES_FIELD` scan cost | One linear pass over `sortedEdges()` per request or indexed adjacency built once per `GraphEngine` snapshot if needed |
| Merge + prune interaction | Prune **after** merge for `expand`; single code path |
| Float/score instability | Integer-only scoring |
| Placeholder / node inconsistency | Explicit rules + tests for `depth_limit` vs pruned nodes |

---

## Test strategy

1. **Determinism:** Extend `ExpandToolDeterminismTest`-style assertions — full JSON tree equality or ordered `node_id` / edge tuple lists for `get_context` and `expand` with ranking enabled.
2. **Contract:** Schema tests assert new `metadata` keys exist and types are correct; existing evidence keys unchanged.
3. **Ranking behavior:** Unit tests on `ContextRanker` with tiny hand-built `GraphEngine` graphs — tied scores → lexical order; floor cuts low scores; top-K truncates; center never dropped.
4. **Regression:** Golden fixture optional — if used, pin **entire** payload hash or canonical JSON string for one fixture graph.
5. **Merge:** One test that `expand` merged result prunes once and metadata counts match `nodes.size()`.

---

## Validation Architecture

Use **three layers** so planning and execution stay aligned with determinism and the response contract:

1. **Unit (pure ranking)** — Feed fixed `GraphEngine` instances (like `ExpandToolBoundsTest`) with known `NodeId`s and `dist` maps; assert retained sets, score ordering, tie-breaks, and integer score math without MCP or filesystem.

2. **Tool integration** — `GetContextTool.execute` / `expand` on `CreContext.defaultFixtureContext()` (and graphs with incomplete Spring semantics) to verify: additive `metadata`; no removal of `slice_version`, `nodes`/`edges`/`sliced_code`/`placeholders` arrays; ordering of arrays still `node_id` / edge tuple sorted after prune; `expand` determinism tests still pass.

3. **Contract / snapshot** — Optional JSON snapshot file for one representative `get_context` response after Phase 4 — guards accidental field renames or reordering of required client-visible structure. Prefer asserting **order-sensitive lists** explicitly over brittle full-file snapshots unless the team wants strict regression locks.

Manual UAT from roadmap (“noise metrics improve”) feeds **Phase 6**; Phase 4 validation should still be **automatable** and **fully deterministic** in CI.

---

## Implementation options (concrete)

| Option | Description | Pros | Cons |
|--------|-------------|------|------|
| **A — Post-pass filter** | Build full candidate slice as today; compute scores; filter `nodes`, `edges`, `sliced_code`; recompute placeholder set per policy | Minimal change to BFS; easy to A/B telemetry vs “unranked” in tests | Full graph still built before prune (work before drop) |
| **B — Rank-first class** | Extract `buildSlice` candidate collection → `RankingPruner.prune(...)` → materialize | Clear separation; testable | Slight refactor of `buildSlice` structure |
| **C — Budget-aware BFS** | Fold ranking into traversal | Could reduce work | Harder to match “top-K of full frontier” semantics; more edge cases |

**Recommendation:** **B** with **A**’s semantics: one explicit pruner module + post-pass filtering of lists, **single** application point after merge for `expand`.

---

## Checklist for `PLAN.md`

- [ ] Integer scoring formula + default `top_k` / `score_floor` / weight table versioned by `ranking_version`
- [ ] Tie-break: `node_id` lexical after score
- [ ] Edge list: include only edges with both endpoints retained; sort by `(from, to, type)`
- [ ] Center (and optional placeholder rules) protected
- [ ] Metadata keys exactly per D-06; no per-node score blobs (D-07)
- [ ] Tests: unit + integration + determinism
- [ ] Confirm no MCP schema / tool signature changes

---
*Research for Phase 04 — ranking-pruning — 2026-03-26*
