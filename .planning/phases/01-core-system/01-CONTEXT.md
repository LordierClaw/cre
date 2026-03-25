# Phase 1: Core System - Context

**Gathered:** 2026-03-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver a minimal demo-ready MCP server that supports:
- structured `get_context` (JSON slice + metadata/provenance + confidence + slice boundaries),
- deterministic controller → service → implementation reconstruction over an AST-backed graph,
- confidence/explainability and a stable placeholder contract,
- `find_implementations` and `trace_flow`,
while preserving core/plugin separation and the Phase 0 symbol-resolution contract.

</domain>

<decisions>
## Implementation Decisions

### `get_context` JSON contract (normalized slice)
- **D-01:** `get_context` uses the canonical **normalized slice contract** as the v1 output shape (normalized structure, not flat text only), with slice boundaries and provenance metadata included so downstream tools can reliably interpret slices.

### Placeholder contract (structured placeholders)
- **D-02:** Placeholders are **structured placeholder objects** (not just text markers), including enough metadata to guide the client toward the most likely next `expand` target (e.g., omission kind, reason, optional `target_node_id`, and slice-boundary context).

### Stable `node_id` identity scheme
- **D-03:** `node_id` is **identity-based and stable**, derived from `(fullyQualifiedType, memberSignature, sourceOrigin)` rather than AST spans/line offsets, to prevent drift across reloads/expansions.

### Spring Boot server startup strategy
- **D-04:** For Phase 1, run the MCP server with an **embedded Spring Boot runtime** (start the Spring application context) so Spring-based plugin wiring works naturally.

### Claude's Discretion

Where not specified above, remaining implementation choices are left to the builder (planner/researcher), as long as they respect:
- Phase boundary scope (core MCP + reconstruction tools),
- deterministic core behavior first (AI only for gated fallback),
- stable identity/evidence so placeholders can be replaced correctly later.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project & phase requirements
- `.planning/PROJECT.md` — vision, non-negotiables, constraints, tool-loop intent
- `.planning/REQUIREMENTS.md` — v1 acceptance criteria for Phase 1 capabilities (`CTX-01`, `REC-01`, `CONF-01`, `PLC-01`, `IMPL-01`, `TRCE-01`)
- `.planning/ROADMAP.md` — phase boundaries and success criteria for Phase 1

### Architecture & workplan
- `.docs/WORKPLAN.md` — intended phased delivery for CRE (Phase 1 ordering + tasks)
- `.docs/ARCHITECURE_SOLUTION.md` — core pipeline and component breakdown used to structure implementation

### Research (stack + domain guidance)
- `.planning/research/STACK.md` — pinned stack/version recommendations for v1
- `.planning/research/FEATURES.md` — feature landscape and MVP definition
- `.planning/research/ARCHITECTURE.md` — recommended component boundaries and patterns
- `.planning/research/PITFALLS.md` — failure modes to prevent (identity drift, mismatch, pruning)
- `.planning/research/SUMMARY.md` — exec summary + roadmap implications

### Workflow enforcement
- `.cursor/rules/CRE.md` — baseline GSD workflow guidance for this project

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- None yet: this repository currently has no `src/` directory or build files (`pom.xml`/`build.gradle`) to reuse for Phase 1 implementation scaffolding.

### Established Patterns
- No existing implementation patterns are present yet in code (Phase 1 will define the initial project structure from scratch).

### Integration Points
- The Phase 1 deliverable must integrate with the v1 MCP tool surface and reconstruction pipeline described in `.docs/ARCHITECURE_SOLUTION.md`.
- Future phases will extend expansion/ranking/plugins; Phase 1 decisions above ensure those later steps can rely on stable schemas and identity.

</code_context>

<specifics>
## Specific Ideas

- Core priority for Phase 1 is correctness + traceability first, with a minimal stable contract so `expand` later can replace placeholders deterministically.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within Phase 1 scope.

</deferred>

---
*Phase: 01-core-system*
*Context gathered: 2026-03-25*

