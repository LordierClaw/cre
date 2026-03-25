# Requirements: CRE

**Defined:** 2026-03-25
**Core Value:** Enable accurate code changes with drastically fewer tokens by progressively reconstructing the exact execution-relevant context and expanding on-demand only when the missing context is truly required.

## v1 Requirements

### Core Query

- [ ] **QRY-01**: User/AI can address a target method using canonical `Controller.method` format and CRE resolves it deterministically to a method node (including safe handling of overload ambiguity)

### Context Reconstruction

- [ ] **CTX-01**: `get_context` returns structured JSON slice for the resolved method, including sliced code plus metadata (provenance, evidence type/quality, slice boundaries)
- [ ] **REC-01**: Deterministic reconstruction path builds execution-relevant context for controller → service → reachable implementations using AST-derived graph traversal with Spring semantics hooks

### Expand-on-demand

- [ ] **EXP-01**: `expand(node_id)` widens the slice in a bounded way, replacing placeholders with deeper graph content without causing token/work explosion

### Confidence / Explainability / Placeholders

- [ ] **CONF-01**: Every `get_context` response includes confidence + explainability (which steps were deterministic vs heuristic vs gated fallback)
- [ ] **PLC-01**: When context is missing, CRE emits a stable placeholder contract that specifies what was omitted and what expansion is likely needed

### Plugins / Spring Semantics

- [ ] **PLUG-01**: Core + plugin architecture is implemented, with a Spring plugin that annotates/derives execution-relevant semantics from Spring annotations (e.g., `@Controller`, `@Service`, `@Autowired`)

### Tool Surface / Traceability

- [ ] **IMPL-01**: `find_implementations` returns implementations for a service/interface type used in controller → service resolution
- [ ] **TRCE-01**: `trace_flow` returns a trace/call-chain representation for the reconstructed entry path (controller → service call chain)

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
| QRY-01 | Phase TBD | Pending |
| CTX-01 | Phase TBD | Pending |
| REC-01 | Phase TBD | Pending |
| EXP-01 | Phase TBD | Pending |
| CONF-01 | Phase TBD | Pending |
| PLC-01 | Phase TBD | Pending |
| PLUG-01 | Phase TBD | Pending |
| IMPL-01 | Phase TBD | Pending |
| TRCE-01 | Phase TBD | Pending |

**Coverage:**
- v1 requirements: 9 total
- Mapped to phases: 0
- Unmapped: 9 ⚠️

---
*Requirements defined: 2026-03-25*
*Last updated: 2026-03-25 after v1 requirements initialization*

