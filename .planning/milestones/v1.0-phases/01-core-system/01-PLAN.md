---
phase: 01-core-system
plan: 01
type: execute
wave: 1
depends_on: []
files_modified: []
autonomous: false
must_haves:
  - CTX-01 structured `get_context` JSON slice contract (nodes/edges + `sliced_code[]` + boundaries + provenance)
  - PLC-01 structured placeholder objects with omission metadata and likely next expand guidance
  - REC-01 deterministic controller → service → implementation reconstruction via AST graph + Spring semantics hooks
  - CONF-01 evidence/provenance categories distinguishing deterministic vs heuristic vs gated fallback
  - IMPL-01 and TRCE-01 tool surface for `find_implementations` and `trace_flow` for controller→service path
---

# Phase 01 — Core System Plan (execute)

## Wave 1 Plans

### Plan Objective

Implement a minimal demo-ready v1 Core System that satisfies Phase 1 success criteria:
- structured `get_context` output contract,
- deterministic reconstruction path grounded in AST/graph,
- structured placeholders with stable `node_id` identity,
- confidence/explainability categories,
- tool endpoints `find_implementations` and `trace_flow`,
- MCP server over stdio with a stable request/response format.

## Tasks

<task>
  <name>01-01: Scaffold Maven + Spring Boot MCP server skeleton</name>
  <files>
    <file>.gitignore</file>
    <file>pom.xml</file>
    <file>src/main/java/com/cre/Application.java</file>
    <file>src/main/java/com/cre/mcp/CreMcpServer.java</file>
    <file>src/main/resources/application.yml</file>
  </files>
  <read_first>
    <file>.planning/PROJECT.md</file>
    <file>.planning/REQUIREMENTS.md</file>
    <file>.planning/phases/01-core-system/01-CONTEXT.md</file>
  </read_first>
  <action>Create a new Maven Spring Boot project (Java 21 / Corretto) with:
1) a `pom.xml` using Spring Boot 3.5.12,
2) main entrypoint `com.cre.Application`,
3) a minimal MCP stdio server bootstrap class `com.cre.mcp.CreMcpServer`,
4) Maven Surefire configured for JUnit 5 tests.

Ensure the MCP server wiring is deterministic and does not require full runtime “request lifecycle” execution for basic tool calls.
  </action>
  <acceptance_criteria>
