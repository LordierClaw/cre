# Phase 20 Plan: Refactoring JavaAstIndexer

## Goal
Fully integrate `JavaSymbolSolver` into the indexing process.

## Tasks

### T1: Symbol Translation Utility
- [ ] Implement `String toCreFqName(String rawType)` to strip generics and normalize JDK names (String vs java.lang.String).
- [ ] Implement `String toCreMethodId(MethodCallExpr call)` using `call.resolve()`.

### T2: Refactor `resolveCallee`
- [ ] Replace `resolveCallee` body with `call.resolve().getQualifiedSignature()`.
- [ ] Ensure formatting matches CRE conventions (using `::`).

### T3: Refactor `resolveTypeReferenceToFqn`
- [ ] Simplify `resolveTypeReferenceToFqn` to primarily rely on `type.resolve().describe()`.

### T4: Refactor Field and Type Usage Resolution
- [ ] Update `indexMethodCalls` to resolve field access using `fa.resolve()`.
- [ ] Update `indexMethodCalls` to resolve name expressions using `ne.resolve()`.

### T5: Verification
- [ ] Ensure existing `GenericTypeSupportTest` and `SymbolSolverIntegrationTest` pass.
- [ ] Run E2E tests for regression.

## Verification
- Run `mvn test`.
