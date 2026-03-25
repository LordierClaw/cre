---
phase: 03-expand-on-demand
plan: 01
type: execute
wave: 1
depends_on: []
files_modified: []
autonomous: false
requirements: [EXP-01]
must_haves:
  - Add MCP tool `expand` with input schema requiring `node_id` and response shape matching `GetContextResponse` from `get_context`
  - `expand` uses merged-slice behavior: include original center context and expanded target context in a single stateless response
  - Expansion is bounded by deterministic server-side defaults and internal hard limits (depth + node budget)
  - Anchor selection prefers nearest reachable `ENTRY_POINT` target, then deterministic lexicographic tie-break
  - Reverse traversal is indexed in `GraphEngine` (no per-request full edge scan for incoming calls)
  - Bounded expansion degrades gracefully by emitting structured `depth_limit` placeholders with `target_node_id` and `slice_boundary`
  - Unknown node handling remains fail-soft via `missing_node` placeholder (no hard MCP error for this case)
  - `missing_spring_mapping` remains governed by semantic completeness; `expand` must not treat it as resolved via graph-depth expansion alone
  - Expansion keeps deterministic ordering for nodes, edges, and sliced_code across repeated runs
---

# Phase 03 - Expand-on-demand Plan (execute)

## Wave 1 Plans

### Plan Objective

Implement a bounded, deterministic `expand(node_id)` path that reuses existing slice contracts from `get_context`, returns deeper graph-backed context without resource blowups, and keeps placeholder semantics observable for iterative deepening.

### Tasks

<task>
  <name>03-01: Introduce shared slice builder with explicit expansion bounds</name>
  <files>
    <file>src/main/java/com/cre/tools/GetContextTool.java</file>
    <file>src/main/java/com/cre/core/graph/GraphEngine.java</file>
  </files>
  <read_first>
    <file>.planning/phases/03-expand-on-demand/03-CONTEXT.md</file>
    <file>.planning/phases/03-expand-on-demand/03-RESEARCH.md</file>
    <file>src/main/java/com/cre/tools/GetContextTool.java</file>
    <file>src/main/java/com/cre/core/graph/GraphEngine.java</file>
    <file>src/test/java/com/cre/tools/GetContextSchemaTest.java</file>
  </read_first>
  <action>
Refactor `GetContextTool` so both existing `execute(nodeIdRaw, depth)` and new expansion logic use one internal deterministic method with concrete parameters:
1) center node id string,
2) max depth,
3) max included nodes hard limit.

Set concrete defaults/constants in `GetContextTool`:
- `DEFAULT_EXPAND_DEPTH = 2`
- `MAX_EXPAND_NODES = 64`

Implementation details:
- Keep BFS traversal over `graph.outgoingCalls(cur)` only.
- Keep deterministic order by sorting included node ids before materializing `nodes` and `sliced_code`.
- When `max depth` or `max nodes` limit stops traversal, emit `Placeholder.expandCallee(...)` for omitted outgoing calls so expansion remains fail-soft and observable.
- Preserve unknown-node behavior exactly: `missing_node` placeholder with `likely_next_tool = "expand"` and `slice_boundary = "unknown_node"`.
- Add deterministic reverse traversal helper(s) in `GraphEngine` for incoming `CALLS` edges only (e.g. `incomingCalls(NodeId)` sorted by `from`) backed by an indexed structure (for example `Map<NodeId, List<GraphEdge>>`) to avoid full edge scans per request.
  </action>
  <acceptance_criteria>
