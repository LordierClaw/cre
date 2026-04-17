---
phase: 09
slug: output-format-optimization
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-03-28
---

# Phase 09 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + AssertJ |
| **Config file** | pom.xml |
| **Quick run command** | `mvn test -Dtest=GetContextSchemaTest` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run task-specific test (e.g., `mvn test -Dtest=GetContextSchemaTest`)
- **After every plan wave:** Run `mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds |

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 09-01-01 | 01 | 1 | PH9-01 | unit | `mvn compile` | ✅ | ⬜ pending |
| 09-01-02 | 01 | 1 | PH9-01, PH9-02 | unit | `mvn test -Dtest=GetContextSchemaTest` | ✅ | ⬜ pending |
| 09-02-01 | 02 | 2 | PH9-02, PH9-03 | unit | `mvn compile` | ❌ | ⬜ pending |
| 09-02-02 | 02 | 2 | PH9-03 | unit | `mvn test -Dtest=IntegratedViewBuilderTest` | ❌ | ⬜ pending |
| 09-02-03 | 02 | 2 | PH9-03 | unit | `mvn test -Dtest=IntegratedViewBuilderTest` | ❌ | ⬜ pending |
| 09-03-01 | 03 | 3 | PH9-01, PH9-02, PH9-03 | unit | `mvn test -Dtest=GetContextSchemaTest` | ✅ | ⬜ pending |
| 09-03-02 | 03 | 3 | PH9-03 | e2e | `mvn test -Dtest=RealProjectE2ETest` | ✅ | ⬜ pending |
| 09-03-03 | 03 | 3 | PH9-01, PH9-02, PH9-03 | suite | `mvn test` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/com/cre/tools/GetContextSchemaTest.java` — Needs update for new JSON structure. (Addressed in 09-01-02)
- [ ] `src/test/java/com/cre/e2e/RealProjectE2ETest.java` — Needs update to verify integrated code content. (Addressed in 09-03-02)
- [ ] `src/test/java/com/cre/tools/IntegratedViewBuilderTest.java` — New test needed for AST transformation. (Addressed in 09-02-02)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Output Readability | D-07 | Subjective readability | Manually inspect `get_context` tool output from a running MCP session to ensure it is clear for both humans and agents. |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
