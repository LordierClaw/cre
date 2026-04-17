# Phase 11: Raw Text & XML Output Migration - Context

**Gathered:** 2026-03-29
**Status:** Ready for implementation

<domain>
## Phase Boundary
Migrate the `get_context` and `expand` tool outputs from JSON-wrapped structures to a raw text format using XML tags. This will minimize token usage and improve AI readability.
</domain>

<decisions>
## Implementation Decisions
- **D-01:** The tool response will be a single string.
- **D-02:** Use `<node_id_map>` block at the beginning for short-ID resolution.
- **D-03:** Wrap each file in `<file origin="...">...</file>` tags.
- **D-04:** Retain the hierarchical XML wrapping (`<Anchor>`, `<Class>`) implemented in Phase 10 inside the file blocks.
- **D-05:** Update controllers to return this string directly (text/plain or wrapped in a simple JSON if MCP requires it, but the content itself is raw text).
</decisions>

<canonical_refs>
## Canonical References
- `src/main/java/com/cre/tools/IntegratedViewBuilder.java` — Needs to produce the final string.
- `src/main/java/com/cre/tools/GetContextTool.java` — Needs to return the string.
- `src/main/java/com/cre/mcp/CreController.java` — Needs to handle string return.
</canonical_refs>

---
*Phase: 11-raw-text-migration*
*Context gathered: 2026-03-29*
