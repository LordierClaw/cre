# Stack Research

**Domain:** CRE (Context Reconstruction Engine) — Java + Spring Boot MCP server (stdio) for code context reconstruction
**Researched:** 2026-03-25
**Confidence:** MEDIUM

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

```bash
# Build
mvn -DskipTests package

# Tests
mvn test
```

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

**If you need fast repeated reconstructions in one session:**
- Cache AST/index results keyed by `(project_root, lastModified/content_hash)`
- Reuse the plugin annotation results where possible

**If type resolution is incomplete (e.g., missing classpath):**
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

---
*Stack research for: CRE (Context Reconstruction Engine)*
*Researched: 2026-03-25*

