# Phase 5: Advanced Plugins - Context

**Gathered:** 2026-03-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Extend the plugin ecosystem for real-world readiness by adding a first advanced plugin capability while preserving deterministic graph semantics, core/plugin separation, and existing MCP response contracts.

For this phase, scope is locked to **exception-flow plugin** delivery first.

</domain>

<decisions>
## Implementation Decisions

### Advanced Plugin Scope
- **D-01:** Implement **exception-flow plugin** first in Phase 5.
- **D-02:** Do not include event-flow or domain-rule plugins in this phase unless needed as minimal supporting test scaffolding.

### Expected Outcome Shape
- **D-03:** Exception-flow semantics must be represented as deterministic graph enrichment outputs consumable by existing slice and trace tooling.
- **D-04:** Plugin behavior remains additive and must not break existing Spring plugin semantics or Phase 4 ranking/pruning metadata behavior.

### Claude's Discretion
- Concrete edge names and exact exception-flow mapping granularity are left to planner/researcher, as long as they are deterministic, testable, and backward-compatible.
- Whether plugin registration remains hardcoded or gains minimal config-based extensibility can be decided during planning if it does not cause scope creep.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase boundary and constraints
- `.planning/ROADMAP.md` — Phase 5 goal and success criteria
- `.planning/PROJECT.md` — core value and deterministic-first constraints
- `.planning/REQUIREMENTS.md` — baseline requirement context and compatibility constraints

### Prior phase decisions that constrain Phase 5
- `.planning/phases/02-plugin-system/02-CONTEXT.md` — plugin registry, determinism, evidence and placeholder behavior
- `.planning/phases/03-expand-on-demand/03-CONTEXT.md` — fail-soft and merge/expand contract expectations
- `.planning/phases/04-ranking-pruning/04-CONTEXT.md` — ranking/pruning and compact metadata compatibility requirements

### Current implementation anchors
- `src/main/java/com/cre/core/plugins/GraphPlugin.java` — plugin interface contract
- `src/main/java/com/cre/core/plugins/PluginRegistry.java` — plugin registration path
- `src/main/java/com/cre/core/plugins/SpringSemanticsPlugin.java` — existing plugin implementation pattern
- `src/main/java/com/cre/core/graph/model/EdgeType.java` — graph edge taxonomy extension point
- `src/main/java/com/cre/tools/GetContextTool.java` — output shaping, placeholders, metadata interactions

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `GraphPlugin` interface already provides a clear plugin hook for enrichment.
- `PluginRegistry` already centralizes deterministic plugin execution order.
- `SpringSemanticsPlugin` provides a concrete reference implementation for deterministic annotation-driven enrichment.

### Established Patterns
- Plugins enrich graph post-indexing and before tool response serialization.
- Deterministic edge ordering and fail-soft evidence/placeholder behavior are existing non-negotiable patterns.
- Metadata evolution is additive (no breaking schema changes).

### Integration Points
- New exception-flow plugin should integrate through `PluginRegistry` and produce graph/evidence outputs consumable by `GetContextTool`.
- Any new edge types must fit existing deterministic ordering and filtering behavior in graph and tool pipelines.

</code_context>

<specifics>
## Specific Ideas

- Prioritize a clear, deterministic exception-flow MVP that is easy to verify on fixtures.
- Keep plugin interactions with ranking/pruning explicit to avoid hidden behavior coupling.

</specifics>

<deferred>
## Deferred Ideas

- Event-flow plugin implementation (future phase candidate)
- Domain-rule/custom business plugin implementation (future phase candidate)

</deferred>

---
*Phase: 05-advanced-plugins*
*Context gathered: 2026-03-26*
