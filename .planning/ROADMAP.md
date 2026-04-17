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

## Milestone v3.0: Symbol Resolution & Accuracy (IN PROGRESS)

### Phase summary

| Phase | Name | Status | Target |
|-------|------|--------|--------|
| 19 | Symbol Solver Integration & Setup | Completed | 2026-04-15 |
| 20 | Refactoring JavaAstIndexer | Completed | 2026-04-15 |
| 21 | Overloading & Generics Support | Completed | 2026-04-15 |
| 22 | Polymorphism & Inheritance | Completed | 2026-04-15 |
| 23 | Modern Java Features | Completed | 2026-04-15 |
| 24 | Final Verification & UAT | Completed | 2026-04-15 |
| 25 | 1/4 | In Progress|  |

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
- [ ] **Phase 25: Output Optimization & Comment Stripping** - Optimize token usage and formatting.

## Phase Details

### Phase 25: Output Optimization & Comment Stripping
**Goal**: Reduce token usage by stripping irrelevant comments and optimizing code formatting.
**Depends on**: Phase 24
**Requirements**: OPT-01
**Success Criteria** (what must be TRUE):
  1. Irrelevant comments/Javadocs are stripped while keeping target function Javadocs and internal comments.
  2. Code formatting issues (line breaks, excessive empty lines) are resolved.
  3. Token count is significantly reduced for large project queries.
**Plans**:
- [x] 25-01-PLAN.md — Traversal and Comment Pruning Refactor
- [ ] 25-02-PLAN.md — Formatting Cleanup & Unit Tests
- [ ] 25-03-PLAN.md — E2E Verification & Performance

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
| 25 | Output Optimization & Comment Stripping | 0/3 | Not started | — |

---

*Last updated: 2026-04-17 — Phase 25 plans added.*
