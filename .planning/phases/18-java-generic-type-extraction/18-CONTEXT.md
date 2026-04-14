# Phase 18: Java Generic Type Extraction & Edge Cases

## Objective
Improve Java code extraction and indexing to handle complex edge cases, specifically generic types (e.g., `List<Item>`, `Map<K, V>`) and Spring Boot specific patterns.

## Context
Current code extraction tool (`JavaAstIndexer` and `AstUtils`) does not handle well with generic types like `List<Item>` etc. We should be able to handle all the edge cases when extracting the JAVA code, along with Spring Boot.

## Success Criteria
1.  **Generic Type Support**: Indexer correctly resolves generic types and their arguments in method signatures and field declarations.
2.  **Spring Boot Semantic Handling**: Better extraction of Spring Boot semantic nodes (Controllers, Services, Components) when they use generic types.
3.  **Comprehensive Coverage**: Edge cases like nested generics, wildcards, and type bounds are handled without falling back to `?`.
4.  **Verification**: All new generic-related test cases pass.
