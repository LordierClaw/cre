# Roadmap: CRE

## Overview

CRE ships as a Java + Spring Boot MCP server (`stdio`) that resolves short symbols (`Controller.method`), reconstructs execution-relevant context via an AST-backed graph, and progressively expands only when needed. The journey moves from a thin prototype (symbol resolution + graph seed) through a demo-ready core (structured context, traceability, confidence, placeholders), a Spring-aware plugin layer, bounded expand-on-demand, then ranking/pruning and advanced plugins to control noise, ending in balanced evaluation (tokens, accuracy, usability) against PROJECT.md success metrics.

## Phase summary

| Phase | Name | v1 requirements | Summary |
|-------|------|-----------------|--------|
| 0 | Prototype | QRY-01 | (ABANDONED) Resolve `Controller.method` to a method node; seed graph and minimal query path |
| 1 | Core System | CTX-01, REC-01, CONF-01, PLC-01, IMPL-01, TRCE-01 | Structured `get_context`, deterministic reconstruction path, confidence/placeholders, `find_implementations`, `trace_flow` |
| 2 | Plugin System | Complete    | 2026-03-25 |
| 3 | Expand-on-demand | EXP-01 | Bounded `expand(node_id)`; placeholders replaced with deeper content |
| 4 | Ranking & Pruning | — | Heuristics, depth weighting, noise reduction (no additional v1 IDs) |
| 5 | Advanced Plugins | — | Event/exception/domain plugins; real-world readiness (no additional v1 IDs) |
| 6 | Evaluation | — | (ABANDONED) Measure tokens, accuracy, usability; decide scale vs pivot (no additional v1 IDs) |
| 7 | Real-project Ingestion | ING-01, E2E-01 | Directory-based ingestion and E2E verification on real project |
| 8 | HTTP/SSE & REST Support | — | Transition to persistent Spring Boot server with SSE/REST transport |

## v1 requirement coverage

Every v1 requirement maps to exactly one phase.

| Requirement | Phase |
|-------------|-------|
| QRY-01 | 0 |
| CTX-01 | 1 |
| REC-01 | 1 |
| CONF-01 | 1 |
| PLC-01 | 1 |
| IMPL-01 | 1 |
| TRCE-01 | 1 |
| PLUG-01 | 2 |
| EXP-01 | 3 |

**Coverage check:** 9 / 9 v1 requirements mapped.

## Phases

**Phase numbering:** Integers 0–6 follow `.docs/WORKPLAN.md`. Decimal phases (e.g. 2.1) are reserved for urgent insertions.

- [-] **Phase 0: Prototype** — (ABANDONED) Symbol resolution and minimal graph/query path
- [x] **Phase 1: Core System** — Full core MCP context pipeline, reconstruction, trace tools
- [ ] **Phase 1.1: Project-wide Ingestion** — (MERGED INTO PHASE 7) Automate source discovery and indexing from directory root
- [x] **Phase 2: Plugin System** — Extensible architecture + Spring semantics plugin (completed 2026-03-25)
- [x] **Phase 3: Expand-on-demand** — Bounded expansion API
- [x] **Phase 4: Ranking & Pruning** — Heuristic quality and noise control
- [x] **Phase 5: Advanced Plugins** — Additional plugin classes for production depth (completed 2026-03-25)
- [-] **Phase 6: Evaluation** — (ABANDONED) MVP metrics and decision
- [x] **Phase 7: Real-project Ingestion** — Automate discovery + E2E verification (Completed 2026-03-27)
- [x] **Phase 8: HTTP/SSE & REST Support** — Transition to persistent Spring Boot server with SSE/REST transport (Completed 2026-03-27)

## Phase details

### Phase 0: Prototype

**Goal:** Prove end-to-end resolution from canonical symbol input to a graph-backed method node with a minimal query surface.

**Depends on:** Nothing (first phase)

**Requirements:** QRY-01

**Success criteria** (what must be TRUE):

