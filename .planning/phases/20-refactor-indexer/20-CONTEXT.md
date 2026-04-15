# Phase 20: Refactoring JavaAstIndexer

## Objective
Refactor `JavaAstIndexer` to use `JavaParser Symbol Solver` for all cross-reference and type resolution.
Eliminate brittle manual string manipulation for symbol discovery.

## Requirements
- **SYMB-06**: Use Symbol Solver for indexing all nodes and edges.

## Status
- **Current Position**: Phase 19 complete (infrastructure in place).
- **Goal**: Replace manual resolution with `resolve()` and `describe()`.

## Success Criteria
1. `JavaAstIndexer` no longer relies on manual string-based symbol parsing for complex types.
2. Indexing process successfully uses Symbol Solver to identify node types.
3. Existing E2E tests pass with the new indexer.
