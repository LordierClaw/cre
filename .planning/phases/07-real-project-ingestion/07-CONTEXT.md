# Phase 7: Real-project Ingestion

## Goal

Automate project-wide source discovery and indexing from a directory root, support multi-project scenarios with a 24/7 runtime, and verify E2E on a real target project (`/home/hainn/blue/code/cre-test-project`).

## Decisions

- **ProjectManager**: Introduce a singleton to manage multiple `CreContext` instances.
- **TTL Cache**: Implement a 2-hour memory cache for project contexts (LRU or simple map with timestamps).
- **MCP Tool Arguments**: All tools (`get_context`, `expand`, etc.) will accept a `project_root` argument.
- **Manual Reset**: Add a `reset_project` tool to force re-indexing.
- **Source Discovery**: Prioritize `src/main/java`; fallback to all `.java` files excluding `target`, `build`, `.git`, etc.
- **E2E Testing**: Verify multi-project isolation and cache behavior on `cre-test-project`.

## Dependencies

- Phase 5: Advanced Plugins (Core system stable and enriched)

## Key Files

- `src/main/java/com/cre/core/bootstrap/ProjectManager.java` (New)
- `src/main/java/com/cre/core/bootstrap/CreContext.java`
- `src/main/java/com/cre/mcp/CreMcpServer.java`
- `src/test/java/com/cre/e2e/RealProjectE2ETest.java` (New)
