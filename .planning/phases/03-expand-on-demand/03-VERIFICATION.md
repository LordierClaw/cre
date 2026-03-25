---
phase: 03-expand-on-demand
status: passed
updated: 2026-03-26
requirements: [EXP-01]
---

# Phase 03 — Verification

## Must-haves (EXP-01)

- [x] MCP tool `expand` exists and accepts required `node_id` input only.
- [x] `expand` returns `GetContextResponse` (schema parity with `get_context`).
- [x] Stateless merged-slice behavior is observable: anchor + target coexist and replaced `depth_limit` placeholder disappears after expansion.
- [x] Expansion is bounded (depth + node budget safeguards) and degrades gracefully with structured placeholders.
- [x] Unknown node remains fail-soft (`missing_node` placeholder; no hard MCP tool error).
- [x] Spring semantics incompleteness remains visible (`missing_spring_mapping` placeholder + evidence gating).
- [x] Deterministic ordering for nodes/edges/sliced_code is regression-tested.

## Evidence

- Automated tests:
  - `ExpandToolContractTest` (schema + merged-slice + placeholder replacement + spring semantics placeholder)
  - `ExpandToolBoundsTest` (bounded expansion emits `depth_limit` placeholder; metadata `expansion_limit_reason=depth`)
  - `ExpandToolDeterminismTest` (stable ordering on repeated calls)
- Command: `mvn -q -DskipITs test` — PASS

