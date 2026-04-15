# Roadmap: CRE

## Overview

CRE (Context Reconstruction Engine) is an AST-backed graph traversal engine designed for precise, token-efficient code context reconstruction.

## Milestone v1.0: MVP (SHIPPED 2026-03-29)

<details>
<summary><b>v1.0 Summary (Phases 01-11)</b></summary>

- Status: ✅ SHIPPED 2026-03-29
- Key Features: AST Graph, Spring Plugins, Ranking & Pruning, Raw Text/XML Migration, SSE/REST Support.
- Archive: [.planning/milestones/v1.0-MVP.md](milestones/v1.0-MVP.md)
</details>

## Milestone v2.0: Redesign & Efficiency (SHIPPED 2026-04-14)

<details>
<summary><b>v2.0 Summary (Phases 12-18)</b></summary>

- Status: ✅ SHIPPED 2026-04-14
- Key Features: Spring DI, Exploration Tools, Optimized XML Output, Symbol-based Resolution.
- Archive: [.planning/milestones/v2.0-REDESIGN.md](milestones/v2.0-REDESIGN.md)
</details>

## Milestone v3.0: Symbol Resolution & Accuracy (PLANNED)

### Phase summary

| Phase | Name | Status | Target |
|-------|------|--------|--------|
| 19 | Symbol Solver Integration & Setup | Not started | TBD |
| 20 | Refactoring JavaAstIndexer | Not started | TBD |
| 21 | Overloading & Generics Support | Not started | TBD |
| 22 | Polymorphism & Inheritance | Not started | TBD |
| 23 | Modern Java Features | Not started | TBD |
| 24 | Final Verification & UAT | Not started | TBD |

## Phases

- [x] **Phase 1: Core System** - Initial graph and traversal engine.
- [x] **Phase 2: Plugin System** - Integration of Spring-specific logic.
- [x] **Phase 3: Expand-on-demand** - Context depth-based graph traversal.
- [x] **Phase 4: Ranking & Pruning** - Heuristic-based token optimization.
- [x] **Phase 5: Advanced Plugins** - Specialized plugins for complex Java patterns.
- [x] **Phase 7: Real-project Ingestion** - Large-scale codebase support.
- [x] **Phase 8: HTTP/SSE & REST Support** - MCP standard transport.
- [x] **Phase 9: Output Format Optimization** - Token-efficient representation.
- [x] **Phase 10: Enhanced XML Context Wrapping** - Structured block tagging.
- [x] **Phase 11: Raw Text & XML Output Migration** - Final v1.0 output engine.
- [x] **Phase 12: Core Refactoring** - Spring DI migration.
- [x] **Phase 13: Exploration Tools** - Discovery tools for code exploration.
- [x] **Phase 14: Optimized Context Output** - Token saving and granular control.
- [x] **Phase 15: Robustness & Exceptions** - Fault-tolerant traversal.
- [x] **Phase 16: Final Verification & UAT** - v2.0 validation.
- [x] **Phase 17.1: Output Format Migration** - Structural updates.
- [x] **Phase 17.2: Context Depth & Indexer Fixes** - Correctness in depth-first traversal.
- [x] **Phase 18: Java Generic Type Extraction** - Basic support for `<T>`.
- [x] **Phase 19: Symbol Solver Integration & Setup** - Add and configure JavaParser Symbol Solver.
- [x] **Phase 20: Refactoring JavaAstIndexer** - Use Symbol Solver for cross-reference extraction.
- [x] **Phase 21: Overloading & Generics Support** - Correct resolution of signatures and types.
- [x] **Phase 22: Polymorphism & Inheritance** - Resolve symbols across hierarchy.
- [x] **Phase 23: Modern Java Features** - Record and sealed class support.
- [x] **Phase 24: Final Verification & UAT** - Validation on complex code patterns.

## Phase Details

### Phase 19: Symbol Solver Integration & Setup
**Goal**: Integrate `JavaParser` Symbol Solver with `CombinedTypeSolver`.
**Depends on**: Phase 18
**Requirements**: SYMB-01, SYMB-05
**Success Criteria** (what must be TRUE):
  1. Project compiles with `javaparser-symbol-solver-core` dependency.
  2. `CombinedTypeSolver` correctly discovers project sources and external dependencies.
  3. Wildcard imports are resolved by the Symbol Solver.
**Plans**: TBD

### Phase 20: Refactoring JavaAstIndexer
**Goal**: Update `JavaAstIndexer` to use Symbol Solver for cross-reference extraction.
**Depends on**: Phase 19
**Requirements**: SYMB-06
**Success Criteria** (what must be TRUE):
  1. `JavaAstIndexer` no longer relies on manual string-based symbol parsing for complex types.
  2. Indexing process successfully uses Symbol Solver to identify node types.
  3. Existing E2E tests pass with the new indexer.
**Plans**: TBD

### Phase 21: Overloading & Generics Support
**Goal**: Enable precise resolution of overloaded methods and generic types.
**Depends on**: Phase 20
**Requirements**: SYMB-02, SYMB-04
**Success Criteria** (what must be TRUE):
  1. Correct method is selected when calling overloaded methods.
  2. Generic types (e.g., `List<User>`) are correctly resolved to their concrete types where possible.
**Plans**: TBD

### Phase 22: Polymorphism & Inheritance
**Goal**: Correctly resolve method calls and field access through class hierarchies.
**Depends on**: Phase 21
**Requirements**: SYMB-03, SYMB-07
**Success Criteria** (what must be TRUE):
  1. Symbols from parent classes are indexed and linked.
  2. Method calls through interface/abstract class types correctly identify potential implementations.
**Plans**: TBD

### Phase 23: Modern Java Features
**Goal**: Ensure accuracy for records, sealed classes, and other recent Java additions.
**Depends on**: Phase 22
**Requirements**: SYMB-08
**Success Criteria** (what must be TRUE):
  1. Records are correctly indexed as classes with components.
  2. Sealed classes and their permitted subclasses are correctly linked in the graph.
**Plans**: TBD

### Phase 24: Final Verification & UAT
**Goal**: Validate the entire v3.0 scope against complex real-world project patterns.
**Depends on**: Phase 23
**Requirements**: SYMB-01 to SYMB-08
**Success Criteria** (what must be TRUE):
  1. All v3.0 E2E tests pass.
  2. Symbol resolution accuracy is measurably higher on complex Java projects.
**Plans**: TBD

## Progress

| Phase | Name | Plans complete | Status | Completed |
|-------|------|----------------|--------|-----------|
| 0 | Prototype | 0/TBD | Abandoned | — |
| 1-18 | v1.0 & v2.0 | 18/18 | Complete | 2026-04-14 |
| 19 | Symbol Solver Integration & Setup | 1/1 | Complete | 2026-04-15 |
| 20 | Refactoring JavaAstIndexer | 1/1 | Complete | 2026-04-15 |
| 21 | Overloading & Generics Support | 1/1 | Complete | 2026-04-15 |
| 22 | Polymorphism & Inheritance | 1/1 | Complete | 2026-04-15 |
| 23 | Modern Java Features | 1/1 | Complete | 2026-04-15 |
| 24 | Final Verification & UAT | 1/1 | Complete | 2026-04-15 |

---

*Last updated: 2026-04-15 — Milestone v3.0 complete.*
