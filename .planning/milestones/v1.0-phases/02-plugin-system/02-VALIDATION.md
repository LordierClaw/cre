---
phase: 02-plugin-system
slug: plugin-system
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-25
---

# Phase 02 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + AssertJ |
| **Config file** | `pom.xml` |
| **Quick run command** | `mvn -q -DskipITs test` |
| **Full suite command** | `mvn -q -DskipITs test` |
| **Estimated runtime** | ~60 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn -q -DskipITs test`
- **After every plan wave:** Run `mvn -q -DskipITs test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|-------|-------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 1 | PLUG-01 | unit | `mvn -q -DskipITs test` | ✅ / ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/**` — add tests/fixtures for plugin enable/disable and missing mapping placeholders
- [ ] `pom.xml` — ensure Surefire discovers new JUnit 5 tests

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|-------------|-------------------|
| MCP stdio tool loop returns evidence and placeholders consistent with plugin enable/disable | PLUG-01 | Hard to simulate end-to-end MCP framing deterministically in unit tests | Run server locally and call `get_context` with `depth=0`, confirm metadata and placeholder payload |

---

## Validation Sign-Off

- [ ] All tasks have `<verify>` or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all missing references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending 2026-03-25

