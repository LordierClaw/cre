# Phase 5 Research: Advanced Plugins (exception-flow first)

**Purpose:** Answer *“What do I need to know to PLAN this phase well?”* for **exception-flow** as the first advanced plugin, aligned with `.planning/phases/05-advanced-plugins/05-CONTEXT.md`, while preserving deterministic behavior, additive MCP contracts, and compatibility with Phase 4 ranking/pruning.

---

## Goals and constraints (from planning)

- **Scope lock:** Ship **exception-flow** first; defer event-flow and domain-rule plugins unless they are trivial test scaffolding only.
- **Output shape:** Enrichment must appear as **deterministic graph data** (edges/nodes already in the model) consumable by **`get_context` / `expand`** (edges in the slice JSON) and **`trace_flow`** where applicable.
- **Non-regression:** Spring plugin semantics, Phase 4 **ranking/pruning** metadata, and **fail-soft** placeholder behavior must remain stable; new behavior is **additive**.
- **Roadmap tension:** `ROADMAP.md` Phase 5 success criteria mention **registration without editing core** (SPI/config). Phase 2 locked **hardcoded `PluginRegistry`** for determinism. Planning must either (a) extend the registry list in one place and defer SPI to a follow-up slice, or (b) introduce minimal SPI/config **without** changing slice JSON schema—either path should be explicit in the plan.

---

## Current architecture (what the code actually does)

### Plugin hook

- `GraphPlugin` is a single method: `enrich(GraphEngine, Path javaSourceRoot, List<Path> javaFiles)` after AST indexing (`CreContext.fromJavaSourceRoot`).
- `PluginRegistry.applyPlugins` runs a **fixed ordered list** (today: `SpringSemanticsPlugin` only) and respects `pluginsEnabled` (when disabled, Spring state is set and the loop is skipped).

### Graph edges and traversal

- **CALLS** edges are indexed in `GraphEngine` for `outgoingCalls` / `incomingCalls` and drive:
  - BFS in `GetContextTool.buildSlice` and `computeDistancesWithin`
  - DFS in `TraceFlowTool`
- **Non-CALLS** edges (e.g. `ENTRY_POINT`, `SERVICE_LAYER`, `DEPENDS_ON`, `BELONGS_TO`, `USES_FIELD`) are stored and appear in **`sortedEdges()`** and thus in **`get_context` edge lists** when both endpoints survive pruning, but they **do not** extend BFS along the primary “call” frontier unless separately wired.

**Planning implication:** Exception-flow edges should almost certainly **not** reuse `CALLS` unless you intentionally want them to behave like normal calls in BFS/trace (usually wrong for catch/handler semantics). Prefer **new `EdgeType` values** for explicit semantics.

### Ranking / pruning (Phase 4)

- `RankingPruner` walks **`graph.sortedEdges()`** and applies **incident bonuses** for `CALLS`, `ENTRY_POINT`, `SERVICE_LAYER`, `DEPENDS_ON`, and `USES_FIELD` (with caps). **Unknown edge types contribute no type bonus** (they still appear in the graph JSON if endpoints are retained).
- Any new exception-related edge type needs a **conscious scoring decision**: zero bonus (annotation-only), or a fixed milliscore bonus documented alongside existing weights in `RankingPruner` javadoc—**deterministic** and **test-backed**.

### Evidence and placeholders

- `GraphEngine.evidenceSnapshot()` is a **small fixed map** (`deterministic_ast`, `spring_semantics`, `heuristic_repair`, `gated_fallback`). Spring uses `springSemanticsState` + placeholders for incomplete mapping.
- For exception-flow, the same pattern applies: **additive** evidence keys and/or **structured placeholders** when analysis is incomplete—without removing or renaming existing keys expected by tests (e.g. `GetContextSchemaTest`).

---

## Implementation options (choose one MVP + optional stretch)

### Option A — Intra-method try/catch → handler linkage (static)

**Idea:** Parse `TryStmt` / `CatchClause` in each `MethodDeclaration`; for each catch block, resolve **method calls** inside the catch body (reuse resolution ideas from `JavaAstIndexer`) and emit edges from the **enclosing method** (or try-region) to **callee methods** with a new edge type (e.g. `EXCEPTION_HANDLER_CALL` or `CATCH_INVOKES`).

