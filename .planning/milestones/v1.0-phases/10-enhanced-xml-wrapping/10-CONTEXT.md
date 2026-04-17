# Phase 10: Enhanced XML Context Wrapping - Context

**Gathered:** 2026-03-28
**Status:** Ready for planning

<domain>
## Phase Boundary
This phase refines the output format of the `get_context` and `expand` tools to match a semantic XML-wrapped structure. The goal is to provide clear boundaries for files, classes, and omitted sections to improve LLM scanability and future subagent processing.

</domain>

<decisions>
## Implementation Decisions

### Output Structure: Multi-Layer Wrapping
- **D-01:** Wrap the entire code block of each file in a tag named `ClassName.MemberName` (e.g., `<LoanService.borrowBook>`).
- **D-02:** Wrap the class definition itself (annotations + body) in a tag named after the `ClassName` (e.g., `<LoanService>`).
- **D-03:** The package declaration and imports stay inside the outer anchor tag but outside the class tag.

### Semantic Grouping (Single Tag per Category)
- **D-04:** **Imports:** If any imports are pruned, use a single `<ommitted_import/>` tag at the end of the import block.
- **D-05:** **Properties:** If any fields are pruned, use a single `<ommitted_properties/>` tag where the first pruned field was located.
- **D-06:** **Functions:** If any methods are pruned (that are not individually tagged as call-site omissions), use a single `<ommitted_functions/>` tag at the end of the class body.

### Descriptive Omissions
- **D-07:** For pruned method call expressions *within* retained code, use `<ommitted_code id="ommitted_NN" description=""/>`.
- **D-08:** The `description` attribute must be present but left empty (`""`).
- **D-09:** IDs must be short sequential strings (e.g., `ommitted_01`) mapped in the existing `node_id_map`.

### Formatting
- **D-10:** Tags must be on their own lines and follow the indentation of the code they replace.

</decisions>

<canonical_refs>
## Canonical References

### Core Implementation
- `src/main/java/com/cre/tools/IntegratedViewBuilder.java` — Needs refactoring to support grouping and multi-layer wrapping.
- `src/main/java/com/cre/tools/GetContextTool.java` — Needs to pass the anchor symbol name to the builder.

### Tests
- `src/test/java/com/cre/tools/IntegratedViewBuilderTest.java` — Update to verify the new hierarchical XML structure.
- `src/test/java/com/cre/e2e/RealProjectE2ETest.java` — Verify E2E with the refined format.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `IntegratedViewBuilder`: Uses JavaParser AST transformation. The current logic replaces individual members; it needs to be updated to track "first pruned field" and "has pruned methods" to emit single category tags.

### Integration Points
- `GetContextTool`: Knows the `NodeId` of the anchor. It can derive the `ClassName.MemberName` string for the D-01 wrap.

</code_context>

---

*Phase: 10-enhanced-xml-wrapping*
*Context gathered: 2026-03-28*
