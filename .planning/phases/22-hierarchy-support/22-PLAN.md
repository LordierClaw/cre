# Phase 22 Plan: Polymorphism & Inheritance

## Goal
Verify and ensure accurate resolution across class hierarchies.

## Tasks

### T1: Inheritance Verification
- [ ] Create `src/test/java/com/cre/fixtures/BaseService.java` (Interface).
- [ ] Create `src/test/java/com/cre/fixtures/AbstractBaseService.java` (Abstract class).
- [ ] Create `src/test/java/com/cre/fixtures/RealService.java` (Implementation).
- [ ] Create `src/test/java/com/cre/fixtures/ServiceConsumer.java` calling methods from the hierarchy.
- [ ] Create `src/test/java/com/cre/tools/InheritanceResolutionTest.java` to verify correct edges.

### T2: Traversal Verification
- [ ] Verify that `get_context` correctly expands to parent classes when needed.

## Verification
- Run `mvn test`.
