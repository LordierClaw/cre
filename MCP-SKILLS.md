# CRE MCP Agent Skills

This document provides guidance on how to use the CRE MCP server effectively to discover source code knowledge.

## Core Principles

1.  **Start Broad**: Use `get_project_structure` to see the overall layout. This helps you identify the main packages and layers.
2.  **Skeletal Discovery**: Use `get_file_structure` to see the methods and properties of a class without getting overwhelmed by method bodies.
3.  **Context Slicing**: Use `get_context` to get a deep, reconstructed context for a specific class or method. This tool is your primary way to understand dependencies and data flow.
4.  **Iterative Deepening**: Use `expand` to explore deeper into a specific `node_id`. If `get_context` has omitted certain parts of the graph (represented by placeholders), use `expand` to reveal them.

## Handling Symbols and IDs

- **Full Paths (FQN)**: Always prefer fully qualified names (e.g., `com.cre.core.service.CreServiceImpl`) or method signatures (e.g., `com.cre.core.service.CreServiceImpl::getContext(Path,String,int,ContextOptions)`).
- **Persistence**: The server now uses absolute paths for all `project_root` arguments. This ensures that your context remains consistent throughout the session.
- **Node ID Map**: When the server returns an integrated view with XML-like tags (e.g., `<omitted_01/>`), it also provides a `node_id_map`. Always check this map to resolve the short tags back to their original `node_id` for subsequent `expand` calls.

## Effective Workflows

- **Bug Investigation**: 
  1. Identify the relevant controller or service.
  2. Use `get_context` on the entry point.
  3. Look for `ExceptionFlow` markers or missing dependencies.
  4. Expand any suspicious branch.

- **Refactoring**:
  1. Use `get_context` to see all current usages of a symbol.
  2. Use `get_file_structure` to see the current contract.
  3. Identify all downstream effects of your change.

- **Knowledge Discovery**:
  1. Ask the server for the `get_project_structure`.
  2. Explore the main service interface with `get_file_structure`.
  3. Deep-dive into the implementation with `get_context`.

## Best Practices

- Always use `.` as the current project root if you are working within the project directory. The server will automatically resolve it to its absolute path.
- If a symbol is not found, try a simpler version (e.g., just the class name) and let the server's fuzzy matching help you find the right FQN.
- Use the `depth` parameter in `get_context` judiciously. A depth of 1 or 2 is usually enough for initial understanding. Use `expand` for more targeted depth later.
