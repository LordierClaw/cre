# Feature Research

**Domain:** Replace RAG with deterministic/heuristic reconstruction of execution-relevant code context for a given Spring Boot symbol (`Controller.method`), with strict core+plugin architecture. Expand/replace context on demand via MCP tools.
**Researched:** 2026-03-25
**Confidence:** MEDIUM

## Feature Landscape

### Table Stakes (Users Expect These)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Canonical query input `Controller.method` | Consistent addressing | LOW | Normalize controller type + method name, handle overloads deterministically |
| MCP tool integration for context lifecycle | Users must be able to drive the engine | MEDIUM | Orchestrator calls `find_symbol` → `get_context` → `expand` |
| Structured `get_context` output (JSON + metadata) | Machine-readable slices | MEDIUM | Includes sliced code plus provenance/confidence + placeholders |
| Deterministic → heuristic → AI fallback | Reliability before free-form generation | MEDIUM | AI only fills gated gaps, not core truth |

### Differentiators (Competitive Advantage)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Execution-relevant reconstruction (no vector RAG) | Faithful, reproducible slices | HIGH | AST + graph traversal + slicing grounded in code structure |
| Confidence + explainability per slice | Trust + easier debugging | MEDIUM | “Why this is included” + evidence quality categories |
| Strict core+plugin architecture | Framework semantics without core rewrites | HIGH | Plugins annotate roles/edges (Spring, events, internal SDKs) |
| Interface/implementation discovery + trace flow | Correct service indirections | HIGH | `find_implementations` + `trace_flow` to build call graph |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|----------|----------------|------------------|-------------|
| “Always AI summarize everything” | Quick demos | Non-deterministic; hard to reproduce; may inject irrelevant code | Deterministic/heuristic first, AI only for explicitly gated gaps |
| “Single-shot huge context dump” | One output looks complete | Too much noise; breaks trust; token blow-ups | Incremental slicing + explicit `expand` with budgets |
| “Silent replace without provenance” | Cleaner UX | Hard to debug regressions; users can’t tell why context changed | Structured diffs + provenance on replace/expand |

## Feature Dependencies

```
[AST parsing & signature matching]
    └──requires──> [Graph edges (CALLS/USES_FIELD/BELONGS_TO)]
                       └──requires──> [Slicing engine boundaries + placeholders]

[Spring semantics plugin]
    └──requires──> [annotation-derived roles/edges]

[Confidence schema + explainability]
    └──requires──> [provenance + evidence quality categories]
```

### Dependency Notes

- Signature matching requires AST parsing to avoid drift across formatting refactors.
- Spring plugin semantics require stable node identity so annotation-derived edges stay consistent across expansions.
- Placeholders require a contract that maps omissions to concrete next `expand` calls.

## MVP Definition

### Launch With (v1)

- [ ] `get_context` returns structured JSON slice for `Controller.method` (main payload + metadata).
- [ ] Deterministic reconstruction path: AST indexing + Spring mapping + graph traversal for controller → service → reachable implementations.
- [ ] On-demand `expand` to widen slice scope when context confidence is low (bounded growth).
- [ ] Confidence + explainability fields (provenance steps, evidence type, placeholder reasons).
- [ ] Placeholder output contract for missing/unknown edges.

### Add After Validation (v1.x)

- [ ] Replace-on-demand with provenance diff (“what changed”) when deterministic re-run finds stronger mappings.
- [ ] Advanced slice controls (max depth, edge-type filters, reachability thresholds).
- [ ] Rich trace controls for exception/event/factory flows.

### Future Consideration (v2+)

- [ ] Multi-repo/multi-module orchestration and cross-build consistency checks.
- [ ] Broader framework support beyond Spring (events, internal SDKs, BPM/Kafka-style domain plugins).

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Structured `get_context` JSON + metadata | HIGH | MEDIUM | P1 |
| Deterministic AST+graph reconstruction for controller→service→impl | HIGH | HIGH | P1 |
| Expand-on-demand bounded slice widening | HIGH | MEDIUM | P1 |
| Confidence + explainability | HIGH | MEDIUM | P1 |
| Placeholder contract for missing edges | MEDIUM | MEDIUM | P2 |
| Spring plugin mapping rules | HIGH | HIGH | P1 |
| Interface/implementation discovery + trace flow | HIGH | HIGH | P1 |

**Priority key:**
- P1: Must have for launch
- P2: Should have, add when possible

## Competitor Feature Analysis

| Feature | Competitor A (Classic RAG) | Competitor B (search + LLM) | Our Approach |
|---------|-----------------------------|--------------------------------|--------------|
| Faithfulness | Retrieval drift | Search-dependent | Evidence-grounded reconstruction + provenance |
| On-demand refinement | Re-run search/prompt | Re-run search | Explicit `expand` with bounded budgets + placeholder-driven next steps |
| Explainability | Often implicit | Usually weak | First-class confidence/evidence categories |

## Sources

- MCP tool contract: `find_symbol`, `get_context`, `expand`, `find_implementations`, `trace_flow`
- CRE principles from `.docs/ARCHITECURE_SOLUTION.md` (replace/expand on demand, core+plugin)
- Spring Boot conventions for controller/service semantics (annotations and wiring)

---
*Feature research for: CRE (Context Reconstruction Engine)*
*Researched: 2026-03-25*

