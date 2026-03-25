# Phase 3: Expand-on-demand - Context

**Gathered:** 2026-03-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver a bounded `expand(node_id)` capability that widens context slices on demand and supports iterative deepening without token/work explosion.

This phase must:
- preserve deterministic behavior and stable identity semantics from Phases 1-2,
- keep output contract aligned with existing `get_context` consumers,
- replace (or enable replacement of) depth-limit placeholders by returning deeper graph-backed context,
- clearly separate graph-depth expansion from plugin completeness issues (e.g., missing Spring mapping).

</domain>

<decisions>
## Implementation Decisions

### API contract
- **D-01:** `expand` returns the same response shape as `get_context` (full normalized slice contract), not a delta/patch-only payload.
- **D-02:** Initial input contract is minimal: `node_id` only. Bounds and defaults are server-controlled in v1.

### Bounding behavior
- **D-03:** Primary bound is depth-based expansion for v1.
- **D-04:** When a bound is hit, `expand` still succeeds and emits additional structured placeholders for omitted neighbors (fail-soft, not hard error).

### Placeholder replacement model
- **D-05:** Expanding a `depth_limit` placeholder target should include both the original center context and the target expansion (merged/widened slice behavior, not target-only isolation).
- **D-06:** Server does not emit an explicit `replaces[]` contract in v1; it returns deterministic expanded data and the client resolves effective placeholder replacement.

### Determinism and state model
- **D-07:** `expand` is pure deterministic recompute from graph + parameters (no sessionful expansion id in v1).

### Error and semantics handling
- **D-08:** For unknown `node_id`, `expand` follows current fail-soft contract and returns a valid response with `missing_node` placeholder, not a hard tool error.
- **D-09:** `missing_spring_mapping` is not treated as depth-expandable; it reflects plugin/semantic completeness and should not be considered resolved by graph-depth expansion alone.

### Claude's Discretion
- How internal depth defaults are tuned (and whether optional node/edge budgets are introduced as hidden safeguards) is left to planning/implementation, as long as the externally locked decisions above remain true.
- Internal representation for carrying "original center" during merged expansion may evolve, but must preserve deterministic output ordering and stable `node_id` identity.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase boundary and requirements
- `.planning/ROADMAP.md` — Phase 3 goal, success criteria, and dependency on Phase 2
- `.planning/REQUIREMENTS.md` — `EXP-01` requirement definition and v1 boundaries
- `.planning/PROJECT.md` — core value, deterministic-first constraints, and MCP/tool-loop assumptions

### Prior locked decisions
- `.planning/phases/01-core-system/01-CONTEXT.md` — normalized slice contract, placeholder contract, stable `node_id` scheme
- `.planning/phases/02-plugin-system/02-CONTEXT.md` — plugin semantics/evidence gating and `missing_spring_mapping` behavior

### Current implementation anchors
- `src/main/java/com/cre/tools/GetContextTool.java` — current slice building behavior and placeholder emission model
- `src/main/java/com/cre/tools/model/Placeholder.java` — placeholder schema (`kind`, `reason`, `likely_next_tool`, `target_node_id`, `slice_boundary`)
- `src/main/java/com/cre/core/graph/GraphEngine.java` — deterministic graph/evidence state and stable edge ordering
- `src/main/java/com/cre/core/graph/NodeId.java` — stable identity format and parse/serialize contract

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `GetContextTool`: already performs bounded depth-based traversal and emits placeholders compatible with `expand`-style iterative deepening.
- `GraphEngine`: provides deterministic node/edge retrieval and evidence snapshot hooks.
- `NodeId`: stable parseable identity contract suitable as canonical `expand` input.

### Established Patterns
- Deterministic ordering is enforced through sorted nodes/edges and stable IDs.
- Missing context is represented as structured placeholders rather than throwing hard errors.
- Evidence and placeholder contracts are treated as API-level behavior, not incidental implementation details.

### Integration Points
- `expand` should be exposed on the same MCP tool surface alongside `get_context`, `trace_flow`, and `find_implementations`.
- Expansion logic should reuse current graph traversal primitives and placeholder construction patterns to avoid contract drift.

</code_context>

<specifics>
## Specific Ideas

- Keep v1 `expand` low-friction: `node_id` input only, deterministic full-slice response.
- Maintain a fail-soft experience under bounds or unknown nodes so agent workflows remain robust.
- Treat semantic incompleteness (`missing_spring_mapping`) as a distinct class of omission from depth-related omissions.

</specifics>

<deferred>
## Deferred Ideas

- Sessionful expansion IDs / server-managed expansion state are deferred beyond v1.
- Delta/patch response mode (`replaces[]`/diff contracts) is deferred unless proven necessary after evaluation.
- Advanced multi-dimensional budget policy (node+edge+byte caps as first-class API inputs) is deferred.

</deferred>

---
*Phase: 03-expand-on-demand*
*Context gathered: 2026-03-25*
