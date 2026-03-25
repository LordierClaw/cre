# Phase 5: Advanced Plugins - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves alternatives considered.

**Date:** 2026-03-26
**Phase:** 05-advanced-plugins
**Areas discussed:** plugin categories to ship first

---

## Plugin categories to ship first

| Option | Description | Selected |
|--------|-------------|----------|
| Exception-flow plugin only | Deterministic exception semantics first (`throws`/`catches`/propagation style edges) | ✓ |
| Event-flow plugin only | Producer/consumer semantics first | |
| Exception-flow + event-flow | Implement both in same phase | |
| Domain-rule plugin only | Custom business semantics first | |
| You decide | Delegate selection | |

**User's choice:** Exception-flow plugin only  
**Notes:** Chosen to maximize value with lower scope and integration risk for this phase.

---

## Claude's Discretion

- Precise exception-flow edge naming and enrichment granularity.
- Minimal plugin-registration evolution details, as long as deterministic behavior and compatibility are preserved.

## Deferred Ideas

- Event-flow plugin
- Domain-rule plugin
