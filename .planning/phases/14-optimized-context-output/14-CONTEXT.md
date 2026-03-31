# Phase 14: Optimized Context Output — Context & Decisions

## Objective
Redesign the `get_context` output to be token-efficient, structure-aware, and granularly controllable via options.

## Decisions

### 1. Granular Options (`options` JSON)
The `get_context` tool will support an `options` parameter with the following structure:
```json
{
  "definitions": {
    "imports": "omitted|relevance|full",
    "properties": "omitted|relevance|full",
    "functions": "omitted|full"
  },
  "expanded_functions": ["com.pkg.Class.method1", "com.pkg.Class.method2"]
}
```
- **Default values**: `imports: omitted`, `properties: relevance`, `functions: omitted`.
- **Relevance (Usage-based)**:
  - **Imports**: Include only those required by the methods/fields being shown in the output.
  - **Properties**: Include only those explicitly used within the methods being shown.
- **`expanded_functions` Priority**: Any function listed in `expanded_functions` (using `Full FQN.Method` format) will be shown in **full**, overriding the global `definitions.functions: omitted` setting.

### 2. Transitive Depth Expansion
- When `depth > 0`, the engine will traverse dependencies.
- Each class encountered will be wrapped in its own XML-like tags (e.g., `<UserController>...</UserController>`).
- All classes will be merged into a single text response, ordered by distance from the starting node (closest first).

### 3. Output Format Optimization
- **Tags**: Use `<omitted_imports/>`, `<omitted_properties/>`, and `<omitted_functions/>` to indicate pruned sections.
- **Circular Dependencies**: The traversal will use a `visited` set to prevent infinite loops and redundant output. A class will only be printed once per request.

### 4. Post-processing Architecture
- **Filter Hook**: Implement a `ContextPostProcessor` interface.
- **Initial Implementation**: An "Empty Hook" that returns the code as-is but allows for future logic to replace method bodies with `<omitted_code description="..."/>` tags.

### 5. Technical Implementation Details
- **LexicalPreservingPrinter**: Continue using this to ensure comments and formatting are preserved for the shown sections.
- **JavaParser Visitors**: Use specialized visitors to calculate "relevance" (tracking symbol usage in the current slice).

## Open Questions / Gray Areas (Deferred to Planning)
- Exact XML tag names for anonymous or inner classes (likely `<OuterClass$InnerClass>`).
- Performance of usage-based relevance calculation for very large files.

## Next Steps
- Research `JavaParser` symbol solver integration for more accurate relevance calculation.
- Plan the `CreServiceImpl` refactoring to support these granular options.
