# Phase 1.1: Project-wide Ingestion

## Goal

Automate project-wide source discovery and indexing from a directory root, replacing manual file listing in `CreContext`.

## Dependencies

- Phase 1: Core System

## Out of Scope

- Handling non-Java sources.
- Advanced exclude/include patterns (keep it simple for now).

## Key Files

- `src/main/java/com/cre/core/bootstrap/CreContext.java`
- `src/main/java/com/cre/core/ast/JavaAstIndexer.java`
