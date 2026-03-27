# Requirements: CRE

**Defined:** 2026-03-25
**Core Value:** Enable accurate code changes with drastically fewer tokens by progressively reconstructing the exact execution-relevant context and expanding on-demand only when the missing context is truly required.

## v1 Requirements

### Core Query

- [-] **QRY-01**: (ABANDONED) User/AI can address a target method using canonical `Controller.method` format and CRE resolves it deterministically to a method node (including safe handling of overload ambiguity)

### Context Reconstruction

- [x] **CTX-01**: `get_context` returns structured JSON slice for the resolved method, including sliced code plus metadata (provenance, evidence type/quality, slice boundaries)
- [x] **REC-01**: Deterministic reconstruction path builds execution-relevant context for controller → service → reachable implementations using AST-derived graph traversal with Spring semantics hooks

### Expand-on-demand

- [x] **EXP-01**: `expand(node_id)` widens the slice in a bounded way, replacing placeholders with deeper graph content without causing token/work explosion

### Confidence / Explainability / Placeholders

- [x] **CONF-01**: Every `get_context` response includes confidence + explainability (which steps were deterministic vs heuristic vs gated fallback)
- [x] **PLC-01**: When context is missing, CRE emits a stable placeholder contract that specifies what was omitted and what expansion is likely needed

### Plugins / Spring Semantics

- [x] **PLUG-01**: Core + plugin architecture is implemented, with a Spring plugin that annotates/derives execution-relevant semantics from Spring annotations (e.g., `@Controller`, `@Service`, `@Autowired`)

### Tool Surface / Traceability

- [x] **IMPL-01**: `find_implementations` returns implementations for a service/interface type used in controller → service resolution
- [x] **TRCE-01**: `trace_flow` returns a trace/call-chain representation for the reconstructed entry path (controller → service call chain)


### Ingestion / Project-wide

- [x] **ING-01**: Support project-wide source discovery and indexing from a directory root (e.g., `src/main/java`) automatically.

### Verification / E2E

- [x] **E2E-01**: End-to-end (E2E) verification on a real target project (`/home/hainn/blue/code/cre-test-project`) to confirm correct symbol resolution and context reconstruction.

### Network / Persistence

- [ ] **HTTP-01**: Provide a REST API for all tools to support manual/interactive testing.
- [ ] **SSE-01**: Support standard MCP SSE transport for persistent connection and agent integration.

## v2 Requirements

Deferred to future release (not in current roadmap):

### TBD

- **TBD-01**: [Feature deferred until Phase 6 evaluation]

## Out of Scope

Explicitly excluded features for v1:

| Feature | Reason |
|---------|--------|
| Full dynamic dispatch resolution across all JVM runtime behaviors | Out of MVP scope; requires runtime tracing or heavy type solving beyond initial AST/graph evidence |
| Broad non-Spring framework support | MVP focuses on Java + Spring Boot; additional frameworks are plugin expansions after core correctness is stable |

## Traceability

Which phases cover which requirements. Updated during roadmap creation:

| Requirement | Phase | Status |
|-------------|-------|--------|
| QRY-01 | Phase 0 — Prototype | Abandoned |
| CTX-01 | Phase 1 — Core System | Complete |
| REC-01 | Phase 1 — Core System | Complete |
| EXP-01 | Phase 3 — Expand-on-demand | Complete |
| CONF-01 | Phase 1 — Core System | Complete |
| PLC-01 | Phase 1 — Core System | Complete |
| PLUG-01 | Phase 2 — Plugin System | Complete |
| IMPL-01 | Phase 1 — Core System | Complete |
| TRCE-01 | Phase 1 — Core System | Complete |
| ING-01 | Phase 7 — Real-project Ingestion | Complete |
| E2E-01 | Phase 7 — Real-project Ingestion | Complete |

**Coverage:**
- v1 requirements: 9 total
- Mapped to phases: 9
- Unmapped: 0

---
*Requirements defined: 2026-03-25*
*Last updated: 2026-03-25 after roadmap traceability (see `.planning/ROADMAP.md`)*

