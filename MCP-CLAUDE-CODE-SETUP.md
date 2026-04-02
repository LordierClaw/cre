# Setting up CRE MCP for Claude Code

This guide explains how to integrate the Context Reconstruction Engine (CRE) with Claude Code using the Model Context Protocol (MCP).

## Prerequisites

- Claude Code installed (`npm install -g @anthropic-ai/claude-code`)
- Java 21+ installed
- Maven installed
- CRE project built (`mvn clean package`)

## Setup Instructions

1.  **Locate the CRE JAR**:
    Ensure you have the CRE JAR file available. By default, it's located at `target/cre-0.1.0-SNAPSHOT.jar`.

2.  **Configure Claude Code**:
    Add the CRE MCP server to your Claude Code configuration. You can do this by editing your `claude-code.config.json` or by using the `claude mcp add` command if available.

    If adding manually, add the following to your MCP configuration:

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

    **Note**: Replace `/absolute/path/to/cre/` with the actual absolute path to your CRE project.

3.  **Verify Connection**:
    Restart Claude Code. You should see `cre` listed in the available MCP servers. You can test it by asking Claude to "get project structure for ." or "get context for a class".

## Usage in Claude Code

Once connected, Claude can use the following tools:

- `get_project_structure`: Visualize the project layout.
- `get_file_structure`: See the skeleton of a specific file or class.
- `get_context`: Get a deep, reconstructed context slice for a specific symbol.
- `expand`: Deeper exploration of a specific node.

## Persistence Note

The `project_root` argument is now automatically converted to an absolute path. This ensures that the indexing and context remain stable even if you change directories within Claude Code.
