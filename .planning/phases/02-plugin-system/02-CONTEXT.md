# Phase 2: Plugin System - Context

**Gathered:** 2026-03-25
**Status:** Ready for planning

<domain>
## Phase Boundary
Deliver the Spring “plugin layer” that enriches the AST-derived graph with Spring execution semantics (controller/service/wiring roles) while keeping the core graph indexing and tool output contracts stable.

This phase must:
- Introduce a plugin interface + wiring so Spring semantics can be added without hard-coding framework knowledge into the core graph builder.
- Enrich the graph deterministically based on Spring annotations.
- Preserve core behavior when the plugin layer is disabled (core remains buildable/testable).
</domain>

<decisions>
## Implementation Decisions

### Plugin discovery/registration
- **D-01:** Use an explicit, hardcoded plugin registry (deterministic initial set) rather than dynamic discovery (SPI) for Phase 2.
- **D-02:** Centralize plugin registration so both runtime hosting (`CreMcpServer`) and fixture graph construction (`CreContext.defaultFixtureContext()`) can share the same plugin list. Tests/tooling that build a graph directly should still have Spring semantics available when the plugin layer is enabled.
- **D-03:** Provide an “enabled/disabled” switch for plugins. When plugins are disabled, the graph remains the Phase 1 core graph and slice semantics fall back to core evidence only (no Spring-derived edges).

### Graph enrichment representation
- **D-04:** Represent Spring semantics as new graph edge types (extensible role/wiring edges), not just metadata/evidence.
- **D-05:** Add a minimal edge set for the Spring plugin:
  - `ENTRY_POINT`: links controller semantics to entry methods (or controller type to its methods, depending on final granularity chosen by the planner).
  - `SERVICE_LAYER`: links service stereotypes to service methods (or service type to its methods).
  - `DEPENDS_ON`: links wiring from controller (or entry method) to service type.

### Spring annotation mapping rules
- **D-06:** Broaden “service-layer” mapping beyond only `@Service`:
  - `@Controller` / `@RestController` => entry/controller semantics
  - `@Service` => service-layer semantics
  - Additionally map `@Repository`, `@Component`, and `@Bean` to service-layer semantics for Phase 2 MVP purposes.
- **D-07:** Wiring semantics:
  - If `@Autowired` is present, use it to infer `DEPENDS_ON`.
  - If `@Autowired` is not present, infer constructor-based injection when the controller/type has a constructor parameter that is assigned to a field (common Spring convention). Only create `DEPENDS_ON` when the injected parameter type can be deterministically mapped to a graph `TYPE` node that is already classified as a service-layer type by the same plugin.
- **D-08:** Never guess missing mappings. If the plugin cannot resolve an annotated stereotype or wiring target to a graph node deterministically, it must treat that part as missing (see evidence gating below) rather than emitting potentially wrong edges.

### Determinism & evidence gating
- **D-09:** Use a hybrid policy with explicit fallback tiering:
  - Deterministically mapped Spring semantics => set `spring_semantics` evidence to `true` and emit the corresponding role/wiring edges.
  - When Spring mapping is incomplete => keep Phase 1 core edges/traversal, set `spring_semantics` to `false` or “missing” for the affected slice (implementation choice for representation), and set `gated_fallback` to indicate the slice contains semantic omissions.
- **D-10:** When mapping is incomplete, emit structured placeholders for missing Spring semantics that follow the Phase 1 placeholder contract:
  - placeholder `kind`: `missing_spring_mapping`
  - placeholder `likely_next_tool`: `expand`
  - include a `slice_boundary` describing what was omitted (e.g., “wiring target service type not resolved deterministically”).

### Claude's Discretion
- ENTRY_POINT / SERVICE_LAYER granularity (type-to-type vs type-to-method vs controller-method selection) is left to the planner, as long as it is deterministic, stable across reloads, and supports downstream slicing/traceability.
- For evidence “missing” representation, the planner may choose a boolean (`false` for missing) or a partial indicator encoded into the existing evidence map keys, but must keep the Phase 1 response shape stable.

</decisions>

