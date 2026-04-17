---
phase: 3
reviewers: [gemini]
reviewed_at: 2026-03-25T18:44:55Z
plans_reviewed: [03-PLAN.md]
---

# Cross-AI Plan Review — Phase 3

## Gemini Review

# Phase 3: Expand-on-demand Plan Review

## Summary
The plan for Phase 3 is technically sound and highly focused on maintaining the stateless, deterministic nature of the Context Reconstruction Engine (CRE). By introducing a shared internal builder and a deterministic anchor derivation strategy, the plan avoids the complexity of session management while fulfilling the "merged-slice" requirement. The use of hard limits (depth + node budget) provides necessary safety against graph explosion, and the cleanup logic for placeholders ensures the output remains coherent for iterative agent tool-loops.

## Strengths
- **Logic Reuse:** Refactoring `GetContextTool` into a shared internal builder ensures that `get_context` and `expand` share the same traversal rules and ordering logic, reducing the risk of contract drift.
- **Stateless Merged-Slices:** The strategy to derive an "anchor" by walking the reverse call graph to find an `ENTRY_POINT` is a clever way to provide context-rich expansions without requiring the client to pass back previous state or the server to store it.
- **Safety Guardrails:** Implementing a `MAX_EXPAND_NODES` limit in addition to depth provides a multi-dimensional defense against pathological code structures (e.g., extremely wide fan-outs).
- **Deterministic Tie-breaking:** Using lexicographical sorting for anchor selection and node/edge ordering ensures that the system remains reproducible, which is critical for AI tool stability.
- **Clean Placeholder Management:** The rule to remove `depth_limit` placeholders if the target is now present in the `nodes` set directly addresses the "observability" requirement.

## Concerns
- **Anchor Ambiguity (Medium):** If a method is shared by many controllers (e.g., a common utility or a base service), picking the "lexicographically smallest" entry point might provide context for a controller the AI isn't currently interested in.
  - *Risk:* The AI might receive a slice that feels "disconnected" from its current task if the arbitrary tie-break picks a distant entry point.
- **Reverse Traversal Performance (Low):** Task 03-01 adds `incomingCalls` to `GraphEngine`. If this is implemented as a simple filter over all edges on every call, performance will degrade as the project size grows.
  - *Risk:* Slow response times for `expand` in large repositories.
- **Merged Slice Volume (Low):** Unioning two slices (anchor + target) could result in a large JSON payload. While `MAX_EXPAND_NODES` helps, there is no mention of a character or token-based hard limit for the `sliced_code` section specifically.

## Suggestions
- **Proximity-based Anchor:** In Task 03-02, consider modifying the anchor derivation to prefer the *closest* `ENTRY_POINT` (shortest path) before falling back to lexicographical sorting. This is more likely to provide the "relevant" execution context.
- **Index Incoming Edges:** Ensure the implementation of `incomingCalls` in `GraphEngine` uses a pre-computed map or index (e.g., `Map<NodeId, List<GraphEdge>>`) rather than a full edge-list scan.
- **Metadata Provenance:** Add the `node_id` of the derived anchor to the `metadata` block (e.g., `metadata.derived_anchor`). This helps the AI (and developers) understand why a specific controller's context was included in the expansion.
- **Explicit "Reason" in Placeholders:** When emitting new placeholders during expansion, explicitly flag if they were caused by the `MAX_EXPAND_NODES` budget vs. the `depth` limit to give the agent better feedback on why it's hitting a wall.

## Risk Assessment
**Risk Level: LOW**

The plan is well-contained and leverages existing patterns from Phases 1 and 2. The most complex part (anchor derivation) is deterministic and fail-soft (falling back to `target_only_fallback`). The testing wave is comprehensive, covering the core requirements and edge cases like unknown nodes. The primary risks are performance-related (traversal overhead) rather than functional, which are manageable within the current scope.

---

## Consensus Summary

Single reviewer available this run (`gemini`), so consensus reflects Gemini's assessment.

### Agreed Strengths
- Plan is deterministic-first and aligned with prior phase contracts.
- Shared builder approach reduces drift between `get_context` and `expand`.
- Bounds plus placeholder cleanup provide safe fail-soft behavior.

### Agreed Concerns
- Anchor-selection strategy may choose context that is less relevant for some call paths.
- Reverse-edge traversal may need indexing for scale.
- Merged payload size could grow without a text/token-oriented guardrail.

### Divergent Views
- None (single external reviewer available).
