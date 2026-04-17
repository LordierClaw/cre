# Phase 13: Exploration Tools & NodeId Removal

## Objective
Implement project and file exploration tools and completely remove the complex `NodeId` logic in favor of human-readable symbols (Strings).

## Context
- `NodeId` is currently a record with `fqn`, `signature`, and `origin`. The user wants it gone.
- New tools needed: `get_project_structure` (tree-like view) and `get_file_structure` (code skeleton).
- `index_project` should be automatic but also available as an explicit tool if needed.

## Requirements

### R1: Remove NodeId Logic
- [ ] Delete `com.cre.core.graph.NodeId`.
- [ ] Refactor `GraphEngine` to use `String` as the node identifier.
- [ ] Update `JavaAstIndexer` and plugins to generate `String` identifiers (e.g., `com.pkg.Class::method(Param)`).
- [ ] Update `CreServiceImpl` and `CreController` to handle string-based symbols.

### R2: Project Exploration (`get_project_structure`)
- [ ] Implement `getProjectStructure(Path projectRoot)` in `CreService`.
- [ ] Generate a tree-like string representation of the project's packages and directories.
- [ ] Automatically trigger indexing if the project is not already in the `ProjectManager` cache.

### R3: File Structure Extraction (`get_file_structure`)
- [ ] Implement `getFileStructure(Path projectRoot, String filePathOrSymbol)` in `CreService`.
- [ ] Use `JavaParser` to extract a skeleton of the file (classes, fields, method signatures with annotations).
- [ ] Omit all method bodies and implementation details.

### R4: Tool Refactoring & MCP Integration
- [ ] Update `McpConfig` to expose the new tools.
- [ ] Ensure all tools use the simplified symbol-based interaction.

## Implementation Steps

### Step 1: NodeId Removal & Refactoring
1. **Refactor Graph model**: Change `GraphNode` and `GraphEdge` to use `String` instead of `NodeId`.
2. **Update Indexer**: Modify `JavaAstIndexer` to generate string identifiers.
3. **Update Plugins**: Update `SpringSemanticsPlugin` and `ExceptionFlowPlugin` to use string identifiers.
4. **Delete NodeId.java**: Once all references are removed.
5. **Fix Tests**: Update all tests to use string identifiers.

### Step 2: Project Structure Tool
1. **Implement Tree Logic**: Create a utility to walk the file system and generate a tree-like string.
2. **Integrate with CreService**: Add `getProjectStructure` to the service and implementation.
3. **Expose in MCP**: Add `get_project_structure` tool to `McpConfig`.

### Step 3: File Structure Tool
1. **Implement Skeleton Logic**: Create a visitor/transformer in `CreServiceImpl` that prunes all bodies from a `CompilationUnit`.
2. **Integrate with CreService**: Add `getFileStructure` to the service and implementation.
3. **Expose in MCP**: Add `get_file_structure` tool to `McpConfig`.

### Step 4: Verification & Cleanup
1. **Run Tests**: Ensure the refactored system passes all existing tests.
2. **Manual Test**: Use the MCP tools to verify the tree and skeleton outputs.

## Verification Plan

### Automated Tests
- `CreServiceTest`: Add tests for `getProjectStructure` and `getFileStructure`.
- `RealProjectE2ETest`: Verify ingestion still works with the new string identifiers.

### Manual Verification
- Verify `get_project_structure` output matches a standard `tree` format.
- Verify `get_file_structure` correctly omits bodies but includes annotations and parameters.

## Success Criteria
- [ ] `NodeId.java` is deleted.
- [ ] `get_project_structure` returns a formatted tree string.
- [ ] `get_file_structure` returns a code skeleton without bodies.
- [ ] All tests pass.