- **Pros:** Fits existing method `NodeId`s; deterministic if call resolution matches indexer rules; visible in slice edges when both methods are in the retained set.
- **Cons:** Does not model “exception type X flows to handler Y” without naming conventions; empty catch blocks add nothing.

### Option B — Declared `throws` + type graph

**Idea:** From method `throws` clauses, add edges from method to **exception type** nodes or to known `Exception` subtypes if resolvable (may require new `NodeKind` or reusing `TYPE` nodes).

- **Pros:** Simple, deterministic for declared checked exceptions.
- **Cons:** Weak for runtime-only types; may need policy for generic/wildcard throws.

### Option C — Spring `@ControllerAdvice` / `@ExceptionHandler` (annotation-first)

**Idea:** Similar to Spring plugin: link **controller entry** or **failing service methods** to **global handler methods** when types can be matched deterministically.

- **Pros:** High value for Spring Boot apps; aligns with product constraints.
- **Cons:** Type matching for `Exception` hierarchies must be strictly defined to stay deterministic; may overlap with Spring plugin responsibilities (coordinate ordering: run Spring plugin first, then exception plugin).

### Option D — Hybrid MVP (recommended for planning discussion)

Combine **C** (if fixtures are Spring-based) with a minimal **A** (catch block calls) for unit-level tests. Defer full exception-type propagation across inheritance until a later iteration.

---

## Risks and mitigations

| Risk | Mitigation |
|------|------------|
| **BFS never reaches** exception handlers that are only linked by non-CALLS edges | Document in plan: either accept “edges visible when endpoints already included via CALLS”, or add **placeholders** (`kind` + `slice_boundary`) when a deterministic handler exists outside the slice; avoid silent omission. |
| **Ranking ignores** new edges | Add explicit weights or document intentional zero weight; add tests when weights are non-zero. |
| **Double-parse / drift** from indexer | Prefer sharing **JavaParser** `CompilationUnit` patterns consistently with `SpringSemanticsPlugin` (parse per file, deterministic iteration order over `javaFiles`). |
| **Ordering nondeterminism** | Follow `GraphEngine.sortedEdges()` / `NodeId` lexicographic tie-breaks; mirror `SpringSemanticsPluginDeterminismTest` for new edges. |
| **Scope creep (SPI)** | Gate SPI behind “if roadmap criterion #2 is mandatory for this milestone”; otherwise ship **registry list + tests** and track SPI as 05-xx follow-up. |
| **`trace_flow` semantics** | Today traces **CALLS only**. Exception plugin edges will **not** appear in trace unless `TraceFlowTool` is extended—call out in plan whether Phase 5 includes trace updates or only `get_context` edges. |

---

## Concrete file touchpoints

| Area | Files | Notes |
|------|-------|--------|
| Edge taxonomy | `src/main/java/com/cre/core/graph/model/EdgeType.java` | Add enum values; preserve stable ordering if anything depends on `name()` ordering in JSON (currently `e.type().name()`). |
| Plugin | New `src/main/java/com/cre/core/plugins/ExceptionFlowPlugin.java` (name TBD in plan) | Implement `GraphPlugin`; parse with JavaParser; `addEdge` with new types only. |
| Registry | `src/main/java/com/cre/core/plugins/PluginRegistry.java` | Append plugin in **fixed order** after Spring (or document dependency). |
| Ranking | `src/main/java/com/cre/tools/rank/RankingPruner.java` | Extend `aggregateBonuses` + javadoc weights for new `EdgeType`s if needed. |
| Evidence | `src/main/java/com/cre/core/graph/GraphEngine.java` | Optional: `exceptionFlowState(...)` mirroring Spring, or generalize evidence later—**additive** keys only. |
| Tools | `src/main/java/com/cre/tools/GetContextTool.java` | Only if placeholders or traversal rules change; default is **no change** if edges are display-only. |
| Trace | `src/main/java/com/cre/tools/TraceFlowTool.java` | Only if product wants exception paths in `trace_flow` in this phase. |
| Bootstrap | `src/main/java/com/cre/core/bootstrap/CreContext.java` | No change if plugins only go through `PluginRegistry` (already invoked). |
| MCP | `src/main/java/com/cre/mcp/CreMcpServer.java` | Only if tool descriptions or capabilities change. |