1) `pom.xml` contains `spring-boot-starter` and `spring-boot-maven-plugin` with Spring Boot version `3.5.12`.
2) `pom.xml` configures the compiler `release` to `21`.
3) `src/main/java/com/cre/mcp/CreMcpServer.java` exists and defines a main bootstrap method for stdio MCP.
4) `src/main/java/com/cre/Application.java` exists in `com.cre` package.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test` (should succeed with zero tests or with placeholder tests).
  </verify>
  <done>
Maven project compiles and starts the MCP server bootstrap without requiring project-specific code beyond placeholders.
  </done>
</task>

<task>
  <name>01-02: Implement AST graph core + node_id identity scheme</name>
  <files>
    <file>src/main/java/com/cre/core/ast/JavaAstIndexer.java</file>
    <file>src/main/java/com/cre/core/graph/GraphEngine.java</file>
    <file>src/main/java/com/cre/core/graph/NodeId.java</file>
    <file>src/main/java/com/cre/core/graph/model/GraphNode.java</file>
    <file>src/main/java/com/cre/core/graph/model/GraphEdge.java</file>
  </files>
  <read_first>
    <file>.docs/ARCHITECURE_SOLUTION.md</file>
    <file>.planning/phases/01-core-system/01-CONTEXT.md</file>
  </read_first>
  <action>
Implement the core AST→graph pipeline using JavaParser, creating:
1) `NodeId` identity-based stable scheme: `(fullyQualifiedType, memberSignature, sourceOrigin)`,
2) `JavaAstIndexer` that parses Java AST and extracts Class/Method/Field nodes,
3) `GraphEngine` that stores edges `CALLS`, `USES_FIELD`, and `BELONGS_TO`,
4) deterministic ordering of nodes/edges when serializing (stable sort by `NodeId`).
  </action>
  <acceptance_criteria>
1) `NodeId` code contains fields consistent with `(fullyQualifiedType, memberSignature, sourceOrigin)` and does not use line/offset as a primary key.
2) `GraphEngine` defines an edge type enum with at least `CALLS`, `USES_FIELD`, `BELONGS_TO`.
3) `JavaAstIndexer` uses JavaParser APIs (e.g., visitor or parsing entrypoint) and creates graph nodes.
  </acceptance_criteria>
  <verify>
Add unit tests that assert NodeId stability: same method across two parses produces equal NodeId.
Run: `mvn -q -DskipITs test`.
  </verify>
  <done>
Core graph can index a test fixture class and returns stable NodeIds.
  </done>
</task>

<task>
  <name>01-03: Implement get_context normalized slice contract + placeholders</name>
  <files>
    <file>src/main/java/com/cre/tools/GetContextTool.java</file>
    <file>src/main/java/com/cre/tools/model/GetContextResponse.java</file>
    <file>src/main/java/com/cre/tools/model/Placeholder.java</file>
    <file>src/test/java/com/cre/tools/GetContextSchemaTest.java</file>
  </files>
  <read_first>
    <file>.planning/phases/01-core-system/01-CONTEXT.md</file>
    <file>.planning/REQUIREMENTS.md</file>
  </read_first>
  <action>
Implement tool logic for `get_context` to return the locked normalized slice contract:
1) include `slice_version`,
2) include `metadata` with evidence categories: deterministic_ast / spring_semantics / heuristic_repair / gated_fallback,
3) include `nodes[]` / `edges[]` (or equivalent normalization),
4) include `sliced_code[]` as payload segments tied to `node_id`,
5) when missing, emit placeholders as structured placeholder objects with:
   - `kind`, `reason`,
   - optional `target_node_id` or `target_identity`,
   - `likely_next_tool` set to `expand` (for Phase 3),
   - `slice_boundary` describing intentional exclusions.
  </action>
  <acceptance_criteria>
1) `GetContextResponse` contains fields for `slice_version` and evidence/provenance categories.
2) `Placeholder` class includes fields `kind`, `reason`, `likely_next_tool`, and a slice boundary field.
3) `GetContextSchemaTest` validates that `get_context` output JSON includes placeholders with required fields when context is intentionally incomplete.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test`.
  </verify>
  <done>
`get_context` returns deterministically shaped JSON and structured placeholders for missing regions.
  </done>
</task>

<task>
  <name>01-04: Implement find_implementations and trace_flow (controller→service)</name>
  <files>
    <file>src/main/java/com/cre/tools/FindImplementationsTool.java</file>
    <file>src/main/java/com/cre/tools/TraceFlowTool.java</file>
    <file>src/test/java/com/cre/tools/TraceFlowToolTest.java</file>
  </files>
  <read_first>
    <file>.docs/ARCHITECURE_SOLUTION.md</file>
    <file>.planning/REQUIREMENTS.md</file>
  </read_first>
  <action>
Implement tool endpoints:
1) `find_implementations` returns implementations for a service/interface type referenced in controller→service traversal.
2) `trace_flow` returns a coherent call-chain representation for the reconstructed entry path.
Ensure both tools rely on the same stable node identity scheme and deterministic traversal ordering.
  </action>
  <acceptance_criteria>
1) `TraceFlowTool` test asserts returned trace contains controller→service call nodes in order.
2) `FindImplementationsTool` returns at least one implementation for an interface in the fixture code.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test`.
  </verify>
  <done>
Tools behave deterministically on fixture code and integrate with the core graph.
  </done>
</task>

<task type="checkpoint">
  <name>01-05: Self-check Phase 1 contract compliance</name>
  <files>
    <file>.planning/phases/01-core-system/01-RESEARCH.md</file>
    <file>.planning/phases/01-core-system/01-CONTEXT.md</file>
  </files>
  <read_first>
    <file>.planning/phases/01-core-system/01-RESEARCH.md</file>
  </read_first>
  <action>
Run automated unit tests and confirm:
1) `get_context` output is schema-compliant (structured slice + metadata),
2) placeholders are structured objects containing likely next tool `expand`,
3) stable node_id identity is used across tools.
  </action>
  <acceptance_criteria>
1) `mvn -q -DskipITs test` exits 0.
2) Tests include assertions for `slice_version`, evidence categories, and placeholder required fields.
  </acceptance_criteria>
  <verify>
Run unit tests.
  </verify>
  <done>
Phase 1 core contract passes automated verification.
  </done>
</task>

