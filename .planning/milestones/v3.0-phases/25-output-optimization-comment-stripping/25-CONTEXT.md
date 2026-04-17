# Phase 25: Output Optimization & Comment Stripping - Context

**Gathered:** 2026-04-17
**Status:** Ready for planning

<domain>
## Phase Boundary

Optimize the context output for large projects by reducing token usage through intelligent comment stripping and improving the physical formatting (line breaks, empty lines) of the returned code blocks.

</domain>

<decisions>
## Implementation Decisions

### Comment Stripping Nuance
- **D-01: Preservation Scope:** Keep Javadocs and all internal comments (`//`, `/* */`) ONLY for "Gathered" nodes (any node shown with its full body).
- **D-02: Class-Level Javadocs:** Strip class-level Javadocs unless the class itself is a "target" (the primary symbol requested).
- **D-03: Skeleton Pruning:** For non-target/non-gathered nodes (skeletons), strip all internal comments and Javadocs.

### Formatting Bloat Control
- **D-04: Line Collapse:** Collapse multiple consecutive empty lines into a single empty line.
- **D-05: Marker Placement:** Place `<omitted_.../>` markers on their own new line, but without surrounding empty lines to maintain density.
- **D-06: Aggressive Trim:** Apply aggressive whitespace trimming to file blocks, but **exclude gathered functions** to preserve their original indentation and style for the agent.
- **D-07: Line Length:** Do not enforce a maximum line length; keep source lines as they appear.

### Query/Traversal Throttling
- **D-08: No Hard Token Limit:** Do not truncate output based on token count; prioritize complete context reconstruction.
- **D-09: Dynamic Node Capping:** Scale the `MAX_GATHER_NODES` limit dynamically based on the requested `depth` to prevent explosion in large projects.
- **D-10: Traversal Priority:** Use Breadth-First Priority to ensure immediate neighbors (depth 1) are always included before deeper branches if a cap is reached.

### Printing Strategy
- **D-11: Preservation over Pretty:** Continue using `LexicalPreservingPrinter` to ensure code accuracy.
- **D-12: Regex Normalization:** Use regex-based post-processing to fix "inline" line break issues and other formatting "bad smells" resulting from node removals.
- **D-13: Hybrid Approach:** Explore using Lexical printing for target/gathered functions while using a cleaner/more efficient method for skeletons if regex cleanup proves insufficient.
- **D-14: Marker Style:** Maintain the current XML-like Tag-style (`<omitted_functions/>`) for clear agent parsing.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Standards
- `.planning/PROJECT.md` — Core value and context reconstruction goals.
- `v2.0-USER-GUIDE.md` — Current token optimization and expansion logic.

### Implementation Context
- `src/main/java/com/cre/core/service/CreServiceImpl.java` — Existing `pruneComments` and `buildIntegratedView` logic.
- `src/main/java/com/cre/core/service/DefaultContextPostProcessor.java` — The hook for regex-based normalization.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `DefaultContextPostProcessor`: The ideal place to implement the regex-based formatting cleanup.
- `LexicalPreservingPrinter`: Currently used in `buildIntegratedView` and needs better cleanup support.

### Established Patterns
- `transformWithRelevance`: The existing logic that decides what members to keep vs. prune.

</code_context>

<specifics>
## Specific Ideas
- The user noted that currently, some lines that are supposed to break return inline, and many empty lines are left where parts were omitted.

</specifics>

<deferred>
## Deferred Ideas
- **Switch to PrettyPrinter:** Deferred the move to a standard pretty printer to maintain original source fidelity via LexicalPreservingPrinter.

</deferred>

---

*Phase: 25-output-optimization-comment-stripping*
*Context gathered: 2026-04-17*
