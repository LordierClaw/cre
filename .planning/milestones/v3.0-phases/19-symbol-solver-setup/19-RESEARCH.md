# Phase 19 Research: Symbol Solver Setup

## Goal
Establish a robust Symbol Solver configuration that accurately discovers and resolves symbols within the target project and JDK libraries.

## Investigation

### 1. JavaParser Symbol Solver Configuration
To resolve symbols, we need to configure `StaticJavaParser` with a `JavaSymbolSolver` and a `TypeSolver`.
- `CombinedTypeSolver`: Aggregates multiple solvers into one.
- `ReflectionTypeSolver`: Resolves JDK classes (String, List, etc.).
- `JavaParserTypeSolver`: Resolves symbols in source files.

Example configuration in `JavaAstIndexer`:
```java
TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
TypeSolver mainSourceSolver = new JavaParserTypeSolver(projectRoot.resolve("src/main/java"));
TypeSolver testSourceSolver = new JavaParserTypeSolver(projectRoot.resolve("src/test/java"));

CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
combinedTypeSolver.add(reflectionTypeSolver);
combinedTypeSolver.add(mainSourceSolver);
combinedTypeSolver.add(testSourceSolver);

JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
```

### 2. Handling Multiple Source Roots
- Not all projects follow `src/main/java`.
- v2.0 `ProjectManager` already discovers the `projectRoot`.
- We can dynamically add source roots to the `CombinedTypeSolver`.
- For now, `src/main/java` and `src/test/java` are safe defaults.

### 3. Wildcard Imports Support
- `JavaSymbolSolver` automatically handles wildcard imports by searching for candidates in the specified source roots and libraries.
- No manual code change is required for this; the solver will just work.

### 4. Dependency: javaparser-symbol-solver-core
- Must match `javaparser-core` version: `3.28.0`.
- Added as a `compile` scope dependency.

## Proposed Design
1. Update `pom.xml`.
2. Update `JavaAstIndexer` to initialize the `CombinedTypeSolver` during construction or the first `index` call.
3. Use `StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver)` to enable resolution globally for the `JavaParser` instances in that process.
   - Wait! `JavaAstIndexer` uses `AstUtils.JAVA_PARSER`. We should configure that specific instance.

## Verification Strategy
1. Create a test fixture with wildcard imports.
2. Verify that `JavaAstIndexer` can correctly resolve a symbol from a wildcard import.
3. Verify that standard JDK classes are resolved.
