# Setting up CRE MCP for Claude Code (Standalone Mode)

This guide explains how to run the Context Reconstruction Engine (CRE) as an independent server and connect Claude Code via the Model Context Protocol (MCP) using HTTP/SSE.

## Benefits of Standalone Mode
- **Persistence**: The server keeps the project index in memory, making subsequent requests much faster.
- **Reliability**: Decouples the server lifecycle from Claude Code's lifecycle.
- **Observability**: You can see server logs in a separate terminal.

## Prerequisites
- Claude Code installed (`npm install -g @anthropic-ai/claude-code`)
- Java 21+ installed
- Maven installed

## Step 1: Start the CRE Server

Run the server in a separate terminal window. You can use Maven directly:

```bash
# In the CRE project root
mvn spring-boot:run
```

By default, the server starts on `http://localhost:8080`. The MCP endpoints are:
- SSE: `http://localhost:8080/mcp/sse`
- Messages: `http://localhost:8080/mcp/messages`

## Step 2: Configure Claude Code

Add the CRE MCP server to your Claude Code configuration. Use the `sse` transport type.

Edit your Claude Code MCP configuration (usually found via `claude mcp add` or in your global config file):

```json
{
  "mcpServers": {
    "cre": {
      "url": "http://localhost:8080/mcp/sse"
    }
  }
}
```

**Note**: If you are using the `claude` CLI, you can often add it by running:
`claude mcp add cre http://localhost:8080/mcp/sse`

## Step 3: Verify Connection

In your Claude Code session, verify the tools are available:
1. Type `/tools` (if supported) or just ask Claude: "What tools do you have from the cre server?"
2. Test it: "get project structure for ."

---

## Alternative: Standard Mode (stdio)

If you prefer NOT to run a separate server, you can use the `stdio` mode:

```json
{
  "mcpServers": {
    "cre": {
      "command": "java",
      "args": [
        "-Dmcp.transport=stdio",
        "-jar",
        "/absolute/path/to/cre/target/cre-0.1.0-SNAPSHOT.jar"
      ]
    }
  }
}
```
