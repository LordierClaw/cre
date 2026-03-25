---
phase: 02-plugin-system
plan: 01
type: execute
wave: 1
depends_on: []
files_modified: []
autonomous: false
must_haves:
  - PLUG-01 core/plugin split with deterministic hardcoded plugin registry (no SPI/dynamic discovery)
  - Spring plugin enriches graph with semantic edges: `ENTRY_POINT`, `SERVICE_LAYER`, and `DEPENDS_ON`
  - Evidence gating: `metadata.evidence.spring_semantics` and `metadata.evidence.gated_fallback` reflect plugin enablement + mapping completeness
  - Missing mapping placeholder contract: `kind: "missing_spring_mapping"`, `likely_next_tool: "expand"`, and non-blank `slice_boundary`
  - Determinism: semantic edge sets are stable across repeated `CreContext` builds
---

# Phase 02 — Plugin System Plan (execute)

## Wave 1 Plans

### Plan Objective

Implement the Spring “plugin layer” that enriches the AST-derived graph with Spring execution semantics (controller/service roles and controller->service wiring), while preserving core behavior and tool contracts when the plugin layer is disabled.

### Tasks

<task>
  <name>02-01: Add plugin interface + deterministic registry + wiring toggle</name>
  <files>
    <file>src/main/java/com/cre/core/bootstrap/CreContext.java</file>
    <file>src/main/java/com/cre/core/plugins/GraphPlugin.java</file>
    <file>src/main/java/com/cre/core/plugins/PluginRegistry.java</file>
    <file>src/main/java/com/cre/core/plugins/SpringSemanticsPlugin.java</file>
  </files>
  <read_first>
    <file>.planning/phases/02-plugin-system/02-CONTEXT.md</file>
    <file>.planning/REQUIREMENTS.md</file>
    <file>.docs/ARCHITECTURE_SOLUTION.md</file>
    <file>.cursor/rules/CRE.md</file>
    <file>src/main/java/com/cre/core/bootstrap/CreContext.java</file>
  </read_first>
  <action>
Implement a deterministic plugin architecture that can enrich the existing `GraphEngine` without hardcoding Spring knowledge into core.

Concretely:
1) Create `com.cre.core.plugins.GraphPlugin` with:
   - method `String pluginId()`
   - method `void enrich(GraphEngine graph, Path javaSourceRoot, List&lt;Path&gt; javaFiles)`
2) Create `com.cre.core.plugins.PluginRegistry` that:
   - registers a deterministic hardcoded plugin list (MVP: only Spring)
   - provides `applyPlugins(GraphEngine graph, Path javaSourceRoot, List&lt;Path&gt; javaFiles, boolean pluginsEnabled)`
   - when `pluginsEnabled == false`, returns without modifying graph semantic state (do not infer anything)
3) Update `com.cre.core.bootstrap.CreContext` so both:
   - `defaultFixtureContext()` and
   - `fromJavaSourceRoot(...)`
   invoke `PluginRegistry.applyPlugins(...)` after AST indexing.
4) Add an overload that supports enable/disable without global side effects:
   - `public static CreContext defaultFixtureContext(boolean pluginsEnabled)`
   - `public static CreContext fromJavaSourceRoot(Path javaSourceRoot, boolean pluginsEnabled, Path... javaFiles)`

5) Create `SpringSemanticsPlugin` as a compilable MVP stub in this task (no semantic edges yet):
   - `SpringSemanticsPlugin implements GraphPlugin`
   - `enrich(...)` is empty for now (actual Spring mapping lands in task 02-03)
  </action>
  <acceptance_criteria>
