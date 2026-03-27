# Phase 8: HTTP/SSE & REST Support

## Goal

Transition the MCP server from a Stdio-only process to a persistent Spring Boot application supporting both HTTP/SSE and REST endpoints. This ensures the memory cache (`ProjectManager`) stays alive across sessions.

## Decisions

- **REST API**: Expose `/api/get_context`, `/api/expand`, etc., accepting `project_root` in the body.
- **SSE API**: Support standard MCP SSE at `/mcp/sse` and `/mcp/messages`.
- **Persistence**: Application runs as a standard Spring Boot web app (`mvn spring-boot:run`).
- **Cache**: 2-hour TTL maintained in `ProjectManager`.

## Dependencies

- Phase 7: Real-project Ingestion (Completed)

## Key Files

- `src/main/java/com/cre/mcp/McpConfig.java`
- `src/main/java/com/cre/mcp/CreController.java`
- `src/main/java/com/cre/Application.java`
