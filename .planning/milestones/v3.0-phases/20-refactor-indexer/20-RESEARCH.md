# Phase 20 Research: Refactoring JavaAstIndexer

## Target Methods for Replacement
The following methods in `JavaAstIndexer` can be significantly simplified or replaced by `JavaSymbolSolver`:

1.  **`resolveCallee`**: Currently uses `resolveScopeTypeFqn` and `inferArgumentType`.
    -   *Replace with*: `call.resolve().getQualifiedSignature()`.
2.  **`resolveScopeTypeFqn`**: Currently traverses scope AST manually.
    -   *Replace with*: `scope.calculateResolvedType().describe()`.
3.  **`resolveTypeForExpression`**: Currently attempts to find method return types manually.
    -   *Replace with*: `expr.calculateResolvedType().describe()`.
4.  **`resolveFieldTypeName`**: Currently searches fields in the class.
    -   *Replace with*: `fieldAccess.resolve().getType().describe()`.

## Challenges & Solutions

### 1. External Symbol Resolution
- For symbols that are NOT in the source code (e.g., standard libraries or missing JARs), the Symbol Solver will throw an exception or return an error.
- **Solution**: Keep the manual fallback *only* for cases where the Symbol Solver fails (e.g., when dependencies are not on the classpath).

### 2. Signature Normalization
- Symbol Solver returns signatures like `com.example.Service.process(java.lang.String)`.
- CRE uses `com.example.Service::process(String)` format.
- **Solution**: Implement a small mapping utility to translate Symbol Solver output to CRE symbol format.

### 3. Generic Parameters
- `describe()` returns `java.util.List<java.lang.String>`.
- Existing logic expects base class `java.util.List` plus separate dependencies on arguments.
- **Solution**: Continue using the stripping logic established in Phase 19.

## Implementation Strategy
- Step 1: Implement `toCreSymbol(ResolvedMethodDeclaration)` and `toCreSymbol(ResolvedType)`.
- Step 2: Update `resolveCallee` to use `resolve()`.
- Step 3: Update `addTypeDependencies` to use `resolve()`.
- Step 4: Clean up now-obsolete helper methods.