1) `src/main/java/com/cre/core/plugins/GraphPlugin.java` exists and declares `public interface GraphPlugin` with `pluginId()` and `enrich(GraphEngine, Path, List&lt;Path&gt;)`.
2) `src/main/java/com/cre/core/plugins/PluginRegistry.java` exists and contains a deterministic hardcoded plugin list that includes `new SpringSemanticsPlugin()`.
3) `CreContext.defaultFixtureContext(boolean pluginsEnabled)` exists.
4) `CreContext.fromJavaSourceRoot(Path, boolean, Path...)` exists and both it and `defaultFixtureContext()` call `PluginRegistry.applyPlugins(...)` after indexing.
5) Maven tests still pass: run `mvn -q -DskipITs test` (existing Phase 1 tests must remain green).
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test`
  </verify>
  <done>
Core+plugin wiring is in place and Spring semantics plugin is safely stubbed without breaking existing tests.
  </done>
</task>

<task>
  <name>02-02: Extend graph model with semantic edge types + evidence gating hooks</name>
  <files>
    <file>src/main/java/com/cre/core/graph/model/EdgeType.java</file>
    <file>src/main/java/com/cre/core/graph/GraphEngine.java</file>
    <file>src/main/java/com/cre/tools/GetContextTool.java</file>
  </files>
  <read_first>
    <file>src/main/java/com/cre/core/graph/model/EdgeType.java</file>
    <file>src/main/java/com/cre/core/graph/GraphEngine.java</file>
    <file>src/main/java/com/cre/tools/GetContextTool.java</file>
    <file>.planning/phases/02-plugin-system/02-CONTEXT.md</file>
  </read_first>
  <action>
Extend the core graph model to support Spring semantic edges and to report plugin completeness through evidence, without breaking the existing `get_context` JSON contract.

Concretely:
1) Update `EdgeType` to add:
   - `ENTRY_POINT`
   - `SERVICE_LAYER`
   - `DEPENDS_ON`
2) Update `GraphEngine` to support evidence gating:
   - add internal fields:
     - `boolean springSemanticsPresent`
     - `boolean springSemanticsComplete`
     - `String springSemanticsMissingSliceBoundary`
   - initialize to the Phase 1 behavior for backwards compatibility:
     - `springSemanticsPresent = true`
     - `springSemanticsComplete = true`
     - `springSemanticsMissingSliceBoundary = ""`
   - add methods:
     - `public void springSemanticsState(boolean present, boolean complete, String missingSliceBoundary)`
     - `public boolean springSemanticsComplete()`
     - `public String springSemanticsMissingSliceBoundary()`
3) Update `GraphEngine.evidenceSnapshot()`:
   - set `deterministic_ast` to `true`
   - set `spring_semantics` to `springSemanticsPresent && springSemanticsComplete`
   - set `heuristic_repair` to `false` (Phase 2 does not introduce heuristic repair)
   - set `gated_fallback` to `!springSemanticsComplete`
4) Update `GetContextTool` to continue emitting evidence from `graph.evidenceSnapshot()` (do not change evidence field names/types yet).
  </action>
  <acceptance_criteria>
1) `EdgeType` enum contains `ENTRY_POINT`, `SERVICE_LAYER`, and `DEPENDS_ON`.
2) `GraphEngine.evidenceSnapshot()` sets `spring_semantics` from internal state and sets `gated_fallback` from `!springSemanticsComplete` (not hardcoded).
3) Default execution (current fixture contexts) still satisfies Phase 1 unit tests:
   - `metadata.evidence.spring_semantics` is `true`,
   - `metadata.evidence.gated_fallback` is `false`.
4) Run `mvn -q -DskipITs test` and ensure all existing tests pass.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test`
  </verify>
  <done>
Graph model supports semantic edge types and evidence gating without regressing the current test suite.
  </done>
</task>

<task>
  <name>02-03: Implement Spring semantics plugin (annotations + constructor wiring -> semantic edges)</name>
  <files>
    <file>src/main/java/com/cre/core/plugins/SpringSemanticsPlugin.java</file>
    <file>src/test/java/com/cre/fixtures/UserController.java</file>
    <file>src/test/java/com/cre/fixtures/UserService.java</file>
    <file>src/test/java/com/cre/fixtures/UserServiceImpl.java</file>
  </files>
  <read_first>
    <file>src/main/java/com/cre/core/plugins/SpringSemanticsPlugin.java</file>
    <file>src/main/java/com/cre/core/bootstrap/CreContext.java</file>
    <file>src/main/java/com/cre/core/ast/JavaAstIndexer.java</file>
    <file>src/main/java/com/cre/core/graph/GraphEngine.java</file>
    <file>src/main/java/com/cre/core/graph/GraphEngine.java</file>
    <file>src/test/java/com/cre/fixtures/UserController.java</file>
  </read_first>
  <action>
Implement deterministic Spring annotation mapping and wiring inference to enrich the graph with semantic edges.

