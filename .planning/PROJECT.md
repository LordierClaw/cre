# CRE

## What This Is

CRE (Context Reconstruction Engine) is a Java + Spring Boot system that reconstructs execution-relevant code context for a target symbol, then slices and progressively expands only what’s needed. It is designed to replace traditional RAG workflows by building a relevance-focused graph from source structure (deterministic -> heuristic -> AI fallback) and exposing that capability through an MCP server.

## Core Value

Enable accurate code changes with drastically fewer tokens by progressively reconstructing the exact execution-relevant context and expanding on-demand only when the missing context is truly required.

## Requirements

### Validated

<!-- Shipped and confirmed valuable. -->
- [x] **EXP-01**: Bounded `expand(node_id)` widens slices on demand (Validated in Phase 03: Expand-on-demand)

### Active

<!-- Current scope. Building toward these. -->
- [ ] MVP v1 scope is being defined via requirements generation (see `.planning/REQUIREMENTS.md`)

### Out of Scope

<!-- Explicit boundaries. Includes reasoning to prevent re-adding. -->
- [Exclusion TBD] — [why]

## Context

- Product goal: replace RAG with structure-aware context reconstruction for code editing/changes.
- Core approach: deterministic -> heuristic -> AI fallback, with strict core/plugin separation.
- Graph model: classes/methods/fields with edges like `CALLS`, `USES_FIELD`, and `BELONGS_TO`.
- Replace-on-demand: unknown/complex parts are represented initially (placeholders) and later replaced via deeper graph traversal and/or plugins.
- Expand-on-demand: the user/agent requests deeper traversal via an `expand(node_id)`-style API when additional context is needed.
- Ranking-and-pruning: context slices are now prioritized with deterministic weighted heuristics and bounded top-k/score-floor pruning to reduce noise while preserving API contracts.
- Target developer workflow: an AI/agent tool loop calls `find_symbol` -> `get_context` -> `expand` (and related MCP functions) to iteratively reconstruct context for edits.
- v1 output shape:
  - An MCP server (stdio transport)
  - `find_symbol` / `trace_flow` accept short input like `Controller.method`
  - `get_context` returns structured output (JSON + metadata), with sliced code as the main payload.

## Constraints

- **Language/Framework**: Java, Spring Boot 3.x — aligns with your Spring-focused context reconstruction goals.
- **Build Tool**: Maven — required for the v1 build pipeline.
- **JDK**: Corretto 21 at `/home/hainn/.jdks/corretto-21.0.10` — used for compilation and runtime assumptions.
- **Maven Installation Path**: `~/.local/share/JetBrains/Toolbox/apps/intellij-idea/plugins/maven/lib` — used for local developer environment compatibility.
- **MCP Transport**: `stdio` — chosen for typical host-agent integration.
- **Input Format**: short symbol format `Controller.method` — used as the canonical v1 query format.
- **Output Format**: `get_context` returns JSON/structured metadata (primary), with sliced code included.
- **MVP Done Metrics**: evaluate balancedly across token reduction, accuracy (edit correctness), and developer usability (workflow friction/time-to-correct-change).

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Build v1 as an MCP server | Matches the intended tool-loop usage | ✓ Good |
| Scope v1 to Java + Spring Boot | Spring semantics are central to the MVP reconstruction behavior | ✓ Good |
| Canonical symbol input: `Controller.method` | Matches WORKPLAN outputs and keeps the API agent-friendly | ✓ Good |
| `get_context` output is structured (JSON + metadata) | Needed for confidence/explainability and downstream formatting | ✓ Good |
| MCP transport: `stdio` | Simplifies host integration patterns for local tooling | ✓ Good |
| Evaluate MVP with balanced metrics | Avoids optimizing one dimension while regressing others | ✓ Good |
| Java toolchain: Corretto 21 | Ensures consistent AST parsing and runtime expectations | ✓ Good |
| Build tool: Maven | Aligns with your environment and v1 packaging workflow | ✓ Good |
| Spring Boot major: 3.x | Required baseline for annotation/plugin mapping | ✓ Good |

## Evolution

This document evolves at phase transitions and milestone boundaries.

After each phase transition (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

After each milestone (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---

*Last updated: 2026-03-26 after Phase 04 completion*

