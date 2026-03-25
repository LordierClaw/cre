# Phase 1 — Research

**Phase:** 01
**Phase Name:** Core System
**Researched:** 2026-03-25
**Confidence:** MEDIUM

## Scope (Phase Boundary)

Deliver a minimal demo-ready MCP server that:
- exposes structured `get_context` returning a normalized slice (nodes/edges + `sliced_code[]` + slice boundaries + provenance/confidence),
- performs deterministic controller → service → reachable implementation reconstruction using an AST-backed graph enriched by Spring semantics hooks,
- emits a stable, structured placeholder contract (metadata-rich) when parts are missing,
- includes `find_implementations` and `trace_flow` for the reconstructed entry path,
- preserves core/plugin separation (core is deterministic; AI only fills gated gaps later).

## Locked Decisions Impacting Planning

Carried from `.planning/phases/01-core-system/01-CONTEXT.md`:

1. `get_context` output must be a **normalized slice contract** (structured, not flat snippets only).
2. Placeholders must be **structured placeholder objects** with metadata guiding “likely next expand”.
3. `node_id` must be **identity-based + stable**: `(fullyQualifiedType, memberSignature, sourceOrigin)` (no line/offset dependence).
4. Phase 1 uses **embedded Spring Boot runtime** so Spring plugin wiring works naturally.

## Implementation Research Notes (for planning)

### JSON contract: what needs to be explicit for deterministic consumers
- Schema versioning fields: include `slice_version` (string) and `build_variant` (string) to make future changes auditable.
- Provenance/evidence quality: for each included slice segment, record evidence category:
  - `deterministic_ast` / `spring_semantics` / `heuristic_repair` / `gated_fallback`
- Placeholder contract: each placeholder object should include:
  - `kind` (e.g., `omitted_logic`, `missing_implementation`, `external_call`)
  - `reason`
  - optional `target_node_id` (or `target_identity` if node_id is not yet resolvable)
  - `likely_next_tool` (for Phase 3: normally `expand`)
  - `slice_boundary` (what was intentionally excluded and why)

### Stable identity & determinism
- Node IDs must be derived from semantic identity, not AST span volatility.
- Define `sourceOrigin` so it is deterministic per build/repo snapshot:
  - e.g., normalized relative path + build variant name + parser mode

### Spring runtime expectations in Phase 1
Because Phase 1 uses embedded Spring Boot:
- plugin wiring can rely on Spring configuration and bean discovery,
- but reconstruction must remain deterministic even if the Spring context contains optional auto-config.

### Validation architecture (Nyquist / Dimension 8)

## Validation Architecture

Create automated tests that verify:
1. `node_id` stability across repeated parses and across re-expansion.
2. `get_context` returns valid schema and includes required evidence/provenance categories.
3. Placeholder objects appear when appropriate and include correct `likely_next_tool` and slice-boundary metadata.
4. Tool routing for MCP stdio server returns the expected tool outputs (at least for a single fixture codebase).

This phase’s validation can use JUnit 5 + JSON schema validation (or explicit field assertions) and can run on a minimal test fixture repository created during Phase 1 execution.

## Open Questions / Needs External Research

None for now — codebase can be seeded from scratch in Phase 1 so the tests and schema can be enforced without relying on external assets.

---
*Researched: 2026-03-25*