Concretely for MVP + the existing fixtures:
1) Parse each input `javaFiles` with JavaParser to find:
   - controller types with annotations named:
     - `Controller` or `RestController`
   - service-layer types with annotations named:
     - `Service`, `Repository`, `Component`, `Bean`
2) For each controller type:
   - Create `ENTRY_POINT` edges from:
     - controller TYPE node to each controller METHOD node
   - Only create edges when both endpoint NodeIds exist in the graph (no guessing).
3) For each service-layer type:
   - Create `SERVICE_LAYER` edges from:
     - service TYPE node to each service METHOD node
4) Wiring inference for `DEPENDS_ON`:
   - Infer controller->service wiring by constructor injection:
     - in controller constructors, detect assignment `this.&lt;field&gt; = &lt;paramName&gt;`
     - treat the constructor parameter type as the injected type
   - create `DEPENDS_ON` only if:
     - the injected type can be resolved to an existing graph `TYPE` NodeId AND
     - that injected type is classified by the plugin as service-layer
5) Determinism & fail-closed:
   - if any controller wiring cannot be deterministically resolved, do not emit those `DEPENDS_ON` edges.
   - call `graph.springSemanticsState(present=true, complete=false, missingSliceBoundary=&lt;non-blank&gt;)`
     in the “incomplete mapping” case.
   - if everything relevant maps deterministically, call:
     `graph.springSemanticsState(present=true, complete=true, missingSliceBoundary="")`.
6) Ensure mapping is stable for the fixture code:
   - for `UserController` constructor injection of `UserService`, produce a `DEPENDS_ON` edge
     from controller TYPE node (`com.cre.fixtures.UserController`) to service TYPE node
     (`com.cre.fixtures.UserService`).
  </action>
  <acceptance_criteria>
1) `SpringSemanticsPlugin` contains annotation-name checks for `Controller`, `RestController`, `Service`, `Repository`, `Component`, and `Bean`.
2) For the default fixture graph:
   - `graph.sortedEdges()` contains at least one edge with `type == ENTRY_POINT`
   - `graph.sortedEdges()` contains at least one edge with `type == DEPENDS_ON`
3) Specifically for `UserController` + `UserService`:
   - there exists a `DEPENDS_ON` edge where `from.fullyQualifiedType() == "com.cre.fixtures.UserController"` and `to.fullyQualifiedType() == "com.cre.fixtures.UserService"`.
4) For incomplete wiring:
   - `graph.springSemanticsComplete() == false`
   - `graph.springSemanticsMissingSliceBoundary()` is non-blank.
5) Run `mvn -q -DskipITs test` and ensure all existing Phase 1 tests pass.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test`
  </verify>
  <done>
Spring semantics plugin enriches the graph with deterministic semantic edges and updates evidence gating state.
  </done>
</task>

<task>
  <name>02-04: Update get_context with missing_spring_mapping placeholders + add plugin tests</name>
  <files>
    <file>src/main/java/com/cre/tools/GetContextTool.java</file>
    <file>src/test/java/com/cre/tools/SpringSemanticsPluginTest.java</file>
    <file>src/test/java/com/cre/tools/PluginsEnabledDisabledTest.java</file>
    <file>src/test/java/com/cre/fixtures/UserControllerMissingServiceMapping.java</file>
    <file>src/test/java/com/cre/fixtures/MissingService.java</file>
    <file>src/test/java/com/cre/fixtures/MissingServiceImpl.java</file>
  </files>
  <read_first>
    <file>src/main/java/com/cre/tools/GetContextTool.java</file>
    <file>src/main/java/com/cre/tools/model/Placeholder.java</file>
    <file>.planning/phases/02-plugin-system/02-CONTEXT.md</file>
    <file>src/test/java/com/cre/tools/GetContextSchemaTest.java</file>
  </read_first>
  <action>
1) Extend `GetContextTool.execute(...)` placeholder generation:
   - after depth-limit placeholders are added (and after any `missing_node` placeholder handling), if `graph.springSemanticsComplete() == false`, append a placeholder:
     - `kind: "missing_spring_mapping"`
     - `reason`: `"Spring semantics mapping incomplete"`
     - `likely_next_tool: "expand"`
     - `slice_boundary`: `graph.springSemanticsMissingSliceBoundary()` (must be non-blank)
2) Ensure evidence fields in `metadata.evidence` come from `graph.evidenceSnapshot()` (already the case after task 02-02).

2) Add tests:
   A) `SpringSemanticsPluginTest` (plugins enabled, default fixtures):
      - call `CreContext.defaultFixtureContext(true)`
      - execute get_context for controller method `com.cre.fixtures.UserController#getUser(String)` with `depth=0`
      - assert JSON `metadata.evidence.spring_semantics == true`
      - assert placeholders contains no placeholder with `kind == "missing_spring_mapping"`
   B) `PluginsEnabledDisabledTest` (plugins disabled):
      - call `CreContext.defaultFixtureContext(false)`
      - execute get_context for controller method with `depth=0`
      - assert `metadata.evidence.spring_semantics == false`
      - assert placeholders contains at least one placeholder with:
        - `kind == "missing_spring_mapping"`
        - `likely_next_tool == "expand"`
        - non-blank `slice_boundary`
   C) Missing mapping completeness test using new fixtures:
      - add `UserControllerMissingServiceMapping` annotated with `@RestController`
      - it should constructor-inject `MissingService` where `MissingService` is NOT annotated with `@Service`/stereotypes
      - build a context including these new fixture sources and run get_context:
        - assert `metadata.evidence.spring_semantics == false`
        - assert placeholders contains `kind == "missing_spring_mapping"`

