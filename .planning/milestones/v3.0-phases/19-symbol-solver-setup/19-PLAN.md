# Phase 19 Plan: Symbol Solver Setup

## Goal
Integrate `JavaParser` Symbol Solver into `CRE`.

## Tasks

### T1: Update `pom.xml`
- [ ] Add `com.github.javaparser:javaparser-symbol-solver-core:3.28.0` dependency.

### T2: Configure `JavaSymbolSolver`
- [ ] Update `JavaAstIndexer.java` constructor to initialize a `CombinedTypeSolver`.
- [ ] Configure `AstUtils.JAVA_PARSER` or the local parser instance with the new symbol solver.

### T3: Verification
- [ ] Create `src/test/java/com/cre/fixtures/WildcardImportController.java` using `import java.util.*`.
- [ ] Create `src/test/java/com/cre/tools/SymbolSolverIntegrationTest.java` to verify wildcard resolution.
- [ ] Ensure all existing tests pass.

## Verification
- Run `mvn compile` to verify dependency.
- Run the new integration test.
- Run `mvn test` for regression.
