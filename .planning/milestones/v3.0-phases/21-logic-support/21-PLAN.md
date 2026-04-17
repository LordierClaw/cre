# Phase 21 Plan: Overloading & Generics Support

## Goal
Verify and ensure accuracy for overloading and generics using Symbol Solver.

## Tasks

### T1: Overloading Verification
- [ ] Create `src/test/java/com/cre/fixtures/OverloadService.java` with overloaded methods (`process(String)`, `process(Object)`, `process(Integer)`).
- [ ] Create `src/test/java/com/cre/fixtures/OverloadController.java` calling these methods.
- [ ] Create `src/test/java/com/cre/tools/OverloadResolutionTest.java` to verify correct edges.

### T2: Complex Generics Verification
- [ ] Verify that `GenericTypeSupportTest` still passes and provides accurate results.
- [ ] Add a test case for `List<? extends T>` resolution.

### T3: Type Inference (`var`)
- [ ] Add a method using `var` in `OverloadController`.
- [ ] Verify that calls on `var` variables are correctly resolved.

## Verification
- Run `mvn test`.
