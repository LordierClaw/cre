# Phase 2 — Research

**Phase:** 02
**Phase Name:** Plugin System
**Researched:** 2026-03-25
**Confidence:** MEDIUM

## Scope (Phase Boundary)

Deliver a Spring-aware plugin layer that enriches the existing AST-backed graph with execution-relevant semantics (controller/service roles and controller->service wiring), while keeping the core graph behavior and tool contracts stable when the plugin layer is disabled.

This phase must:
- Add a deterministic plugin architecture (hardcoded registry) so core never depends on framework-specific logic.
- Implement a minimal Spring semantics plugin that:
  - maps Spring stereotypes (`@Controller`/`@RestController`, `@Service`, plus MVP `@Repository`, `@Component`, `@Bean`),
  - infers wiring from `@Autowired` when present (MVP) and from constructor-based injection when `@Autowired` is absent,
  - emits semantic edges as first-class graph edges (not only metadata).
- Extend evidence + placeholder gating so `get_context` can report semantic completeness via `metadata.evidence.spring_semantics` and emit structured placeholders when Spring mappings are missing.

## Locked Decisions Impacting Planning

Carried from `.planning/phases/02-plugin-system/02-CONTEXT.md`:

1. Plugin discovery/registration uses a deterministic hardcoded registry.
2. Spring semantics are represented as new graph edge types (extensible role/wiring edges).
3. Spring annotation mapping is deterministic and fail-closed:
   - Never guess missing mappings.
   - When mapping is incomplete, keep core edges and report missing semantics via evidence + structured placeholders.
4. Evidence gating:
   - deterministically mapped semantics => `spring_semantics` true and semantic edges emitted,
   - incomplete mapping => `spring_semantics` false (or missing) and `gated_fallback` true for the affected slice.

## Implementation Research Notes (for planning)

### 1. Plugin API shape

The codebase currently has:
- `CreContext.defaultFixtureContext()` which builds an in-memory `GraphEngine` via `JavaAstIndexer`.
- `GetContextTool` which formats graph evidence via `graph.evidenceSnapshot()` and generates placeholders for depth-limited traversal.

Therefore, Phase 2 should implement plugin enrichment inside `CreContext` (after AST indexing, before tool usage), so both:
- runtime hosting (MCP server uses `CreContext.defaultFixtureContext()`),
- tests that build fixtures directly
share the same plugin wiring.

### 2. Edge model extension

Phase 1’s `EdgeType` currently has:
- `CALLS`, `USES_FIELD`, `BELONGS_TO`

To satisfy the Phase 2 boundary, introduce at least:
- `ENTRY_POINT` (controller stereotype -> entry semantics; granularity is planner-chosen as long as deterministic),
- `SERVICE_LAYER` (service stereotype -> service semantics),
- `DEPENDS_ON` (controller entry or controller type -> service type; MVP should support controller constructor injection).

This ensures semantic edges are visible to downstream slicing/serialization (since `GetContextTool` serializes all edges whose endpoints are in the slice).

### 3. Spring annotation mapping rules (MVP)

Stereotypes to support:
- `@Controller` and `@RestController` => controller semantics
- `@Service` => service-layer semantics
- `@Repository`, `@Component`, `@Bean` => service-layer semantics (MVP usefulness)

Wiring inference:
- If `@Autowired` is present (MVP):
  - infer injected types from annotated constructor parameters (or field injection if needed later).
- If `@Autowired` is absent:
  - infer constructor-based injection by detecting assignment patterns in the controller constructor:
    - parameter `<T> x` assigned to field `this.y = x`
    - injected type must map deterministically to an existing graph `TYPE` node

Fail-closed:
- Only create `DEPENDS_ON` when the injected type can be resolved to an existing graph `TYPE` node AND is classified as service-layer by this plugin.

### 4. Evidence + placeholder gating

`GetContextTool` currently:
- sets evidence from `graph.evidenceSnapshot()` (hardcoded booleans in Phase 1),
- emits placeholders only for `missing_node` and `depth_limit`.

Phase 2 should:
- update `GraphEngine.evidenceSnapshot()` to reflect plugin enrichment results,
- update `GetContextTool` to append `missing_spring_mapping` placeholders when Spring semantic completeness is false.

Placeholder contract to preserve:
- placeholders must be structured `Placeholder` objects,
- include `likely_next_tool: "expand"`,
- include non-blank `slice_boundary` explaining what was omitted.

### 5. Determinism checks

Evidence + semantic edges must remain stable across:
- repeated graph builds from the same sources,
- repeated planning/execution passes that re-run fixture indexing.

Therefore tests should compare:
- Node identity stability (`NodeId` is already identity-based; Phase 2 adds more edges),
- semantic edge sets and ordering (`graph.sortedEdges()`).

## Validation Architecture (Nyquist / Dimension 8)

Create automated tests that verify:
1. `EdgeType` includes all Phase 2 semantic edges.
2. With plugins enabled (default fixture context):
   - `get_context` evidence `spring_semantics == true` for `UserController.getUser(String)`,
   - at least one semantic edge exists (`ENTRY_POINT` and `DEPENDS_ON` expected on fixture).
3. With plugins disabled:
   - `get_context` evidence `spring_semantics == false`,
   - `GetContextTool` emits at least one placeholder of `kind == "missing_spring_mapping"` with:
     - `likely_next_tool == "expand"`,
     - non-blank `slice_boundary`.
4. Determinism:
   - `graph.sortedEdges()` output is identical across two independent `CreContext` builds with plugins enabled.

---
*Researched: 2026-03-25*

