# Phase 1: Core System - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions captured in `01-CONTEXT.md` — this log preserves the alternatives considered.

**Date:** 2026-03-25
**Phase:** 01-core-system
**Areas discussed:** get_context JSON contract, placeholder contract, stable node_id identity scheme, Spring Boot startup strategy

---

## `get_context` JSON contract (normalized slice)

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Normalized slice contract (nodes/edges + `sliced_code[]` + boundaries + provenance/metadata) | ✓ |
| 2 | Flat snippets contract (metadata + list of code snippets, minimal structure) | |
| 3 | Both (largest payload) | |

**User's choice:** Option 1 (normalized slice)
**Notes:** Locked to normalized slice contract for v1 tool stability.

---

## Placeholder contract (structured placeholders)

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Structured placeholder objects with metadata guidance for likely next `expand` | ✓ |
| 2 | Text-only placeholders embedded in `sliced_code[]` | |
| 3 | Hybrid (structured metadata + text markers) | |

**User's choice:** Option 1 (structured placeholder objects)
**Notes:** Placeholders must contain metadata for what was omitted and how to expand.

---

## Stable `node_id` identity scheme

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Identity-based and stable: `(fullyQualifiedType, memberSignature, sourceOrigin)` (avoid line/offset volatility) | ✓ |
| 2 | Span-based: `(filePath, startLine/startCol)` | |
| 3 | Hybrid: identity-based primary key + span as secondary debug field | |

**User's choice:** Option 1 (identity-based stable node_id)
**Notes:** Required so placeholders can later be replaced deterministically.

---

## Spring Boot server startup strategy

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Minimal standalone MCP process (Spring only for packaging/plugins; no full context init) | |
| 2 | Embedded Spring Boot runtime (start Spring application context so plugins wire naturally) | ✓ |
| 3 | Hybrid: start minimal MCP, lazily init Spring when Spring plugin first invoked | |

**User's choice:** Option 2 (embedded Spring Boot runtime)
**Notes:** Chosen to make plugin wiring simpler for Phase 1.

---

## Claude's Discretion

- No areas were delegated to “you decide” explicitly during selection; remaining implementation details stay flexible as long as they respect the locked schemas and phase boundary.

