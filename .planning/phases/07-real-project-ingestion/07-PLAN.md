# Phase 7 Plan: Real-project Ingestion

## Tasks

### 07-01: Automate directory indexing in `CreContext`
- [ ] Modify `CreContext` to accept a project root directory.
- [ ] Implement recursive discovery of `.java` files.
- [ ] Update `JavaAstIndexer` to handle project-wide indexing if necessary.

### 07-02: E2E test suite for `cre-test-project`
- [ ] Create `src/test/java/com/cre/e2e/RealProjectE2ETest.java`.
- [ ] Implement test cases that use the real project directory.
- [ ] Verify `find_symbol`, `get_context`, and `trace_flow` on the real project.

### 07-03: Final verification and cleanup
- [ ] Run all tests including the new E2E suite.
- [ ] Ensure `cre-test-project` paths are handled correctly and portably (within the environment).