**Test resources:** Add or extend fixtures under `src/test/java/com/cre/fixtures/` (e.g. controller with try/catch, or `@ControllerAdvice`) and mirror patterns in `SpringSemanticsPluginDeterminismTest`, `GetContextSchemaTest`, `ContextRankingScoringTest`.

---

## Test strategy

1. **Determinism:** Two builds from the same sources → identical sorted exception-related edges (same pattern as `SpringSemanticsPluginDeterminismTest`).
2. **Graph invariants:** New edges never break `sortedEdges()` ordering contract; `NodeId` strings stable vs Phase 1 indexing.
3. **Ranking:** If new weights: unit tests for score stability and tie-breaks (`ContextRankingScoringTest` style); if zero weight: document and optionally assert **no score change** on graphs with only core+Spring edges.
4. **MCP/schema:** Extend or add assertions for **additive** `metadata.evidence` keys if introduced; keep existing keys (`deterministic_ast`, `spring_semantics`, etc.) intact (`GetContextSchemaTest`).
5. **Plugins flag:** Reuse `PluginsEnabledDisabledTest` pattern—disabled run must not emit exception plugin edges (and evidence must remain consistent with policy).
6. **Fixtures:** Prefer small, readable Java sources; avoid flaky ordering from HashSet iteration—use deterministic loops and sorted collections when aggregating.

---

## Validation Architecture

This section names **how** the planner should prove the phase without breaking deterministic/additive contracts.

1. **Contract layers**
   - **Graph contract:** Enumerated `EdgeType`s, deterministic `sortedEdges()` / node order, no duplicate edge identity beyond what `GetContextTool` already dedupes by `(from, to, type)`.
   - **Tool JSON contract:** `slice_version`, `metadata.evidence` shape, ranking fields from Phase 4 unchanged in meaning; new fields only **added**.
   - **Placeholder contract:** New placeholder `kind`s follow the same record shape as `Placeholder` (`kind`, `reason`, `likely_next_tool`, `target_node_id`, `slice_boundary`).

2. **Automated validation matrix**

   | Layer | Primary tests / artifacts |
   |-------|---------------------------|
   | Plugin isolation | New unit/integration test class for exception edges + `CreContext.defaultFixtureContext` or dedicated fixture context |
   | Determinism | Repeated `CreContext` build comparison on fixed file list |
   | Ranking interaction | `RankingPruner` tests when new edge weights ≠ 0; pruning smoke on merged slices if placeholders reference pruned nodes |
   | Regression | Existing `GetContextSchemaTest`, `ExpandTool*` tests, `SpringSemantics*` tests remain green |

3. **Manual / review gates (short)**
   - Confirm **roadmap** wording on “registration without core edits” vs actual delivery (registry edit vs SPI).
   - Confirm **trace_flow** story documented if unchanged.

4. **Definition of done (for research handoff to PLAN)**
   - Chosen MVP option (A–D) with explicit **non-goals** for this phase.
   - Edge type names + whether they affect **BFS**, **ranking**, **trace**, and **evidence** each marked yes/no.
   - Fixture list and test class names sketched for implementation PR.

---

## What the planning agent should decide explicitly

1. **MVP graph semantics:** Which edges exist for the first shippable exception-flow plugin, and whether handlers outside the call-expanded neighborhood require **placeholders**.
2. **Registration model:** Hardcoded list extension vs minimal SPI—match roadmap expectations.
3. **Ranking weights:** Numeric bonuses for new `EdgeType`s or explicit “zero bonus, visibility only.”
4. **`trace_flow`:** In scope or explicitly deferred.
5. **Evidence keys:** Whether incomplete exception analysis surfaces as `gated_fallback`-style semantics or new evidence booleans—**additive only**.

---

*Research for Phase 05 — advanced-plugins — exception-flow first. Last updated: 2026-03-26.*
