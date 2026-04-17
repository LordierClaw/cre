# Phase 14: Optimized Context Output

## Objective
Redesign the `get_context` output to be token-efficient, structure-aware, and granularly controllable via options.

## Context
- `get_context` needs to support an `options` JSON for granular control (`omitted|relevance|full`).
- "Relevance" means usage-based: include only what's used by the shown methods.
- Support `expanded_functions` to override the global settings.
- Implement a `ContextPostProcessor` hook for future code-shortening logic.

## Requirements

### R1: Granular Options Support
- [ ] Parse `options` JSON in `CreController` and pass to `CreService`.
- [ ] Implement `ContextOptions` record to hold these settings.
- [ ] Support `imports`, `properties`, and `functions` with values `omitted|relevance|full`.
- [ ] Support `expanded_functions` (Full FQN.Method format).

### R2: Usage-based Relevance Calculation
- [ ] Implement logic to track which fields and imports are used within the retained methods.
- [ ] Update `transformType` to only retain "relevant" fields if `properties` is set to `relevance`.
- [ ] Update `buildIntegratedView` to only retain "relevant" imports if `imports` is set to `relevance`.

### R3: Enhanced Output Formatting
- [ ] Ensure XML-like tags for classes are consistently applied.
- [ ] Support `depth > 0` with proper ordering (distance-based).
- [ ] Add `<omitted_imports/>` tag.
- [ ] Handle circular dependencies by tracking visited types during view building.

### R4: Post-processing Architecture
- [ ] Create `com.cre.core.service.ContextPostProcessor` interface.
- [ ] Implement `DefaultContextPostProcessor` (initial empty hook).
- [ ] Integrate post-processor into the final stages of context reconstruction.

## Implementation Steps

### Step 1: Infrastructure & Data Models
1. Create `ContextOptions` record in `com.cre.core.service`.
2. Create `ContextPostProcessor` interface.
3. Update `CreService` and `CreServiceImpl` method signatures to use `ContextOptions`.

### Step 2: Relevance Logic
1. Implement a `UsageVisitor` using `JavaParser` to collect used field names and type names from a set of method declarations.
2. Update `transformType` to use the `UsageVisitor` when `properties: relevance` is set.
3. Update `buildIntegratedView` to use the collected type names to filter imports when `imports: relevance` is set.

### Step 3: Service Implementation Refactoring
1. Update `gatherNodes` to ensure it respects `depth` correctly and returns nodes in a stable, distance-based order.
2. Refactor `buildIntegratedView` to:
    - Track visited types to avoid duplicates.
    - Wrap each class in XML-like tags.
    - Include `<omitted_imports/>`, `<omitted_properties/>`, `<omitted_functions/>` as needed.
3. Integrate the `ContextPostProcessor` at the end of the view building process.

### Step 4: Tool & MCP Integration
1. Update `CreController` to parse the `options` JSON into `ContextOptions`.
2. Update `McpConfig` if any schema adjustments are needed (though `options` is already an object).

## Verification Plan

### Automated Tests
- `CreServiceTest`: Add tests for different `options` combinations (especially `relevance`).
- `CreServiceTest`: Add tests for `expanded_functions` override.
- `CreServiceTest`: Verify `depth > 1` with circular dependency handling.

### Manual Verification
- Call `get_context` with `options: {"definitions": {"properties": "relevance"}}` and verify only used fields are shown.
- Call `get_context` with `options: {"expanded_functions": ["..."]}` and verify the specific function is shown fully.

## Success Criteria
- [ ] `get_context` respects all `options` parameters.
- [ ] "Relevance" mode correctly filters imports and properties based on usage.
- [ ] `expanded_functions` correctly overrides global settings.
- [ ] Output uses XML-like tags and indicates omissions correctly.
- [ ] All tests pass.
