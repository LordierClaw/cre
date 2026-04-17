---
phase: 04
slug: ranking-pruning
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-26
---

# Phase 04 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Maven Surefire) |
| **Config file** | `pom.xml` |
| **Quick run command** | `mvn -q -DskipITs test -Dtest=ExpandToolDeterminismTest,GetContextSchemaTest` |
| **Full suite command** | `mvn -q -DskipITs test` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn -q -DskipITs test -Dtest=ExpandToolDeterminismTest,GetContextSchemaTest`
- **After every plan wave:** Run `mvn -q -DskipITs test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 20 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 01 | 1 | NOISE-01 | unit | `mvn -q -DskipITs test -Dtest=ContextRankingScoringTest` | ✅ | ✅ green |
| 04-01-02 | 01 | 1 | NOISE-02 | unit | `mvn -q -DskipITs test -Dtest=ContextPruningPolicyTest` | ✅ | ✅ green |
| 04-01-03 | 01 | 1 | API-01 | integration | `mvn -q -DskipITs test -Dtest=GetContextSchemaTest,ExpandToolContractTest` | ✅ | ✅ green |
| 04-01-04 | 01 | 1 | DET-01 | integration | `mvn -q -DskipITs test -Dtest=ExpandToolDeterminismTest` | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠ flaky*

---

## Wave 0 Requirements

- [x] `src/test/java/com/cre/tools/ContextRankingScoringTest.java` — scoring/tie-break + saturation assertions
- [x] `src/test/java/com/cre/tools/ContextPruningPolicyTest.java` — top-k + score-floor behavior assertions

*Existing infrastructure covers framework execution and fixture setup.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Ranking telemetry usefulness in debugging | OBS-01 | Requires human judgment on readability | Run `get_context` and `expand` on sample node IDs; inspect metadata compactness and clarity (`ranking_version`, `pruned_count`, `retained_count`, `prune_policy`, `top_k`, `score_floor`, `score_components_used`). |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 20s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-03-26

## ROADMAP Success Criteria Mapping

- Criterion 1 (heuristic prioritization visible):
  - `ContextPruningPolicyTest` enforces top-k + score-floor retention behavior.
  - `GetContextSchemaTest` asserts `retained_count`/`pruned_count` telemetry exists.
- Criterion 2 (variable/field-style signals included):
  - `ContextRankingScoringTest` covers deterministic scoring and tie-break behavior with structured graph signals.
  - `GetContextSchemaTest` asserts `score_components_used` is published in metadata.
- Criterion 3 (measured size/noise improvement without API break):
  - `ContextPruningPolicyTest` asserts `retained_count + pruned_count` accounting and non-zero pruning when active.
  - `ExpandToolDeterminismTest` asserts post-merge prune keeps `retained_count == nodes.size()` and no dangling `target_node_id`.
  - `GetContextSchemaTest` + `ExpandToolContractTest` keep v1 response envelope compatibility checks green.
