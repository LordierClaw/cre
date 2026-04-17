# Phase 09: Output Format Optimization - Context

**Gathered:** 2026-03-28
**Status:** Ready for planning

<domain>
## Phase Boundary
This phase is about optimizing the `get_context` tool's output format. The goal is to move away from the current verbose, multi-list JSON structure toward a more compact, integrated, and readable representation (file-centric) that uses XML-style placeholders for omitted code.

</domain>

<decisions>
## Implementation Decisions

### Output Structure: File-centric Integrated View
- **D-01:** The output will group code by file, reconstructing the class structure (package, imports, class/field declarations) for included nodes.
- **D-02:** Multiple methods from the same file will be shown within their shared class context.
- **D-05:** Remove the redundant `nodes` and `edges` lists from the response; the integrated code and a `node_id_map` are sufficient for both humans and agents.

### Placeholder Integration: Block/Call Replacement Tags
- **D-03:** Use a single unified tag type `<ommitted_NN/>` for all omissions.
- **D-04:** Replace omitted methods, fields, and individual method call expressions (when the callee is omitted) with these XML-style placeholders.
- **D-06:** Provide a `node_id_map` in the response to resolve short IDs (like `ommitted_01`) back to their full NodeId (FQN/signature) for subsequent `expand` or `trace_flow` calls.

### Human/Agent Readability
- **D-07:** The entire response must remain valid JSON. The structure should be easy for a human to scan and for an LLM to parse and reason about.
- **D-08:** Omit full paths of classes in the code snippets; use simple names where possible, relying on the `package` declaration and imports for context.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Core Implementation
- `src/main/java/com/cre/tools/GetContextTool.java` — Core logic for building the context response.
- `src/main/java/com/cre/tools/model/GetContextResponse.java` — Current response model (to be updated).
- `src/main/java/com/cre/core/ast/JavaAstIndexer.java` — Source extraction and node indexing logic.
- `src/main/java/com/cre/core/graph/model/GraphNode.java` — Data structure for nodes and snippets.

### Tests
- `src/test/java/com/cre/tools/GetContextSchemaTest.java` — Schema validation tests.
- `src/test/java/com/cre/e2e/RealProjectE2ETest.java` — End-to-end verification.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `JavaAstIndexer`: Can be extended or used to identify exact locations for placeholder embedding.
- `RankingPruner`: Continues to provide the list of nodes to include/omit; its output will drive the integrated view construction.

### Established Patterns
- The current `GetContextTool` uses a BFS traversal (execute) or anchor derivation (expand) to gather nodes. This logic remains sound; only the formatting of the gathered nodes changes.

### Integration Points
- `CreController`: The entry point for tool calls. It will need to handle the new `GetContextResponse` structure.

</code_context>

<specifics>
## Specific Ideas
- The `node_id_map` should be a simple mapping from the short sequential IDs (e.g., `ommitted_01`) used in the XML tags to the full canonical NodeId strings.

</specifics>

<deferred>
## Deferred Ideas
- None — the discussion was tightly focused on the output format optimization.

</deferred>

---

*Phase: 09-output-format-optimization*
*Context gathered: 2026-03-28*
