# Phase 25: Output Optimization & Comment Stripping - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-17
**Phase:** 25-output-optimization-comment-stripping
**Areas discussed:** Comment Stripping Nuance, Formatting Bloat Control, Query/Traversal Throttling, Printing Strategy

---

## Comment Stripping Nuance

| Option | Description | Selected |
|--------|-------------|----------|
| Explicit Only | Only functions the user asked for or that are at depth 0. | |
| Gathered | Any function that the engine decided is relevant enough to show the full body. | ✓ |
| Expanded | Only those explicitly in expanded_functions. | |

**User's choice:** 1.Gathered 2.Target only (class-level) 3.Strip all (for skeletons) 4.Keep all inside target.

---

## Formatting Bloat Control

| Option | Description | Selected |
|--------|-------------|----------|
| Collapse to One | Never allow more than one empty line in a row. | ✓ |
| Collapse All | Strip all empty lines completely for a "dense" view. | |
| Keep Original | Preserve what LexicalPreservingPrinter produces. | |

**User's choice:** 1.Collapse to one 2.New line (markers) 3.No (line length cap) 4.Aggressive Trim but excluded the gathered functions.

---

## Query/Traversal Throttling

| Option | Description | Selected |
|--------|-------------|----------|
| Yes (Truncate) | If output exceeds N tokens, stop and return truncated version. | |
| Yes (Prune) | Automatically reduce depth or prune least relevant nodes. | |
| No | Always return the full reconstructed context requested. | ✓ |

**User's choice:** 1.No (token limit) 2.Dynamic (node limit) 3.Breadth-First Priority 4.No (pre-calculated summary).

---

## Printing Strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Yes | Switch to PrettyPrinter for clean format. | |
| No | Keep LexicalPreservingPrinter for fidelity. | ✓ |

**User's choice:** 1.No (Switching) 2.Regex (Line Breaks) 3.Tag-style (Markers) 4.Hybrid (Lexical for target/gathered).

---

## Claude's Discretion

- Deciding the specific regex patterns for line-break normalization and empty-line collapse.
- Refining the "Dynamic" node cap formula based on depth.

## Deferred Ideas

- **Switch to PrettyPrinter**: Decided to stick with LexicalPreservingPrinter for now to ensure original source code styling is preserved for the agent's view.
