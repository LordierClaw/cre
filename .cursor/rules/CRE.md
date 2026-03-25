<!-- gsd-project-start source:PROJECT.md -->
## Project

**CRE**

CRE (Context Reconstruction Engine) is a Java + Spring Boot system that reconstructs execution-relevant code context for a target symbol, then slices and progressively expands only what’s needed. It is designed to replace traditional RAG workflows by building a relevance-focused graph from source structure (deterministic -> heuristic -> AI fallback) and exposing that capability through an MCP server.

**Core Value:** Enable accurate code changes with drastically fewer tokens by progressively reconstructing the exact execution-relevant context and expanding on-demand only when the missing context is truly required.

### Constraints

- **Language/Framework**: Java, Spring Boot 3.x — aligns with your Spring-focused context reconstruction goals.
- **Build Tool**: Maven — required for the v1 build pipeline.
- **JDK**: Corretto 21 at `/home/hainn/.jdks/corretto-21.0.10` — used for compilation and runtime assumptions.
- **Maven Installation Path**: `~/.local/share/JetBrains/Toolbox/apps/intellij-idea/plugins/maven/lib` — used for local developer environment compatibility.
- **MCP Transport**: `stdio` — chosen for typical host-agent integration.
- **Input Format**: short symbol format `Controller.method` — used as the canonical v1 query format.
- **Output Format**: `get_context` returns JSON/structured metadata (primary), with sliced code included.
- **MVP Done Metrics**: evaluate balancedly across token reduction, accuracy (edit correctness), and developer usability (workflow friction/time-to-correct-change).
<!-- gsd-project-end -->

<!-- gsd-stack-start source:research/STACK.md -->
## Technology Stack

## Recommended Stack
### Core Technologies
| Technology | Version | Purpose | Why Recommended |
|------------|---------|----------|------------------|
| Spring Boot | 3.5.12 | Server framework + dependency management | Stable 3.x baseline with strong ecosystem alignment |
| JavaParser | 3.28.0 | AST parsing for Java code reconstruction | Mature AST APIs for extracting structured symbols |
| MCP Java SDK (`StdioServerTransport`) | 1.1.0 | Implement MCP server over stdio | Official SDK includes stdio transport support and MCP protocol plumbing |
| JDK (Corretto) | 21.0.10 | Build/runtime baseline | Matches your required toolchain; avoids bytecode/runtime mismatches |
### Supporting Libraries
| Library | Version | Purpose | When to Use |
|---------|---------|----------|--------------|
| SLF4J | 2.0.16 | Logging facade | Always useful for parse/index/slice tracing |
| Logback Classic | 1.5.6 | Logging backend | Spring Boot default backend |
| Jackson Databind | (managed by Spring Boot) | JSON serialization | Needed for structured MCP payloads/metadata |
| JUnit Jupiter | 5.11.4 | Unit tests | Validate slicing, node identity, placeholders, plugin hooks |
| Mockito | 5.14.0 | Mocking | Isolate reconstruction pipeline from filesystem/index backends |
| AssertJ | 3.26.0 | Fluent assertions | Improve test readability |
### Development Tools
| Tool | Purpose | Notes |
|------|---------|-------|
| Maven | Build orchestrator | Configure deterministic plugin versions in `pom.xml` |
| `maven-compiler-plugin` | Target JDK bytecode | Use `release=21` |
| `maven-surefire-plugin` | Unit test execution | Keep consistent with JUnit 5 |
## Installation
# Build
# Tests
## Alternatives Considered
| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|--------------------------|
| Spring Boot 3.5.12 | Quarkus/Micronaut | If you strongly prefer smaller footprint/fast startup |
| JavaParser | Eclipse JDT | If you need heavier type-binding resolution (with more classpath complexity) |
| MCP Java SDK | Custom stdio glue | Only if you must deviate from official protocol helpers |
## What NOT to Use
| Avoid | Why | Use Instead |
|-------|-----|--------------|
| Regex-based Java parsing | Brittle against nested types/generics/annotations | JavaParser AST |
| Full-project embeddings/RAG as retrieval | Reintroduces drift and non-reproducible context | Graph-based reconstruction + on-demand expand |
| Forcing runtime classloading for “resolution” | Can execute code / is unsafe | Static analysis + safe symbol solving |
## Stack Patterns by Variant
- Cache AST/index results keyed by `(project_root, lastModified/content_hash)`
- Reuse the plugin annotation results where possible
- Use syntax/graph evidence first
- Mark uncertain edges as “heuristic/unknown” and request `expand` instead of guessing silently
## Version Compatibility
| Package A | Compatible With | Notes |
|-----------|------------------|-------|
| Spring Boot 3.5.12 | Java 17+ | Project targets Java 21 |
| MCP Java SDK (mcp) 1.1.0 | Java 17+ | Verify behavior under JDK 21 |
| JavaParser 3.28.0 | Modern Java (incl. 21) | Still compile with `--release 21` |
## Sources
- Spring Boot 3.5.12 release (Spring blog / Spring project page)
- JavaParser 3.28.0 (GitHub/Maven Central)
- MCP Java SDK (Maven Central, stdio transport docs)
- Maven plugin documentation (configuration guidance)
<!-- gsd-stack-end -->

<!-- gsd-conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- gsd-conventions-end -->

<!-- gsd-architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- gsd-architecture-end -->

<!-- gsd-workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- gsd-workflow-end -->



<!-- gsd-profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- gsd-profile-end -->
