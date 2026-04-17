# CRE

## What This Is

CRE (Context Reconstruction Engine) is an AST-backed graph traversal engine designed for precise, token-efficient code context reconstruction.

## Core Value

Enable accurate code changes with drastically fewer tokens by progressively reconstructing the exact execution-relevant context and expanding on-demand only when the missing context is truly required.

## Shipped Versions

- **v1.0 (2026-03-29):** Core MCP server, AST graph, Spring/Exception plugins, Heuristic ranking/pruning, Raw Text/XML Migration, SSE/REST support.
- **v2.0 (2026-04-14):** Redesign & Efficiency. Spring DI, exploration tools, optimized XML output, symbol-based resolution, robust exception handling.
- **v3.0 (2026-04-15):** Symbol Resolution & Accuracy. Integrated JavaParser Symbol Solver, method overloading, generics, inheritance, wildcard imports, modern Java features (records, sealed classes).

## Requirements

### In Progress (v3.0)

- [ ] **OPT-01**: Intelligent output optimization and comment stripping for large projects.

### Validated (v3.0)

- [x] **SYMB-01**: Integrate `JavaParser` Symbol Solver with `CombinedTypeSolver`
- [x] **SYMB-02**: Support for method overloading resolution
- [x] **SYMB-03**: Support for inheritance resolution
- [x] **SYMB-04**: Support for generic type extraction and resolution
- [x] **SYMB-05**: Support for wildcard imports
- [x] **SYMB-06**: Refactor `JavaAstIndexer` to use Symbol Solver
- [x] **SYMB-07**: Handle polymorphism (dynamic dispatch)
- [x] **SYMB-08**: Support for modern Java features (records, sealed classes)

### Validated (v2.0)

- [x] **REF-02**: Core engine refactoring (Spring DI, centralized services, removal of ranking/pruning)
- [x] **CORE-02**: Enhanced exploration tools (`get_project_structure`, `get_file_structure`)
- [x] **CTX-02**: Optimized context output (XML-like tagging, depth-based expansion, granular options)
- [x] **BUG-02**: Robustness improvements (Standardized exceptions, commented code handling)

## Context

- Product goal: replace RAG with structure-aware context reconstruction for code editing/changes.
- Core approach: deterministic traversal with expansion on demand (v2.0 simplified this by removing heuristics).
- Graph model: classes/methods/fields with string-based identifiers (Symbols).
- Replace-on-demand: unknown/complex parts are represented initially (placeholders) and later replaced via deeper graph traversal and/or plugins.
- Expand-on-demand: the user/agent requests deeper traversal via higher `depth` in `get_context`.
- Output format: token-optimized XML-like tagging for structured code blocks.

## Constraints

- **Language/Framework**: Java 21, Spring Boot 3.5.x
- **Build Tool**: Maven
- **JDK**: Corretto 21
- **MCP Transport**: stdio, SSE
- **Output Format**: Token-optimized XML-like tagging

## Key Decisions

<details>
<summary><b>v1.0 Decisions</b></summary>

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Identity-based NodeId | FQN + signature + normalized path (no lines) ensures stability | ✓ Deprecated |
| Hierarchical XML Wrapping | Switched to XML tags for code blocks to improve agent readability | ✓ Good |
| Raw Text Migration | Abandoned structured JSON payloads in favor of raw text to save tokens | ✓ Good |
| ProjectManager Cache | Implemented 2-hour TTL cache to support multi-project 24/7 runtime | ✓ Good |
| Abandon Phase 0 & 6 | Focused development on high-value reconstruction and ingestion | ✓ Done |
</details>

<details>
<summary><b>v2.0 Decisions</b></summary>

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Remove Heuristic Ranking | Deterministic traversal + expansion on demand is more predictable for agents | ✓ Good |
| Tool Simplification | Consolidating into `get_context` and structure tools reduces complexity | ✓ Good |
| XML-like Class Tagging | Further improves token efficiency and structured readability | ✓ Good |
| Spring DI for ProjectManager | Improves maintainability and testability | ✓ Good |
| Symbol-based Resolution | Human-readable symbols are easier for agents to manage than complex IDs | ✓ Good |
| Selective Comment Pruning | Keeping only Javadoc saves significant tokens while preserving semantics | ✓ Good |
</details>

<details>
<summary><b>v3.0 Decisions</b></summary>

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| JavaParser Symbol Solver | Needed for accuracy in method overloading, generics, and inheritance | ✓ Good |
</details>

---

*Last updated: 2026-04-17 after Phase 25 addition.*