1) `src/main/java/com/cre/tools/GetContextTool.java` contains constants `DEFAULT_EXPAND_DEPTH` and `MAX_EXPAND_NODES` with values `2` and `64`.
2) `GetContextTool` contains a shared internal method used by both `execute(` and expansion entrypoint (grep for both methods calling same private builder method name).
3) The shared builder emits `depth_limit` placeholders when traversal is truncated by depth or node budget (grep for `Placeholder.expandCallee(` in truncation paths).
4) Existing `missing_node` placeholder contract is still present with `likely_next_tool` equal to `expand`.
5) `GraphEngine` exposes sorted incoming `CALLS` traversal used by anchor derivation logic.
6) `GraphEngine` incoming traversal path uses an index/map and does not rebuild by scanning all edges on each `expand` call.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test`
  </verify>
  <done>
Context slicing and expansion use one deterministic bounded implementation with explicit fail-soft truncation behavior.
  </done>
</task>

<task>
  <name>03-02: Add expand execution path and MCP tool registration</name>
  <files>
    <file>src/main/java/com/cre/tools/GetContextTool.java</file>
    <file>src/main/java/com/cre/mcp/CreMcpServer.java</file>
  </files>
  <read_first>
    <file>src/main/java/com/cre/mcp/CreMcpServer.java</file>
    <file>src/main/java/com/cre/tools/GetContextTool.java</file>
    <file>.planning/phases/03-expand-on-demand/03-CONTEXT.md</file>
  </read_first>
  <action>
Expose expansion as a dedicated MCP tool:
1) Add `public GetContextResponse expand(String nodeIdRaw)` to `GetContextTool`.
2) Implement merged-slice semantics in `expand(...)`:
   - derive anchor center deterministically from graph state only (no request metadata, no persisted session): walk reverse graph using incoming `CALLS` edges only from `nodeIdRaw` and collect reachable nodes,
   - from that reachable set, select candidates that are `to` endpoints of `ENTRY_POINT` edges; choose the shortest-path candidate from `nodeIdRaw`; if multiple candidates have the same shortest path length, choose lexicographically smallest `NodeId`; if no candidate exists, anchor is `nodeIdRaw`,
   - build center slice for anchor node id and expansion slice for `nodeIdRaw`,
   - return union of both slices with de-duplicated nodes/edges/sliced_code by node id + edge tuple,
   - compute placeholders from merged slice and remove any `depth_limit` placeholder whose `target_node_id` is already present in merged `nodes`,
   - merge placeholders by `(kind,target_node_id,slice_boundary)` dedup key and preserve non-depth placeholders (including `missing_spring_mapping`) unless source graph semantics are complete.
3) Set metadata observability fields:
   - `metadata.expansion_mode = "merged"` when derived anchor differs from target; `target_only_fallback` when anchor equals target,
   - `metadata.derived_anchor = "<node_id>"` for the selected anchor,
   - `metadata.expansion_limit_reason = "depth" | "node_budget" | "none"` for the final response,
   - preserve evidence fields from `graph.evidenceSnapshot()` so `spring_semantics` / `gated_fallback` remain aligned with Phase 2 contracts.
4) Register MCP tool `expand` in `CreMcpServer` using schema:
   `{"type":"object","properties":{"node_id":{"type":"string"}},"required":["node_id"]}`
5) Add `handleExpand(...)` in `CreMcpServer` matching existing JSON serialization path (`JSON.writeValueAsString(...)`) and returning `CallToolResult` text payload.
6) Keep error-path style consistent with other handlers: runtime exceptions use `isError(true)` with message text.
  </action>
  <acceptance_criteria>
1) `CreMcpServer` includes a tool named exactly `expand`.
2) The `expand` tool input schema requires `node_id` and does not require `depth`.
3) `CreMcpServer` contains a `handleExpand(` method that calls `GetContextTool.expand(`.
4) `GetContextTool` contains a public `expand(String nodeIdRaw)` method returning `GetContextResponse`.
5) `GetContextTool.expand(...)` sets `metadata.expansion_mode` to either `merged` or `target_only_fallback`.
6) Anchor derivation code uses only graph traversal and deterministic tie-break (`lexicographically smallest NodeId`) and does not read request/session metadata.
7) Union logic de-duplicates nodes by `node_id` and edges by `(from,to,type)` tuple before response serialization.
8) Placeholder merge/removal rule is implemented: no `depth_limit` placeholder remains for a `target_node_id` that already exists in response `nodes`.
9) Anchor selection code prioritizes shortest-path `ENTRY_POINT` candidate before lexicographic tie-break.
10) Response metadata includes `derived_anchor` and `expansion_limit_reason`.
11) If `graph.springSemanticsComplete() == false`, merged expand responses still include `missing_spring_mapping` placeholder and evidence remains `spring_semantics=false`, `gated_fallback=true`.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test`
  </verify>
  <done>
`expand(node_id)` is available on MCP surface and returns the same response shape class as `get_context`.
  </done>
</task>

<task>
  <name>03-03: Add automated tests for expand contract, bounds, and determinism</name>
  <files>
    <file>src/test/java/com/cre/tools/ExpandToolContractTest.java</file>
    <file>src/test/java/com/cre/tools/ExpandToolDeterminismTest.java</file>
    <file>src/test/java/com/cre/tools/ExpandToolBoundsTest.java</file>
  </files>
  <read_first>
    <file>src/test/java/com/cre/tools/GetContextSchemaTest.java</file>
    <file>src/test/java/com/cre/tools/PluginsEnabledDisabledTest.java</file>
    <file>src/test/java/com/cre/tools/SpringSemanticsPluginDeterminismTest.java</file>
    <file>src/main/java/com/cre/tools/GetContextTool.java</file>
    <file>src/main/java/com/cre/core/bootstrap/CreContext.java</file>
  </read_first>
  <action>
Create explicit tests that map to EXP-01 and roadmap success criteria:
1) `ExpandToolContractTest`:
   - call `expand` on a valid fixture node id,
   - assert response has `slice_version`, `metadata.evidence`, `nodes`, `edges`, `sliced_code`, `placeholders`.
2) `ExpandToolBoundsTest`:
   - build/choose a fixture chain where expansion truncates at depth/node limits,
   - assert at least one placeholder has `kind == "depth_limit"`, non-null `target_node_id`, and non-blank `slice_boundary`.
