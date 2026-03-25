# Phase 4: Ranking & Pruning - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-03-26
**Phase:** 04-ranking-pruning
**Areas discussed:** scoring model inputs, pruning policy behavior, determinism/tie-break rules, observability/response contract

---

## Scoring model inputs

| Option | Description | Selected |
|--------|-------------|----------|
| Weighted structural scoring | Deterministic multi-signal scoring (edge type, depth, usage signals) | ✓ |
| Depth-first simple scoring | Mostly depth-based with light edge adjustments | |
| Conservative minimal scoring | Very light ranking changes on current behavior | |

**User's choice:** Weighted structural scoring  
**Notes:** User asked what scoring is for and requested detailed explanation of all options before selecting.

---

## Pruning policy behavior

| Option | Description | Selected |
|--------|-------------|----------|
| Top-K + score floor | Keep top K while enforcing minimum quality threshold | ✓ |
| Top-K only | Fixed output size regardless of quality floor | |
| Score-threshold only | Variable output size, quality threshold only | |

**User's choice:** Top-K + score floor  
**Notes:** Chosen as balanced deterministic behavior.

---

## Determinism / tie-break rules

| Option | Description | Selected |
|--------|-------------|----------|
| Stable lexical tie-break | Tie by `node_id` lexical order (edge tuple lexical for edges) | ✓ |
| Traversal-order tie-break | Keep traversal discovery order | |
| Hybrid tie-break | Depth then lexical order | |

**User's choice:** Stable lexical tie-break  
**Notes:** Aligns with prior deterministic behavior across phases.

---

## Observability and response contract

| Option | Description | Selected |
|--------|-------------|----------|
| Compact metadata | Version/counters/policy/components only, no verbose node-level scores | ✓ |
| Verbose per-node explainability | Per-node score breakdown and prune reasons | |
| Minimal metadata | Only ranked/pruned-applied signal | |

**User's choice:** Compact metadata  
**Notes:** Keeps payload efficiency while preserving evaluation/debug visibility.

---

## Claude's Discretion

- Numeric weight calibration and exact initial defaults.
- Measurement thresholds for ranking/pruning quality in tests.

## Deferred Ideas

- Verbose per-node explainability payloads.
- Optional AI-assisted pruning mode.
- User-configurable ranking profiles.
