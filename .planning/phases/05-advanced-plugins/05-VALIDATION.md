---
phase: 05
slug: advanced-plugins
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-26
---

# Phase 05 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Maven Surefire) |
| **Config file** | `pom.xml` |
| **Quick run command** | `mvn -q -DskipITs test -Dtest=ExceptionFlowPluginDeterminismTest,GetContextSchemaTest` |
| **Full suite command** | `mvn -q -DskipITs test` |
| **Estimated runtime** | ~20 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn -q -DskipITs test -Dtest=ExceptionFlowPluginDeterminismTest,GetContextSchemaTest`
- **After every plan wave:** Run `mvn -q -DskipITs test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 05-01-01 | 01 | 1 | PLUG-ADV-01 | unit | `mvn -q -DskipITs test -Dtest=ExceptionFlowPluginDeterminismTest` | ✅ | ✅ green |
| 05-01-02 | 01 | 1 | PLUG-ADV-02 | integration | `mvn -q -DskipITs test -Dtest=ExceptionFlowPluginIntegrationTest` | ✅ | ✅ green |
| 05-01-03 | 01 | 1 | COMPAT-01 | integration | `mvn -q -DskipITs test -Dtest=GetContextSchemaTest,ExpandToolContractTest,ContextRankingScoringTest` | ✅ | ✅ green |
| 05-01-04 | 01 | 1 | REG-01 | regression | `mvn -q -DskipITs test` | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/com/cre/tools/ExceptionFlowPluginDeterminismTest.java` — deterministic plugin edge generation checks
- [ ] `src/test/java/com/cre/tools/ExceptionFlowPluginIntegrationTest.java` — get_context / ranking interaction checks
- [ ] `src/test/java/com/cre/fixtures/ExceptionFlowController.java` — fixture with try/catch and exception handler patterns

*Existing infrastructure covers framework execution and core fixture bootstrapping.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Exception-flow edge semantics match intended plugin story | OBS-ADV-01 | Requires semantic judgment on modeled behavior | Run `get_context` on exception fixture entry methods and inspect emitted exception-related edges and placeholders for expected topology. |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

## Roadmap Success Criteria Mapping

| ROADMAP Phase 5 success criteria | Concrete artifacts | Evidence used |
|---|---|---|
| #1 “At least one additional plugin category…” | `ExceptionFlowPlugin` + `ExceptionFlowController` fixture + `ExceptionFlowPluginIntegrationTest` | New `EdgeType.CATCH_INVOKES` edges emitted in `get_context` slice JSON |
| #2 “Third-party or domain plugins can be added without modifying core source…” | `PluginRegistry` fixed ordered list extension (SPI deferred) | `05-RESEARCH.md` roadmap tension + deterministic plugin enable/disable behavior |
| #3 “Plugin interactions with ranking/pruning…” | `RankingPruner` policy/Javadoc + `ContextRankingScoringTest` + integration schema regression tests | `CATCH_INVOKES` has explicit milliscore policy (zero incident) and Phase 4 metadata remains additive |

**Approval:** approved 2026-03-26
