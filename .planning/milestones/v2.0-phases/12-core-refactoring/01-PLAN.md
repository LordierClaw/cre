# Phase 12: Core Refactoring

## Objective
Refactor the core engine for better maintainability, simplify the toolset by removing legacy/unused logic, and adopt full Spring Dependency Injection.

## Context
- Current implementation uses a mix of static `getInstance()` (ProjectManager) and manual instantiation of tools (CreController, McpConfig).
- Heuristic ranking (RankingPruner) is deeply integrated but slated for removal in favor of deterministic expansion.
- Legacy tools (TraceFlowTool, FindImplementationsTool) are being replaced by a more powerful `get_context` and structure exploration tools.

## Requirements

### R1: Spring DI & Service Layer
- Create `com.cre.core.service.CreService` interface.
- Create `com.cre.core.service.CreServiceImpl` implementation.
- Refactor `ProjectManager` to remove static `INSTANCE` and `@Deprecated getInstance()`.
- Inject `ProjectManager` and `CreService` where needed.

### R2: Centralize Core Logic
- Move core logic from `GetContextTool` and `IntegratedViewBuilder` into `CreService`.
- Consolidate common traversal and reconstruction logic.

### R3: Remove Legacy Logic & Tools
- Delete `com.cre.tools.rank.RankingPruner`.
- Delete `com.cre.tools.TraceFlowTool`.
- Delete `com.cre.tools.FindImplementationsTool`.
- Delete `com.cre.tools.GetContextTool`.
- Delete `com.cre.tools.IntegratedViewBuilder`.
- Update `CreController` and `McpConfig` to use `CreService`.

### R4: Standardize Output (Initial Redesign)
- Update `get_context` to use the new XML-like tagging for classes (e.g., `<ClassName>...</ClassName>`).
- Ensure it supports basic depth-based traversal without heuristic pruning.

## Implementation Steps

### Step 1: Infrastructure (Service & DI)
1. Create `com.cre.core.service.CreService` and `CreServiceImpl`.
2. Update `ProjectManager`:
    - Remove `INSTANCE` field and `getInstance()` method.
    - Ensure it is a proper Spring `@Service`.
3. Update `CreController` and `McpConfig` to inject `CreService`.

### Step 2: Logic Consolidation
1. Port logic from `GetContextTool` and `IntegratedViewBuilder` to `CreServiceImpl`.
2. Remove any references to `RankingPruner` during the port.
3. Simplify the traversal to be purely deterministic (BFS/DFS based on depth).

### Step 3: Cleanup & Removal
1. Delete legacy tool classes and the `rank` package.
2. Clean up `CreController` and `McpConfig` (remove legacy endpoints if not needed, or redirect to `CreService`).
3. Update `pom.xml` if any dependencies are no longer needed (unlikely, but worth checking).

### Step 4: Output Refactoring
1. Update the context reconstruction in `CreServiceImpl` to use the new XML-like format.
2. Support `depth` parameter correctly in the new implementation.

## Verification Plan

### Automated Tests
- Create `CreServiceIntegrationTest` to verify `get_context` and `expand` logic.
- Update existing tests that relied on `ProjectManager.getInstance()`.
- Delete tests for removed tools (TraceFlow, FindImplementations, Pruning).

### Manual Verification (MCP)
- Run `test_mcp.sh` (if updated) or use a local MCP client to call `get_context` and verify the new XML-like output.
- Verify `expand` functionality still works as expected.

## Success Criteria
- [ ] No static `getInstance()` in `ProjectManager`.
- [ ] `CreService` manages all core operations.
- [ ] Legacy tools and `RankingPruner` are removed from the codebase.
- [ ] `get_context` output uses XML-like tagging for classes.
- [ ] Project builds and all relevant tests pass.