3) `ExpandToolDeterminismTest`:
   - call `expand` twice with same node id and same context,
   - assert stable ordering/equality of `nodes`, `edges`, and `sliced_code` node ids.
4) Unknown node case:
   - assert `expand("unknown::node::id")` does not throw and includes `missing_node` placeholder.
5) Merged-slice case:
   - create/get a depth-0 baseline response with a depth-limit placeholder target and known center,
   - call `expand` for that target,
   - assert expanded response still contains baseline center node id and also contains the target node id as concrete node content,
   - assert `metadata.derived_anchor` exists and equals the chosen center for that scenario.
  </action>
  <acceptance_criteria>
1) New tests exist at:
   - `src/test/java/com/cre/tools/ExpandToolContractTest.java`
   - `src/test/java/com/cre/tools/ExpandToolBoundsTest.java`
   - `src/test/java/com/cre/tools/ExpandToolDeterminismTest.java`
2) One test asserts `missing_node` placeholder for unknown `node_id`.
3) One test asserts bounded expansion yields `depth_limit` placeholder with non-null `target_node_id` and non-blank `slice_boundary`.
4) `mvn -q -DskipITs test` exits 0.
5) One test asserts merged-slice behavior by checking both baseline center node id and expanded target node id are present in one `expand` response.
6) One test asserts metadata fields `derived_anchor` and `expansion_limit_reason` are present and use expected values.
7) One test with incomplete Spring mapping fixture asserts `expand` response still contains `missing_spring_mapping` and evidence remains `spring_semantics=false` with `gated_fallback=true`.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test`
  </verify>
  <done>
Expand behavior is regression-protected for schema parity, bounded degradation, and deterministic results.
  </done>
</task>

<task type="checkpoint">
  <name>03-04: Verify placeholder observability and Phase 3 success criteria mapping</name>
  <files>
    <file>.planning/phases/03-expand-on-demand/03-CONTEXT.md</file>
    <file>.planning/phases/03-expand-on-demand/03-PLAN.md</file>
    <file>src/test/java/com/cre/tools/ExpandToolContractTest.java</file>
    <file>src/test/java/com/cre/tools/ExpandToolBoundsTest.java</file>
  </files>
  <read_first>
    <file>.planning/ROADMAP.md</file>
    <file>.planning/REQUIREMENTS.md</file>
    <file>.planning/phases/03-expand-on-demand/03-CONTEXT.md</file>
  </read_first>
  <action>
Run tests and perform explicit criteria mapping:
1) Confirm success criterion #1 by asserting expansion returns deeper graph-backed content for a placeholder target.
2) Confirm success criterion #2 by asserting bounded behavior emits structured placeholders instead of crashing/exploding.
3) Confirm success criterion #3 by documenting before/after placeholder and node-set observability in test assertions/comments.
  </action>
  <acceptance_criteria>
1) `mvn -q -DskipITs test` exits 0.
2) `ExpandToolContractTest` contains an assertion that expanding a placeholder target increases `nodes` count or `sliced_code` count compared to a depth-0 baseline for the same fixture entrypoint.
3) `ExpandToolBoundsTest` contains assertions that bounded expansion returns at least one placeholder with `kind == "depth_limit"`, non-null `target_node_id`, and non-blank `slice_boundary`.
4) `ExpandToolContractTest` contains an assertion that a specific depth-limit placeholder target chosen from the depth-0 baseline appears as a concrete node in the post-expand response (`nodes.node_id` contains that target).
5) `ExpandToolContractTest` contains an assertion that the exact depth-limit placeholder for the expanded target is absent in post-expand placeholders (kind+target pair removed), proving placeholder replacement observability.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test`
  </verify>
  <done>
Phase 3 execution can proceed with concrete, verifiable outcomes tied to EXP-01.
  </done>
</task>

