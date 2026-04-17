# Phase 26: Gap Closure & Integration Fixes - Context

**Gathered:** 2026-04-17
**Status:** Ready for planning
**Mode:** Auto-generated (gap closure from Milestone v3.0 audit)

<domain>
## Phase Boundary

Resolve critical integration gaps identified in the Milestone v3.0 audit:
1. Inconsistent signature normalization between `JavaAstIndexer` and `CreServiceImpl`.
2. Lack of `RecordDeclaration` support in `CreServiceImpl.transformWithRelevance`.

</domain>

<decisions>
## Implementation Decisions

### Signature Normalization
- **D-01: Unified Normalization:** Move signature normalization logic to `AstUtils` or a shared helper. Both the Indexer (during graph creation) and the Service (during reconstruction) must use the same logic to ensure stable IDs.
- **D-02: Normalization Strategy:** Prefer simple names for parameter types (e.g., `String` instead of `java.lang.String`) to ensure compatibility with how JavaParser often resolves types from source code when full symbol resolution is not available or when using simple imports.

### Record Support
- **D-03: Record Transformation:** Update `CreServiceImpl.transformWithRelevance` to explicitly handle `RecordDeclaration`.
- **D-04: Record Pruning:** Apply the same surgical pruning logic to records as to classes (preserve Javadoc and components/methods for gathered nodes, strip for skeletons).

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `AstUtils`: Should host the unified normalization logic.
- `CreServiceImpl.transformWithRelevance`: Needs extension for `RecordDeclaration`.
- `JavaAstIndexer.methodSignature`: The current source of normalized IDs that needs to be externalized.

### Integration Points
- `JavaAstIndexer`: Will use the unified `AstUtils` method.
- `CreServiceImpl`: Will use the unified `AstUtils` method for `targetNodeId` matching and `pruneComments`.

</code_context>

<specifics>
## Specific Ideas
- Fix the `Record` invisibility in the output optimizer to ensure token savings apply to modern Java features.
- Fix the "missing body" bug caused by FQN parameter mismatch (e.g., `List<java.lang.String>` vs `List<String>`).

</specifics>

<deferred>
## Deferred Ideas
- None — this is a focused gap closure phase.

</deferred>

---

*Phase: 26-gap-closure-integration-fixes*
*Context gathered: 2026-04-17*
