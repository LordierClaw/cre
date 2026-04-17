# Phase 4: Ranking & Pruning - Context

**Gathered:** 2026-03-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Improve `get_context`/`expand` signal-to-noise using deterministic ranking and pruning over existing graph-backed slices, without introducing new MCP capabilities or breaking current response shape contracts.

This phase focuses on:
- heuristic scoring/prioritization of candidate context,
- deterministic pruning of lower-value context,
- preserving placeholder/evidence semantics and API compatibility from prior phases.

</domain>

<decisions>
## Implementation Decisions

### Scoring Model Inputs
- **D-01:** Use **weighted structural scoring** as the baseline model.
- **D-02:** Score is derived from deterministic signals (edge-type importance, depth weighting/decay, and variable/field-use style relevance signals).

### Pruning Policy
- **D-03:** Apply **Top-K + minimum score floor** pruning.
- **D-04:** Pruning must remain deterministic and bounded, and must not invalidate fail-soft contracts from prior phases.

### Determinism and Ordering
- **D-05:** When scores tie, use stable lexical tie-break by `node_id`; for edges use stable edge tuple ordering.

### Observability / Response Contract
- **D-06:** Expose **compact ranking metadata** only, including `ranking_version`, `pruned_count`, `retained_count`, `prune_policy`, `score_floor`, `top_k`, and high-level `score_components_used`.
- **D-07:** Do not add verbose per-node score breakdowns in v1 phase scope; keep payload growth controlled.

### Claude's Discretion
- Exact numeric weight values and initial defaults are left to planner/researcher discretion, as long as behavior remains deterministic, testable, and aligned with the locked policy choices above.
- Specific metric thresholds for "noise reduction improvement" may be tuned during plan/test design.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase boundary and project constraints
- `.planning/ROADMAP.md` — Phase 4 goal/success criteria and scope boundary
- `.planning/PROJECT.md` — core value, deterministic-first direction, evaluation constraints
- `.planning/REQUIREMENTS.md` — active v1 constraints and traceability context

### Prior locked contracts (must preserve)
- `.planning/phases/01-core-system/01-CONTEXT.md` — normalized slice contract, placeholder contract, stable identity semantics
- `.planning/phases/02-plugin-system/02-CONTEXT.md` — Spring semantics and evidence-gating behavior
- `.planning/phases/03-expand-on-demand/03-CONTEXT.md` — bounded expansion, merged behavior, fail-soft placeholder replacement rules

### Current implementation anchors
- `src/main/java/com/cre/tools/GetContextTool.java` — current slice construction, placeholder emission, and expand merge behavior
- `src/main/java/com/cre/core/graph/GraphEngine.java` — deterministic edge/node retrieval and graph traversal primitives
- `src/main/java/com/cre/tools/model/GetContextResponse.java` — response shape to preserve while adding compact metadata
- `src/main/java/com/cre/tools/model/Placeholder.java` — placeholder schema and semantics

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `GetContextTool` already centralizes context assembly for `get_context` and `expand`; this is the natural insertion point for scoring/pruning.
- `GraphEngine` provides deterministic sorted traversals and indexed call relations suitable for deterministic ranking signals.

### Established Patterns
- Deterministic output ordering (sorted nodes/edges/sliced_code) is already a hard pattern.
- Fail-soft semantics via structured placeholders (`missing_node`, `depth_limit`, `missing_spring_mapping`) are part of stable behavior.
- Metadata is already an evidence map and can be extended with compact ranking telemetry.

### Integration Points
- Ranking/pruning logic should integrate before response materialization in `GetContextTool` so both `get_context` and `expand` benefit consistently.
- `CreMcpServer` should require no new tool shape; behavior changes should remain behind existing tool endpoints.

</code_context>

<specifics>
## Specific Ideas

- Keep ranking configuration deterministic and server-controlled in v1 (no user-provided scoring params yet).
- Favor observable, compact telemetry over verbose explainability payloads.

</specifics>

<deferred>
## Deferred Ideas

- Per-node verbose score breakdown in response payloads (candidate for later diagnostic phase).
- Optional AI-assisted pruning mode (explicitly optional in roadmap; not locked for this phase).
- End-user configurable scoring profiles.

</deferred>

---
*Phase: 04-ranking-pruning*
*Context gathered: 2026-03-26*
