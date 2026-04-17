---
phase: 01-core-system
slug: core-system
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-25
---

# Phase 01 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + AssertJ (and optional JSON schema validation) |
| **Config file** | `pom.xml` (tests run via Maven Surefire) |
| **Quick run command** | `mvn -q -DskipITs test` |
| **Full suite command** | `mvn -q -DskipITs test` (same for MVP) |
| **Estimated runtime** | ~60 seconds (MVP-sized fixtures) |

---

## Sampling Rate

- **After every task commit:** Run `mvn -q -DskipITs test`
- **After every plan wave:** Run `mvn -q -DskipITs test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01 | 1 | CTX-01 | unit | `mvn -q -DskipITs test` | ✅ / ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/**` — stubs/fixtures for schema + placeholder behaviors
- [ ] `pom.xml` — ensure Surefire discovers JUnit 5 tests

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| MCP stdio tool loop works end-to-end with a real host | REC-01 / TRCE-01 | Hard to simulate deterministic host orchestration automatically in unit tests | Run server locally and invoke MCP tools via a minimal client harness; confirm returned JSON structure and trace output are coherent |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending 2026-03-25