1. A caller can supply a target in `Controller.method` form and receive a deterministic resolution to a method node when the project sources uniquely identify that method.
2. Overload or ambiguity cases are detected and reported safely (no silent incorrect binding).
3. Repeated identical queries against an unchanged codebase yield the same resolved node id (deterministic resolution behavior is observable in tests or scripted checks).

**Plans:** TBD

Plans:

- [ ] 00-01: TBD — JavaParser setup, class/method extraction, simple graph (per WORKPLAN)

### Phase 1: Core System

**Goal:** Deliver a minimal demo-ready MCP server: structured `get_context`, deterministic controller→service→implementation reconstruction over the AST graph, confidence and explainability, placeholder contracts, plus `find_implementations` and `trace_flow` for the entry path.

**Depends on:** Phase 0

**Requirements:** CTX-01, REC-01, CONF-01, PLC-01, IMPL-01, TRCE-01

**Success criteria** (what must be TRUE):

1. `get_context` returns structured JSON for the resolved method, including sliced source, provenance, evidence type/quality, and clear slice boundaries (CTX-01).
2. A deterministic traversal path builds execution-relevant context along controller → service → reachable implementations using the AST-derived graph, with documented integration points for later Spring-aware plugins (REC-01).
3. Every `get_context` response exposes confidence and explainability that distinguish deterministic steps from heuristic steps from gated fallback (CONF-01).
4. When information is missing, responses include a stable placeholder contract that states what was omitted and what expansion would likely address (PLC-01).
5. `find_implementations` returns implementations relevant to service/interface types used in controller→service resolution, and `trace_flow` returns a coherent trace for the reconstructed controller→service call chain for the entry path (IMPL-01, TRCE-01).

**Plans:** TBD

Plans:

- [ ] 01-01: TBD — Graph traversal, depth control, basic pruning, context formatter (per WORKPLAN)
- [ ] 01-02: TBD — MCP tools: `get_context`, `find_implementations`, `trace_flow` wired to core

**Discuss-phase notes (for `/gsd-discuss-phase 1`):** Lock transport (`stdio`), JSON schema shape for `get_context`, how overload ambiguity propagates from QRY-01 into tools, and where placeholder vs slice boundaries are recorded so Phase 3 `expand` can target node ids consistently.

### Phase 2: Plugin System

**Goal:** Formalize core vs plugin boundaries; ship a Spring plugin that annotates or derives execution-relevant semantics from Spring annotations.

**Depends on:** Phase 1

**Requirements:** PLUG-01

**Success criteria** (what must be TRUE):

1. Plugins register through a defined interface and extend AST/graph processing without forking the core.
2. The Spring plugin enriches or annotates the graph using conventions such as `@Controller`, `@Service`, and `@Autowired` (examples from requirements).
3. The core runtime remains buildable and testable without loading optional plugins (clear default wiring).

**Plans:** 1/1 plans complete

Plans:

- [ ] 02-01: TBD — Plugin interface and lifecycle
- [ ] 02-02: TBD — Spring plugin implementation

### Phase 3: Expand-on-demand

**Goal:** Allow iterative deepening via `expand(node_id)` without token or work explosion.

**Depends on:** Phase 2

**Requirements:** EXP-01

**Success criteria** (what must be TRUE):

1. `expand(node_id)` returns a widened context slice that replaces prior placeholders with deeper graph-backed content when requested.
2. Expansion is bounded by explicit limits (e.g. depth, node budget, or similar) and refuses or degrades gracefully rather than exhausting resources.
3. Expansion results remain consistent with Phase 1 placeholder contracts (what was omitted vs what expansion added is observable).

**Plans:** TBD

Plans:

- [ ] 03-01: TBD — Node id scheme and partial graph reload (per WORKPLAN)

### Phase 4: Ranking & Pruning

**Goal:** Improve signal-to-noise via heuristics, variable-usage awareness, and depth weighting; optional lightweight AI pruning if adopted.

