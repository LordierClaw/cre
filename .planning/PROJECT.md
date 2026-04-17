# CRE

## What This Is

CRE (Context Reconstruction Engine) is an AST-backed graph traversal engine designed for precise, token-efficient code context reconstruction.

## Core Value

Enable accurate code changes with drastically fewer tokens by progressively reconstructing the exact execution-relevant context and expanding on-demand only when the missing context is truly required.

## Shipped Versions

- **v1.0 (2026-03-29):** Core MCP server, AST graph, Spring/Exception plugins, Heuristic ranking/pruning, Raw Text/XML Migration, SSE/REST support.
- **v2.0 (2026-04-14):** Redesign & Efficiency. Spring DI, exploration tools, optimized XML output, symbol-based resolution, robust exception handling.
- **v3.0 (2026-04-17):** Symbol Resolution & Accuracy. Integrated JavaParser Symbol Solver, method overloading, generics, inheritance, wildcard imports, modern Java features (records, sealed classes), and output optimization.

## Requirements

### Validated (v3.0)

- ✓ **SYMB-01**: Integrate `JavaParser` Symbol Solver with `CombinedTypeSolver` — v3.0
- ✓ **SYMB-02**: Support for method overloading resolution — v3.0
- ✓ **SYMB-03**: Support for inheritance resolution — v3.0
- ✓ **SYMB-04**: Support for generic type extraction and resolution — v3.0
- ✓ **SYMB-05**: Support for wildcard imports — v3.0
- ✓ **SYMB-06**: Refactor `JavaAstIndexer` to use Symbol Solver — v3.0
- ✓ **SYMB-07**: Handle polymorphism (dynamic dispatch) — v3.0
- ✓ **SYMB-08**: Support for modern Java features (records, sealed classes) — v3.0
- ✓ **OPT-01**: Intelligent output optimization and comment stripping — v3.0
- ✓ **GAP-01**: Resolve signature ID inconsistencies and add Record support to optimization logic — v3.0

### Validated (v2.0)

- ✓ **REF-02**: Core engine refactoring (Spring DI) — v2.0
- ✓ **CORE-02**: Enhanced exploration tools — v2.0
- ✓ **CTX-02**: Optimized context output — v2.0
- ✓ **BUG-02**: Robustness improvements — v2.0

## Context

- Product goal: replace RAG with structure-aware context reconstruction for code editing/changes.
- Core approach: deterministic traversal with expansion on demand.
- Graph model: classes/methods/fields with string-based identifiers (Symbols).
- Output format: token-optimized XML-like tagging for structured code blocks.
- Current state: v3.0 shipped with high precision symbol resolution and optimized output.

## Constraints

- **Language/Framework**: Java 21, Spring Boot 3.5.x
- **Build Tool**: Maven
- **JDK**: Corretto 21
- **MCP Transport**: stdio, SSE
- **Output Format**: Token-optimized XML-like tagging

## Key Decisions

<details>
<summary><b>v3.0 Decisions</b></summary>

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| JavaParser Symbol Solver | Needed for accuracy in method overloading, generics, and inheritance | ✓ Good |
| [Phase 25] Dynamic Capping | Scales traversal based on depth to prevent token explosion | ✓ Good |
| [Phase 25] Surgical Pruning | Preserves comments only for gathered nodes | ✓ Good |
| [Phase 26] Unified Signatures | Centralized ID generation in AstUtils ensures Indexer-Service sync | ✓ Good |
</details>

<details>
<summary><b>v2.0 Decisions</b></summary>

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Remove Heuristic Ranking | Deterministic traversal + expansion on demand is more predictable | ✓ Good |
| Tool Simplification | Consolidating into `get_context` reduces complexity | ✓ Good |
| XML-like Class Tagging | Improves token efficiency and structured readability | ✓ Good |
</details>

---

*Last updated: 2026-04-17 after v3.0 milestone completion.*
