# Project Research Summary

**Project:** CRE
**Domain:** Java + Spring Boot “Context Reconstruction Engine” (MCP server)
**Researched:** 2026-03-25
**Confidence:** MEDIUM

## Executive Summary

CRE is intended to replace RAG for code-changing workflows by reconstructing execution-relevant context from source structure (AST + graph traversal) rather than vector similarity. The core approach is deterministic -> heuristic -> AI fallback, with replace-on-demand and expand-on-demand semantics so the system converges on the missing context iteratively.

The recommended v1 architecture uses a Spring Boot MCP server (stdio) that exposes tool endpoints like `find_symbol`, `get_context`, and `expand`. The reconstruction pipeline is grounded in JavaParser AST extraction into a graph model (nodes: Class/Method/Field; edges: CALLS/USES_FIELD/BELONGS_TO), then enriched by framework plugins (Spring semantics for controller/service wiring). Slicing produces a minimal relevant subgraph and emits placeholders for omissions; expansion replaces placeholders through targeted, evidence-preserving graph growth.

The primary risks are (1) node identity drift across expansions, (2) semantic mismatch between graph evidence and emitted slices, and (3) plugin ordering/contract drift causing nondeterministic outputs. These risks should be explicitly prevented in Phase 1/2/3 with stable identity schemas, evidence links, and deterministic plugin execution order.

## Key Findings

### Recommended Stack

Recommended stack is Spring Boot 3.5.12 for hosting the MCP stdio server, JavaParser 3.28.0 for reliable AST extraction, and the official MCP Java SDK (stdio transport) for protocol/tool plumbing. Maven is the build tool, with JDK 21 (Corretto) as the baseline.

**Core technologies:**
- Spring Boot 3.5.12: hosting + dependency management for the MCP server
- JavaParser 3.28.0: structured AST parsing for deterministic context reconstruction
- MCP Java SDK (v1.1.0, stdio transport): stable MCP server implementation surface

### Expected Features

**Must have (table stakes):**
- `Controller.method` canonical query input
- MCP tool integration (`find_symbol`, `get_context`, `expand`, etc.)
- Structured `get_context` output (JSON + metadata/provenance)
- Deterministic -> heuristic -> AI fallback gating

**Should have (competitive):**
- Execution-relevant reconstruction without vector RAG
- Confidence + explainability per slice
- Core + plugin architecture (Spring semantics as the first plugin)
- Interface/implementation discovery + trace flow

**Defer (v2+):**
- Broader framework expansion beyond Spring/event patterns
- Multi-module/multi-repo orchestration unless required by your initial evaluation dataset

### Architecture Approach

CRE should be implemented as a modular pipeline: Query Intake -> Context Builder -> Graph Engine -> Plugin Layer -> Slicing Engine -> Formatter -> Expand API. The system should remain deterministic for core truth and only use AI at gated “gap filling” points, always with provenance/evidence quality labels.

**Major components:**
1. Graph Engine — AST indexing + graph edges (CALLS/USES_FIELD/BELONGS_TO)
2. Plugin Layer — Spring semantics annotation and role/edge enrichment
3. Slicing + Expand — minimal slices + placeholder contract + lazy expansions

### Critical Pitfalls

Top pitfalls to prevent:

1. Node identity drift across expansions (breaks dedupe and convergence)
2. AST–Graph–Slice mismatch (emitted context doesn’t match evidence)
3. Plugin contract drift (nondeterminism via hook ordering/side effects)
4. Expand stale/contradictory edges (partial reload invalidation gaps)
5. Ranking over-prunes critical evidence (correctness vs cleanliness conflict)

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 0: Prototype (Graph seed + minimal slicing)
**Rationale:** Establish AST->graph extraction and a minimal “slice output” pipeline before adding plugins/expand.  
**Delivers:** A working core graph + first slice output with placeholders.  
**Addresses:** Node ID schema baseline and formatting determinism risk.

### Phase 1: Core System (controller->service graph traversal)
**Rationale:** Core correctness must be achieved before framework semantics and expand-on-demand.  
**Delivers:** Deterministic reconstruction for controller/service flows + evidence/parity tests.  
**Uses:** JavaParser indexing + Spring mapping hooks (initial, possibly simplistic).  
**Avoids:** AST–Graph–Slice mismatch and depth/cycle runaway.

### Phase 2: Plugin System (Spring semantics contract)
**Rationale:** Framework semantics are necessary to make execution-relevant traversal accurate.  
**Delivers:** Stable plugin API + deterministic ordering + Spring plugin that annotates roles/edges.  
**Avoids:** Plugin contract drift and side-effect ordering issues.

### Phase 3: Expand-on-demand (placeholder convergence)
**Rationale:** Expand/replace semantics validate the iterative “progressive discovery” promise.  
**Delivers:** `expand(node_id)` with identity-based dedupe and placeholder replacement.  
**Avoids:** Identity drift and contradictory partial reload merges.

### Phase 4: Ranking & Pruning (bounded context quality)
**Rationale:** After correctness, reduce noise while preserving correctness-required evidence.  
**Delivers:** Heuristic scoring with correctness constraints and evidence category guarantees.  
**Avoids:** Over-pruning critical exception/event paths.

### Phase 5: Advanced Plugins (real-world readiness)
**Rationale:** Extend beyond the initial Spring annotation set without destabilizing core.  
**Delivers:** Additional plugins (event handling, exceptions, or domain-specific hooks).  

### Phase 6: Evaluation
**Rationale:** Validate token reduction, accuracy, and developer usability with your real change tasks/feedback loop.  
**Delivers:** Metrics, case studies, and go/no-go decision on scaling/pivot.

### Phase Ordering Rationale

- Phase 0/1 prevent semantic mismatch and define stable identity/evidence contracts early.
- Phase 2 makes Spring semantics deterministic and testable before expanding scope.
- Phase 3 validates convergence and “replace-on-demand” behavior under repeated expansions.
- Phase 4 adds ranking/pruning only after correctness is stable, preventing correctness regressions.

### Research Flags

Phases likely needing deeper research during planning:
- Phase 1: how to best validate AST->graph evidence parity for method calls/overloads.
- Phase 2: plugin API contract details to avoid side-effect ordering bugs.
- Phase 3: stable node-id schema and invalidation rules for partial reload expansion.
- Phase 4: how to enforce correctness-required evidence categories while pruning.

Phases with standard patterns:
- Phase 0 (prototype) and Phase 6 (evaluation) once baseline pipeline exists.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | MEDIUM | Key versions pinned; transitive/SDK specifics may vary in your final `pom.xml` |
| Features | MEDIUM | MVP defined by tool loop and deterministic reconstruction goals; exact feature boundaries may shift |
| Architecture | MEDIUM | Pipeline aligns with `.docs`, but implementation details need confirmation during planning |
| Pitfalls | MEDIUM | Pitfalls mapped to likely failure modes; verification will come during Phase 0-3 |

**Overall confidence:** MEDIUM

### Gaps to Address

- Stable node identity schema and evidence-linking strategy: define in Phase 0/1 and lock with tests.
- MCP tool payload schema: confirm exact JSON shape and error semantics during Phase 1/2.
- Spring mapping edge cases (controllers, autowiring, proxies): confirm with your real Spring Boot examples.

## Sources

### Primary (MEDIUM confidence)
- Spring Boot 3.5.12 release information (pinned baseline)
- JavaParser 3.28.0 releases (pinned AST parser)
- MCP Java SDK v1.1.0 stdio transport documentation

---
*Research completed: 2026-03-25*
*Ready for roadmap: yes*

