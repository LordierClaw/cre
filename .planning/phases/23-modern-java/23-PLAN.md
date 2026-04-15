# Phase 23 Plan: Modern Java Features

## Goal
Support indexing and resolution for Records and Sealed Classes.

## Tasks

### T1: Update `JavaAstIndexer` for Records
- [ ] Update `index` method to handle `RecordDeclaration`.
- [ ] Implement `indexRecord` method (similar to `indexType`).

### T2: Verification Fixtures
- [ ] Create `src/test/java/com/cre/fixtures/ModernJavaFixture.java` containing a record and a sealed hierarchy.
- [ ] Create `src/test/java/com/cre/tools/ModernJavaSupportTest.java` to verify the graph nodes and edges.

## Verification
- Run `mvn test`.