3) Keep existing Phase 1 tests green:
   - `GetContextSchemaTest` should still observe `spring_semantics == true` for the default fixtures.
  </action>
  <acceptance_criteria>
1) `GetContextTool` appends a placeholder with `kind == "missing_spring_mapping"` and `likely_next_tool == "expand"` when `graph.springSemanticsComplete() == false`.
2) `SpringSemanticsPluginTest` passes and asserts `metadata.evidence.spring_semantics == true` for default fixtures.
3) `PluginsEnabledDisabledTest` passes and asserts:
   - `metadata.evidence.spring_semantics == false`
   - placeholders include a `missing_spring_mapping` placeholder with non-blank `slice_boundary`.
4) Missing mapping test using `UserControllerMissingServiceMapping` passes and asserts:
   - `metadata.evidence.spring_semantics == false`
   - `missing_spring_mapping` placeholder exists.
5) Run `mvn -q -DskipITs test` and ensure full suite passes.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test`
  </verify>
  <done>
`get_context` correctly reports Spring semantic completeness and emits structured placeholders for missing Spring mappings, with full automated test coverage.
  </done>
</task>

<task type="checkpoint">
  <name>02-05: Self-check Phase 2 contract compliance + determinism</name>
  <files>
    <file>src/test/java/com/cre/tools/SpringSemanticsPluginDeterminismTest.java</file>
    <file>.planning/phases/02-plugin-system/02-RESEARCH.md</file>
    <file>.planning/phases/02-plugin-system/02-CONTEXT.md</file>
  </files>
  <read_first>
    <file>src/test/java/com/cre/tools/SpringSemanticsPluginDeterminismTest.java</file>
    <file>src/main/java/com/cre/core/graph/GraphEngine.java</file>
    <file>src/main/java/com/cre/core/graph/model/EdgeType.java</file>
  </read_first>
  <action>
Add a determinism test that ensures plugin-enriched semantic edges are stable across repeated context builds.

Concretely:
1) Build two contexts with plugins enabled:
   - `CreContext.defaultFixtureContext(true)` twice
2) Compare:
   - `graph.sortedEdges()` equality (or at least semantic edges subset equality)
   - ensure the `DEPENDS_ON` edge between `UserController` and `UserService` exists in both.
3) Ensure NodeIds remain stable for semantic edges (no identity drift).
  </action>
  <acceptance_criteria>
1) Determinism test asserts equality of `graph.sortedEdges()` across two builds OR asserts equality of the filtered semantic edge set for edge types `ENTRY_POINT`, `SERVICE_LAYER`, and `DEPENDS_ON`.
2) Run: `mvn -q -DskipITs test` and ensure it passes.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test`
  </verify>
  <done>
Phase 2 plugin semantics are deterministic and stable across repeated runs.
  </done>
</task>

