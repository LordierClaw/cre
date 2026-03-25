# Pitfalls Research

**Domain:** CRE (AST + graph + slicing + plugins + expand-on-demand)
**Researched:** 2026-03-25
**Confidence:** MEDIUM

## Critical Pitfalls

### 1. Node Identity Drift Across Expansions

**What goes wrong:**
Expanded nodes become “different” conceptual entities (overloads/generics/inner classes).

**Why it happens:**
Node IDs derived from unstable attributes (file path/line spans/classpath differences between runs).

**How to avoid:**
- Define stable identity schema (e.g., fullyQualifiedType + member signature + source-kind).
- Version schema and persist it with graph snapshots.
- Deduplicate expansions by identity, not by traversal order.

**Warning signs:**
- Missing content after reload
- Duplicate context blocks for the same method

**Phase to address:**
Phase 3 (expand-on-demand)

---

### 2. AST–Graph–Slice Mismatch (Semantic Gaps)

**What goes wrong:**
Traversal follows graph edges, but emitted slices lack the corresponding AST evidence/call-site context.

**Why it happens:**
Normalization rules differ between graph construction, slicing, and formatting.

**How to avoid:**
- Ensure slicing consumes the exact same normalized node objects produced by graph build.
- Store evidence links (AST origin + normalization result) per slice segment.

**Warning signs:**
- Slice claims a call path but emitted context omits the call-site.

**Phase to address:**
Phase 1 (core system)

---

### 3. Depth Control Off-by-One + Cycle Handling Failures

**What goes wrong:**
Either context starvation (prune too early) or infinite recursion on cycles (mutual calls/builders/event handlers).

**Why it happens:**
Depth semantics inconsistent (hop vs edge-count), and cycle breakers use transient keys.

**How to avoid:**
- Define depth semantics explicitly.
- Cycle detection based on stable identity.
- Hard caps: max visited nodes, max expansions, time/token budgets.

**Warning signs:**
- Latency spikes in cycle-heavy code.

**Phase to address:**
Phase 1 and Phase 3

---

### 4. Plugin Contract Drift (Hook Ordering + Side Effects)

**What goes wrong:**
Plugins interfere with each other depending on ordering; adding/changing plugins breaks unrelated context.

**Why it happens:**
Underspecified plugin API (mutate-in-place ambiguity), missing deterministic ordering rules, no versioning contract.

**How to avoid:**
- Make plugins deterministic and order-stable (priority + stable name).
- Prefer pure transforms/overlays; isolate side effects.
- Version plugin interface and enforce compatibility levels.

**Warning signs:**
- Results change when a seemingly unrelated plugin is toggled.

**Phase to address:**
Phase 2 (plugin system) and Phase 5 (advanced plugins)

---

### 5. Expand-on-demand Creates Stale or Contradictory Edges

**What goes wrong:**
After partial reload, new edges contradict old ones (duplicates/staleness).

**Why it happens:**
Reload merges by superficial keys and misses invalidation rules.

**How to avoid:**
- Introduce invalidation policy for affected identities.
- Store snapshot hashes/config hashes with each expansion.
- Reconcile merges by stable identity + evidence categories.

**Warning signs:**
- Same query yields different slice content after multiple expands.

**Phase to address:**
Phase 3

---

### 6. Ranking Over-prunes Critical Evidence

**What goes wrong:**
Heuristics remove the only correct causal path (exceptions/events/factories/indirection).

**Why it happens:**
Scoring too aggressive; noise reduction blended with correctness requirements.

**How to avoid:**
- Separate correctness constraints (hard evidence categories) from soft ranking.
- Guarantee minimum edge counts per required evidence category before pruning.

**Warning signs:**
- Fixes require increasing depth manually, but ranking still misses key paths.

**Phase to address:**
Phase 4 (ranking & pruning)

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|---------------------|----------------|------------------|
| Stringly-typed node IDs | Quick to implement | Breaks reload/dedupe; causes drift | Never for core identity |
| Mixed concerns (reconstruction + rendering) | Faster output | Hard to test correctness; output changes destabilize logic | Only temporary in Phase 0 |
| Inconsistent evidence/provenance | Easier formatting | Users cannot trust context; evaluation becomes noisy | Never for v1 |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|------------------|------------------|
| MCP stdio framing | Assuming chunking won’t split JSON | Use robust JSON-RPC/transport framing and test with pathological chunking |
| Maven/Spring Boot packaging | Using runtime classpath without sources | Keep build configuration deterministic; ensure parsing has access to sources/dependency info needed |
| JavaParser parsing edge cases | Treating generics/lambdas as simple calls | Add targeted fixtures; treat resolution as fallible and gated |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|-----------------|
| Unbounded neighborhood expansion | token blow-up / latency collapse | hard caps + dedupe ledger + caching | when > ~100 new nodes per expand round |
| Re-slicing too many candidates | CPU spike | slice only after ranking shortlist | when candidate set > ~5k |
| Evidence-rich formatting overload | payload too large | enforce snippet budgets + structured summaries | when emitted snippet > ~200KB |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Path traversal via plugin inputs | data exfiltration | validate and constrain filesystem paths to project root |
| Executing build steps during analysis | RCE risk | never execute untrusted code; parse/build metadata only |
| Context injection via plugin output | prompt/model confusion | escape/normalize emitted markers; keep strict structured fields |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|--------------|------------------|
| “Expand” feels random | users can’t predict outcome | show why expansion happened (evidence category missing) |
| Unknowns not surfaced | users assume correctness | always emit confidence + evidence quality + what was excluded |

## "Looks Done But Isn't" Checklist

- [ ] Output looks complete, but lacks traceability (why included/excluded missing).
- [ ] Node IDs appear stable for one run but drift after reload/refactor.
- [ ] Expansion appears to work once but contradicts earlier context after repeated expands.
- [ ] Ranking improves token reduction but silently reduces correctness.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Node identity drift | MEDIUM | Freeze stable identity schema; add reload parity tests; dedupe by identity |
| AST–graph–slice mismatch | MEDIUM | Unify normalization pipeline; require evidence links in formatting |
| Depth/cycle runaway | LOW | Apply hard caps; adjust cycle breakers to stable identity |
| Plugin drift | MEDIUM | Enforce deterministic plugin ordering; add integration tests per plugin |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Node Identity Drift Across Expansions | Phase 3 | Same query + repeated expand yields convergent, deduped output |
| AST–Graph–Slice Mismatch | Phase 1 | Slice contains call-site evidence matching graph edges |
| Depth Control Off-by-One + Cycle Failures | Phase 1; Phase 3 | Adversarial cycle fixtures produce bounded runtime |
| Plugin Contract Drift | Phase 2; Phase 5 | Enabling/disabling plugins changes only expected edges |
| Expand Stale/Contradictory Edges | Phase 3 | No contradictions across multiple expand rounds |
| Ranking Over-prunes | Phase 4 | Correctness-required evidence categories never disappear |

## Sources

- CRE principles from `.docs/WORKPLAN.md` and `.docs/ARCHITECURE_SOLUTION.md`
- MCP stdio integration guidance (MCP Java SDK docs)
- JavaParser AST behavior (official parser concepts)

---
*Pitfalls research for: CRE*
*Researched: 2026-03-25*

