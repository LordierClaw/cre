---
phase: 04
reviewers: [gemini]
reviewed_at: 2026-03-26T02:37:05+07:00
plans_reviewed:
  - .planning/phases/04-ranking-pruning/04-PLAN.md
---

# Cross-AI Plan Review — Phase 04

## Gemini Review

# Phase 4 Plan Review: Ranking & Pruning

## Summary
The implementation plan for Phase 4 is exceptionally thorough and technically grounded, demonstrating a deep understanding of the "deterministic-first" mandate of the Context Reconstruction Engine (CRE). By isolating the ranking logic into a pure `RankingPruner` and focusing on integer-based scoring, the plan avoids common pitfalls like floating-point instability or non-deterministic iteration orders. The integration strategy for the `expand` tool—pruning only after the merge—is architecturally sound and ensures consistent output quality.

## Strengths
- **Deterministic Rigor:** The explicit requirement to avoid `HashSet` iteration, `Random`, and floating-point math, combined with lexical tie-breaking on `node_id`, ensures that the system remains "bit-for-bit" stable across environments.
- **Architectural Separation:** Decoupling the scoring/pruning logic from the MCP/JSON layer into a testable pure module (`RankingPruner`) follows best practices and simplifies unit testing.
- **Contract Integrity:** The plan strictly adheres to prior-phase API contracts by using additive metadata instead of breaking changes, ensuring backward compatibility for existing MCP clients.
- **Roadmap Alignment:** The explicit mapping of success criteria to automated test assertions (e.g., the "accounting identity" test) provides a clear path to phase sign-off.
- **Heuristic Depth:** Inclusion of `USES_FIELD` and structural edge types (e.g., `SERVICE_LAYER`) as scoring signals provides a meaningful baseline for "execution relevance" beyond simple BFS depth.

## Concerns
- **Performance Overhead (MEDIUM):** Task 04-01 suggests "deterministic scans" over `graph.sortedEdges()` for each candidate node to calculate incident edge bonuses. If a graph has a large number of edges (`E`) and many candidates (`N`), this `O(N x E)` approach could lead to latency spikes.
- **Placeholder/Pruning Inconsistency (LOW):** If a node is pruned, but another retained node contains a placeholder that references the pruned node (e.g., as a `target_node_id`), the client might receive a reference to a node not present in the `nodes[]` array. While the plan mentions "re-evaluating placeholders," the specific logic for this edge case needs careful implementation to avoid dangling references.
- **Weight Overflow/Precision (LOW):** Using "milliscore" integers is smart, but if weights for `USES_FIELD` or `CALLS` are too high, the cumulative score could theoretically overflow an `int` if not capped, or lose meaningful differentiation if the scale is too narrow.

## Suggestions
- **Optimization:** In `RankingPruner`, perform a single linear pass over `graph.sortedEdges()` to build a temporary `Map<NodeId, Integer>` of incident edge bonuses or a compact adjacency list for the candidate set before scoring individual nodes. This reduces the complexity to `O(N + E)`.
- **Placeholder Rule:** Explicitly define that if a node `A` is pruned, any `depth_limit` or `budget_limit` placeholders pointing to `A` should be dropped or converted into a new `pruning_limit` placeholder type to maintain graph referential integrity.
- **Scoring Documentation:** In the `RankingPruner` Javadoc, include a small table of the chosen weights for each `EdgeType` and the depth decay formula to serve as a living reference for the heuristic model.
- **Test Case for Merged Pruning:** Add a specific test case in `ExpandToolDeterminismTest` where a node is present in the target slice but gets pruned after being merged with the center slice, verifying `retained_count` reflects the final result only.

## Risk Assessment: LOW
The risk is low because the plan focuses on additive, deterministic improvements that do not destabilize existing functionality. The use of pure logic and extensive testing mitigates the risk of regressions in the core graph traversal. The primary risk is performance-related, which is manageable through standard optimization techniques during the implementation of Task 04-01.

**Verdict:** The plan is ready for execution.

---

## Claude Review

Unavailable for this run (`claude` CLI not installed).

---

## Codex Review

Unavailable for this run (`codex` CLI not installed).

---

## Consensus Summary

Single-reviewer run (Gemini only). No cross-reviewer consensus weighting available.

### Agreed Strengths

- Deterministic-first plan design is strong and aligns with prior phase contracts.
- Separation of ranking logic into a pure module is high quality and test-friendly.
- Additive metadata and unchanged MCP/API surface reduce regression risk.

### Agreed Concerns

- Potential performance cost if scoring repeatedly scans all edges per candidate.
- Placeholder consistency must be explicit when pruned nodes are referenced.
- Integer score scale/overflow boundaries should be constrained and tested.

### Divergent Views

Not applicable (only one external reviewer available).
