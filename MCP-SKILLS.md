# CRE MCP: Agent Mastery Guide

This guide provides precise, actionable workflows for using CRE to master any Java codebase.

## 🚀 The "First 5 Minutes" Workflow

When you enter a new project, follow these exact steps:

1.  **Map the Terrain**:
    `get_project_structure(project_root=".")`
    *Goal: Identify the `controller`, `service`, and `repository` layers.*

2.  **Inspect the Entry Point**:
    `get_file_structure(project_root=".", symbol="com.package.MainController")`
    *Goal: See all available endpoints and their signatures without the noise of the implementation.*

3.  **Deep Dive into Logic**:
    `get_context(project_root=".", node_id="com.package.MainService::importantMethod(...)")`
    *Goal: Get a reconstructed view of that method AND all the code it calls.*

## 🛠 Tool Cheat Sheet

| Tool | When to use it | Key Output |
| :--- | :--- | :--- |
| `get_project_structure` | First time in project | Directory tree + basic file list |
| `get_file_structure` | Before editing a file | Class/Interface skeleton (methods & fields) |
| `get_context` | Understanding complex logic | Reconstructed code slice with dependencies |
| `expand` | When you see `<omitted_XX />` | Fills in the missing code for a specific node |
| `reset_project` | After major code changes | Clears cache and re-indexes everything |

## 💡 Advanced Pro-Tips

### 1. Handling "Omitted" Code
If `get_context` returns a result with tags like `<omitted_01 />`, it means the engine pruned that part to save tokens. 
- Check the `node_id_map` in the response.
- Find the real ID for `01` (e.g., `com.package.Helper::method`).
- Run `expand(node_id="com.package.Helper::method")` to see just that piece.

### 2. Precise Symbol Resolution
- **Prefer FQN**: Always use `com.mycompany.service.MyService` instead of just `MyService`.
- **Method Signatures**: If a class has overloads, use `MyService::methodName(ParamType1,ParamType2)`.
- **Fuzzy Search**: If you're not sure, try a partial name. CRE will return a list of matches.

### 3. Understanding Data Flow
Use `get_context` with `depth=1` or `depth=2`. 
- `depth=0` (default): Only the symbol and its immediate code.
- `depth=1`: The symbol + its first-level dependencies.
- `depth=2`: Follows the rabbit hole one step further.

## 🐛 Troubleshooting

- **"Symbol not found"**: Ensure you have built the project (`mvn compile`) and that the `project_root` is correct.
- **"Connection Refused"**: If using Standalone Mode, ensure your Spring Boot server is actually running in a terminal.
- **Outdated Context**: If you just refactored code, run `reset_project` to force CRE to see the new changes.
