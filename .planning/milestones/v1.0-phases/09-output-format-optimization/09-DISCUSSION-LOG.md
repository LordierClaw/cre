# Phase 09: Output Format Optimization - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-03-28
**Phase:** 09-output-format-optimization
**Areas discussed:** Output Structure, Placeholder Embedding Depth, Path and ID Compactness, Redundant Metadata Removal, Placeholder Format, Node Map Inclusion

---

## Output Structure: Integrated vs. Split

| Option | Description | Selected |
|--------|-------------|----------|
| File-centric Integrated View | Group code by file, reconstructing the class structure with imports and class declarations, but omitting unselected methods/fields using placeholders. | ✓ |
| Method-centric List View | List selected methods/fields individually but include their minimal class context for each entry. | |

**User's choice:** File-centric Integrated View
**Notes:** Reconstructing the class structure provides better context for both humans and agents, making it feel like reading the actual source code.

---

## Placeholder Embedding Depth

| Option | Description | Selected |
|--------|-------------|----------|
| Comment-appended Tags | Append the XML tag as a comment at the end of the line (e.g., `service.call(); // <node_id/>`). | |
| Block/Call Replacement Tags | Replace the entire method call or omitted block with the XML tag (e.g., `<ommitted_NN/>`). | ✓ |

**User's choice:** Block/Call Replacement Tags
**Notes:** Directly replacing omitted parts with tags makes the output more compact and emphasizes what logic is actually being presented.

---

## Path and ID Compactness

| Option | Description | Selected |
|--------|-------------|----------|
| Short Numeric IDs | Use simple class names in the code view and map them to short IDs (like `<node_01/>`) that the agent can use for `expand`. | ✓ |
| Descriptive Slugs | Use simple class names but include the FQN or a stable slug in the tag. | |

**User's choice:** Short Numeric IDs
**Notes:** Combined with a mapping in the response, this keeps the code snippets extremely clean.

---

## Redundant Metadata Removal

| Option | Description | Selected |
|--------|-------------|----------|
| Remove Redundant Lists | Yes, remove 'edges' and 'nodes' lists; the integrated code with tags is sufficient. | ✓ |
| Keep Minimal Node Metadata | Keep a minimal 'nodes' list for metadata (evidence, ranking scores) but remove 'edges'. | |

**User's choice:** Remove Redundant Lists
**Notes:** This will significantly reduce token usage while maintaining the necessary context through the integrated view and the ID map.

---

## Placeholder Format

| Option | Description | Selected |
|--------|-------------|----------|
| Distinct Tag Types | `<node_01/>` for explicit omitted nodes and `<ommitted_logic_id01/>` for implicit deep logic. | |
| Single Unified Tag Type | Use a single unified tag for all omissions (e.g., `<ommitted_NN/>`). | ✓ |

**User's choice:** Single Unified Tag Type
**Notes:** Simplest approach for both generator and parser.

---

## Node Map Inclusion

| Option | Description | Selected |
|--------|-------------|----------|
| Include ID Map | Yes, include a `node_id_map` in the response to resolve short IDs to FQNs. | ✓ |
| Use FQNs in Tags Directly | No, use the FQN directly in the tag. | |

**User's choice:** Include ID Map
**Notes:** Required to maintain the functionality of downstream tools (`expand`, `trace_flow`) while keeping the code view clean.

---

## Claude's Discretion
None explicitly noted, though the exact implementation of the integrated source view construction is left to the developer.

## Deferred Ideas
None.
