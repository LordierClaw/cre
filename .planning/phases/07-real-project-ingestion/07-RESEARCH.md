# Phase 7 Research: Real-project Ingestion

## Ingestion Strategy

### Current State
`CreContext` currently seems to require manual file registration or is hardcoded for internal fixtures.

### Target State
A `ProjectRoot` should be configurable (e.g., via `application.yml` or CLI argument). `CreContext` should use `Files.walk` or similar to find all `.java` files and index them via `JavaAstIndexer`.

## E2E Testing Strategy

### Target Project
`/home/hainn/blue/code/cre-test-project`

### Verification Points
1. Symbol Resolution: Resolve a known controller method in the test project.
2. Context Reconstruction: Verify the reconstructed context matches expected service/implementation chain in the test project.
3. Determinism: Multiple runs on the same project yield identical results.
