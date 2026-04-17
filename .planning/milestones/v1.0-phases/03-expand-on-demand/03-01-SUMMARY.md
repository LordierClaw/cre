---
phase: 03-expand-on-demand
plan: 01
subsystem: mcp
tags: [expand, get_context, placeholders, determinism, bounds, graph]
requires:
  - phase: 02-plugin-system
    provides: Spring semantics edges (ENTRY_POINT) + evidence gating
provides:
  - MCP tool `expand(node_id)` returning `GetContextResponse` shape (same as `get_context`)
  - Bounded deterministic expansion with merged-slice semantics and placeholder replacement observability
  - Indexed reverse traversal for incoming CALLS edges in `GraphEngine`
key-files:
  created:
    - src/test/java/com/cre/tools/ExpandToolContractTest.java
    - src/test/java/com/cre/tools/ExpandToolBoundsTest.java
    - src/test/java/com/cre/tools/ExpandToolDeterminismTest.java
  modified:
    - src/main/java/com/cre/core/graph/GraphEngine.java
    - src/main/java/com/cre/tools/GetContextTool.java
    - src/main/java/com/cre/mcp/CreMcpServer.java
requirements-completed: [EXP-01]
completed: 2026-03-26
---

# Phase 03: Expand-on-demand — Execution Summary

## Outcome

Implemented a bounded, deterministic `expand(node_id)` MCP tool that returns the same response shape as `get_context`, supports merged-slice expansion (derived anchor + target expansion in one stateless response), and preserves placeholder semantics (including fail-soft unknown nodes and Spring completeness placeholders).

## Key behavior

- **Merged-slice**: `expand(node_id)` derives an anchor (prefers nearest reachable `ENTRY_POINT` method via indexed reverse CALLS traversal; lexicographic tie-break) and returns a union of anchor center slice + target expansion slice.
- **Bounds**: expansion uses server-side defaults (`DEFAULT_EXPAND_DEPTH=2`, `MAX_EXPAND_NODES=64`) and degrades gracefully by emitting structured `depth_limit` placeholders.
- **Determinism**: nodes/edges/sliced_code ordering is stable across repeated runs; reverse traversal avoids per-request full edge scans.

## Verification

- `mvn -q -DskipITs test` — **PASS**

## Task Commits

1. **03-01** bounded slice builder + incoming calls index — `120b20d`
2. **03-02** MCP `expand` tool + merged-slice semantics — `6245653`
3. **03-03** tests (contract, bounds, determinism) — `91f91c9`
4. **03-04** placeholder replacement observability assertions — `3e25ecc`

