# Phase 19: Symbol Solver Integration & Setup

## Objective
Integrate `JavaParser` Symbol Solver with `CombinedTypeSolver`.
Configure CRE to resolve symbols accurately across multiple source roots and standard libraries.

## Requirements
- **SYMB-01**: Configure JavaParser Symbol Solver in `JavaAstIndexer`.
- **SYMB-05**: Support wildcard imports (`import java.util.*`).

## Status
- **Current Position**: Milestone v3.0 started.
- **Goal**: Add dependency and initialize `JavaSymbolSolver`.

## Stakeholders
- Development Team

## Constraints
- **Language/Framework**: Java 21, Spring Boot 3.5.x
- **Build Tool**: Maven
- **JDK**: Corretto 21
- **Library**: JavaParser 3.28.0

## Context
- `JavaAstIndexer` currently uses manual string manipulation and package/import traversal to resolve symbols.
- This fails for:
    - Method overloading.
    - Inheritance and polymorphism.
    - Complex generic signatures.
    - Wildcard and static imports.
- `JavaSymbolSolver` provides a complete type system and resolution engine that correctly handles these cases.

## Success Criteria
1. Project compiles with `javaparser-symbol-solver-core` dependency.
2. `CombinedTypeSolver` correctly discovers project sources and external dependencies.
3. Wildcard imports are resolved by the Symbol Solver.
