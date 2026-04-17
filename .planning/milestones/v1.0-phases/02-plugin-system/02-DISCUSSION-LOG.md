# Phase 2: Plugin System - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions captured in `02-CONTEXT.md` — this log preserves the alternatives considered.

**Date:** 2026-03-25
**Phase:** 02-plugin-system
**Mode:** discuss
**Areas discussed:** Plugin discovery/registration, Graph enrichment representation, Spring annotation mapping rules, Determinism & evidence gating

---

## Plugin discovery/registration

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Hardcoded plugin registry in `CreMcpServer` (deterministic list) | ✓ |
| 2 | Java `ServiceLoader` (SPI registry) |  |
| 3 | Spring component scanning / Spring beans |  |

**User's choice:** 1
**Notes:** Prefer deterministic wiring for Phase 2 MVP.

---

## Graph enrichment representation

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Add new edge types so Spring semantics participates in traversal naturally | ✓ |
| 2 | Add node/evidence metadata only (no edge-type expansion) |  |
| 3 | Hybrid: metadata plus a small set of new edges |  |

**User's choice:** 1
**Notes:** Keep core traversal stable; add explicit semantics edges.

---

## Spring annotation mapping rules

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Minimum mapping set: `@Controller`/`@RestController`, `@Service`, and `@Autowired` (wiring) |  |
| 2 | Broader stereotypes: also map `@Repository`, `@Component`, and `@Bean` with similar layer semantics | ✓ |
| 3 | Strict + configurable: only minimum set, but allow config-driven extension |  |

**User's choice:** 2
**Notes:** Map additional stereotypes to “service-layer” for MVP usefulness.

---

## Determinism & evidence gating

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Fail/flag deterministically with missing evidence + placeholders; never guess |  |
| 2 | Best-effort mapping with uncertain labels; never block slice |  |
| 3 | Hybrid: deterministic mapping when possible; otherwise placeholders with explicit fallback tier | ✓ |

**User's choice:** 3
**Notes:** Preserve core edges/traversal; surface missing Spring semantics via evidence + placeholders.

---

## Claude's Discretion
- Exact mapping granularity for semantic edges (`ENTRY_POINT` / `SERVICE_LAYER`) and wiring inference details when `@Autowired` is absent are implementation choices that must remain deterministic and stable.

---

## Deferred Ideas
- Enriching beyond MVP stereotypes (e.g., deeper Spring MVC annotations).
- SPI-based dynamic plugin registration.
- Plugin ordering/versioning strategies beyond deterministic hardcoded registration.