<specifics>
## Specific Ideas
- This phase should reuse Phase 1’s existing graph + stable `node_id` identity scheme so that annotation-derived edges remain stable across later `expand` calls.
- The Phase 1 placeholder contract is the enforcement mechanism: missing Spring semantics must turn into structured placeholders that point to `expand` so downstream agents can request more context safely.
- Current fixture code already provides an MVP Spring signal surface:
  - `src/test/java/com/cre/fixtures/UserController.java` uses `@RestController` and constructor injection
  - `src/test/java/com/cre/fixtures/UserServiceImpl.java` uses `@Service`
  - These should drive deterministic Spring semantics during Phase 2 MVP enrichment.
</specifics>

<canonical_refs>
## Canonical References
**Downstream agents MUST read these before planning or implementing.**

### Phase boundary & requirements
- `.planning/ROADMAP.md` — Phase 2 goal and PLUG-01 scope anchor
- `.planning/REQUIREMENTS.md` — PLUG-01 acceptance criteria and “core/plugin” constraints
- `.planning/PROJECT.md` — core constraints (Java/Spring, deterministic->heuristic->AI fallback, tool-loop shape)

### Prior decisions
- `.planning/phases/01-core-system/01-CONTEXT.md` — Phase 1 slice contract, placeholder contract, and stable `node_id` rules

### Architecture & risk guidance
- `.docs/WORKPLAN.md` — Phase 2 deliverables (plugin interface, hook into AST & graph, Spring basic plugin)
- `.docs/ARCHITECURE_SOLUTION.md` — plugin contract concept: “plugins annotate roles/edges in the graph”
- `.planning/research/STACK.md` — pinned stack assumptions and compatibility notes
- `.planning/research/PITFALLS.md` — plugin contract drift + identity drift pitfalls and prevention guidance
- `.planning/research/FEATURES.md` — Spring plugin as a dependency for deterministic reconstruction

### Workflow enforcement
- `.cursor/rules/CRE.md` — “do work via GSD workflows” and overall project constraints
</canonical_refs>

<code_context>
## Existing Code Insights
### Reusable Assets
- `src/main/java/com/cre/core/graph/GraphEngine.java`: in-memory nodes/edges store with deterministic ordering; evidence snapshot hook exists
- `src/main/java/com/cre/core/ast/JavaAstIndexer.java`: deterministic AST-to-graph extraction for types/methods/fields and CALLS/USES_FIELD/BELONGS_TO edges
- `src/main/java/com/cre/core/bootstrap/CreContext.java`: fixture graph construction (defaultFixtureContext) and in-memory wiring for tool tests
- `src/main/java/com/cre/tools/GetContextTool.java`: normalized slice JSON output + evidence categories + placeholder contract + evidence map emission
- `src/main/java/com/cre/mcp/CreMcpServer.java`: stdio MCP server hosting + tool registration patterns

### Established Patterns
- Deterministic ordering:
  - nodes/edges are emitted via stable sorting by `NodeId` and edge tuples
  - traversal uses stable edge ordering (sorted CALLS edges)
- Stable identity:
  - graph node identity is derived from `(fullyQualifiedType, memberSignature, sourceOrigin)`
  - no AST span/offset as primary identity
- Placeholders are structured objects with `likely_next_tool: "expand"` in Phase 1 outputs

### Integration Points
- Spring plugin should run after AST indexing and before slicing/tool serialization so it can enrich the same graph instance used by `GetContextTool`.
- `GetContextTool` obtains evidence from `GraphEngine.evidenceSnapshot()`; Phase 2 should update this evidence based on whether Spring semantics edges were deterministically produced for the slice.
- The Spring plugin should extend the graph’s `EdgeType` enum with Phase 2 semantics edges while keeping core edge types intact.
</code_context>

<deferred>
## Deferred Ideas
- Additional Spring stereotypes beyond the MVP set (`@RequestMapping`/`@Transactional`/`@Qualifier`) are deferred to Phase 5 advanced plugins.
- Plugin registration via dynamic SPI/service discovery is deferred (we chose explicit hardcoded registry for determinism).
- More granular wiring inference (profiles, multi-constructor resolution rules, qualifiers) is deferred until after Phase 2 MVP validation.
- Ranking/pruning integration and “evidence-weighted traversal” are deferred to Phase 4.
</deferred>

---
*Phase: 02-plugin-system*
*Context gathered: 2026-03-25*