**Depends on:** Phase 3

**Requirements:** None (engineering milestone; supports quality goals from PROJECT.md)

**Success criteria** (what must be TRUE):

1. Context outputs apply heuristic scoring or prioritization so lower-relevance nodes are dropped or ranked lower observably vs an unranked baseline.
2. Variable usage or similar static signals influence inclusion or ordering in a documented way.
3. Measured context size or noise metrics improve on representative fixtures without breaking v1 API contracts.

**Plans:** 1/1 plans complete

Plans:

- [x] 04-01: Heuristic scoring and pruning pipeline (`.planning/phases/04-ranking-pruning/04-PLAN.md`) — completed 2026-03-26
- [x] 04-02: Integrate ranked pruning into `GetContextTool` with compact metadata (`.planning/phases/04-ranking-pruning/04-PLAN.md`) — completed 2026-03-26
- [x] 04-03: Add ranking/pruning schema and determinism test coverage (`.planning/phases/04-ranking-pruning/04-PLAN.md`) — completed 2026-03-26
- [x] 04-04: Validation + roadmap traceability checkpoint (`.planning/phases/04-ranking-pruning/04-PLAN.md`) — completed 2026-03-26

### Phase 5: Advanced Plugins

**Goal:** Extend plugin ecosystem for events, exception paths, and custom domains toward real-world readiness.

**Depends on:** Phase 4

**Requirements:** None (engineering milestone; future-facing)

**Success criteria** (what must be TRUE):

1. At least one additional plugin category (e.g. event handling or exception flow) is demonstrable on sample code.
2. Third-party or domain plugins can be added without modifying core source (configuration-only or SPI-only registration).
3. Plugin interactions with ranking/pruning from Phase 4 are defined and testable.

**Plans:** 1/1 plans complete

Plans:

- [x] 05-01: exception-flow advanced plugin first (`.planning/phases/05-advanced-plugins/05-PLAN.md`) — checklist `05-01`..`05-04` completed 2026-03-26

### Phase 6: Evaluation

(ABANDONED)

### Phase 7: Real-project Ingestion

**Goal:** Automate project-wide source discovery and indexing from a directory root and verify E2E on a real target project.

**Depends on:** Phase 5

**Requirements:** ING-01, E2E-01

**Success criteria** (what must be TRUE):

1. `CreContext` automatically indexes all Java files in a provided directory (e.g. `src/main/java`).
2. E2E tests against `/home/hainn/blue/code/cre-test-project` correctly resolve symbols and reconstruct context.
3. System behaves deterministically and correctly on the real target codebase.

**Plans:** TBD

Plans:

- [x] 07-01: Automate directory indexing in `CreContext`
- [x] 07-02: E2E test suite for `cre-test-project`
- [x] 07-03: Final verification and cleanup
## Progress

**Execution order:** 0 → 1 → 2 → 3 → 4 → 5 → 6 → 7.

| Phase | Name | Plans complete | Status | Completed |
|-------|------|----------------|--------|-----------|
| 0 | Prototype | 0/TBD | Abandoned | — |
| 1 | Core System | 1/1 | Complete | 2026-03-25 |
| 1.1 | Project-wide Ingestion | 0/TBD | Merged | — |
| 2 | Plugin System | 1/1 | Complete | 2026-03-25 |
| 3 | Expand-on-demand | 1/1 | Complete | 2026-03-26 |
| 4 | Ranking & Pruning | 1/1 | Complete | 2026-03-26 |
| 5 | Advanced Plugins | 1/1 | Complete | 2026-03-25 |
| 6 | Evaluation | 0/TBD | Abandoned | — |
| 7 | Real-project Ingestion | 1/1 | Complete | 2026-03-27 |
| 8 | HTTP/SSE & REST Support | 1/1 | Complete | 2026-03-27 |

---

*Last updated: 2026-03-27 — roadmap synchronized with execution summaries and planning integrity check.*

