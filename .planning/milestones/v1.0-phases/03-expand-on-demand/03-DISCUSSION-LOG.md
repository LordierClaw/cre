# Phase 3: Expand-on-demand - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-03-25
**Phase:** 03-expand-on-demand
**Areas discussed:** API contract, bounding strategy, placeholder replacement, determinism/caching, error handling

---

## API Contract

| Option | Description | Selected |
|--------|-------------|----------|
| Full-shape response | Return same shape as `get_context` (full normalized slice) | ✓ |
| Delta/patch response | Return only incremental graph/slice updates | |

**User's choice:** Full-shape response
**Notes:** Keep compatibility with existing consumers and avoid introducing patch merge complexity in v1.

| Option | Description | Selected |
|--------|-------------|----------|
| `node_id` only | Server controls bounds with defaults | ✓ |
| `node_id` + `depth` | Client controls depth explicitly | |
| `node_id` + budget | Client controls node/edge budgets | |
| `node_id` + `depth` + budget | Full explicit controls | |

**User's choice:** `node_id` only
**Notes:** Minimize API surface for v1; planner can still choose internal safeguards.

---

## Bounding Strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Depth-only | Bound expansion by depth | ✓ |
| Node-budget-only | Bound by max nodes | |
| Depth + node budget | Dual-guard strategy | |

**User's choice:** Depth-only
**Notes:** Keep model simple initially.

| Option | Description | Selected |
|--------|-------------|----------|
| Succeed with placeholders | Return partial expanded slice plus new omission placeholders | ✓ |
| Fail with error | Abort when limit reached | |

**User's choice:** Succeed with placeholders
**Notes:** Prefer fail-soft behavior for agent loops.

---

## Placeholder Replacement Rules

| Option | Description | Selected |
|--------|-------------|----------|
| Target-only slice | Expansion centers only on target node | |
| Merged slice | Include original center + target expansion | ✓ |

**User's choice:** Merged slice
**Notes:** Expansion should feel like widening existing context, not switching focus entirely.

| Option | Description | Selected |
|--------|-------------|----------|
| Client-resolved replacement | Server returns expanded data; client infers replacement | ✓ |
| Explicit `replaces[]` | Server marks placeholders replaced | |

**User's choice:** Client-resolved replacement
**Notes:** Avoid introducing extra replacement contract semantics in v1.

---

## Determinism & Caching

| Option | Description | Selected |
|--------|-------------|----------|
| Pure recompute | Deterministic stateless expansion | ✓ |
| Sessionful expansion | Track expansion session IDs and cache state | |

**User's choice:** Pure recompute
**Notes:** Determinism and simplicity over session optimization.

---

## Error Handling

| Option | Description | Selected |
|--------|-------------|----------|
| Fail-soft placeholder | Return valid response with `missing_node` placeholder | ✓ |
| Hard error | Return tool-level failure for unknown node | |

**User's choice:** Fail-soft placeholder
**Notes:** Keeps behavior aligned with existing `get_context` contract.

| Option | Description | Selected |
|--------|-------------|----------|
| Not depth-expandable | `missing_spring_mapping` remains plugin-completeness concern | ✓ |
| Potentially depth-resolvable | Expansion may eventually clear it | |

**User's choice:** Not depth-expandable
**Notes:** Distinguish semantic mapping gaps from depth traversal gaps.

---

## Claude's Discretion

- Internal default depth values and guardrail tuning.
- Internal state needed to merge original center with expansion target while maintaining deterministic output ordering.

## Deferred Ideas

- Sessionful expansion IDs and cache semantics.
- Patch/delta response contract with explicit replacement metadata.
- Richer API-level budgeting controls.
