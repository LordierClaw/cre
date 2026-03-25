# Architecture Research

**Domain:** Java + Spring Boot Context Reconstruction Engine (CRE)
**Researched:** 2026-03-25
**Confidence:** MEDIUM

## Standard Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                  Context Reconstruction Engine (CRE)        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Query → Context Builder → Graph Engine → Plugin Layer     │
│                              ↓             ↓                │
│                           Slicing Engine → Formatter → Expand API
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**End-to-end pipeline:** `Query -> Context Builder -> Graph Engine -> Plugin Layer -> Slicing Engine -> Formatter -> Expand API`

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|------------------|------------------------|
| Query Intake | Normalize user input (`Controller.method`) into an entry-point request | Parse/normalize string; resolve overload ambiguity deterministically |
| Context Builder | Resolve seed facts and entry nodes (controller mapping, service wiring hints) | Symbol store lookup + lightweight AST scans |
| Graph Engine | Build/maintain the graph from Java AST | JavaParser AST walk; nodes: Class/Method/Field; edges: CALLS/USES_FIELD/BELONGS_TO |
| Plugin Layer | Inject framework semantics into graph edges/roles | Spring plugin maps annotations (`@Controller`, `@Service`, `@Autowired`) into graph semantics |
| Slicing Engine | Produce minimal relevant subgraph for the query and goal | Depth/range-limited traversal; placeholders for omissions |
| Formatter | Serialize slice output into structured context | Stable node ordering + metadata/provenance fields |
| Expand API | Progressive discovery: `expand(node_id)` reveals omitted logic | Targeted expansion: reload/extend only affected regions; replace placeholders |
| MCP Tool Adapter | Expose internal operations as MCP tools | Tool router mapping MCP calls to internal services |

## Recommended Project Structure

```
src/
├── main/
│   ├── java/com/cre/
│   │   ├── Application.java
│   │   ├── core/
│   │   │   ├── query/
│   │   │   │   └── QueryNormalizer.java
│   │   │   ├── context/
│   │   │   │   ├── ContextBuilder.java
│   │   │   │   └── SeedResolver.java
│   │   │   ├── graph/
│   │   │   │   ├── GraphEngine.java
│   │   │   │   ├── nodes/
│   │   │   │   │   └── (ClassNode/MethodNode/FieldNode)
│   │   │   │   └── edges/
│   │   │   │       └── (CALLS/USES_FIELD/BELONGS_TO)
│   │   │   ├── plugins/
│   │   │   │   ├── Plugin.java
│   │   │   │   ├── PluginRegistry.java
│   │   │   │   └── spring/SpringPlugin.java
│   │   │   └── slice/
│   │   │       ├── SlicingEngine.java
│   │   │       └── Placeholder.java
│   │   ├── format/
│   │   │   └── Formatter.java
│   │   └── mcp/
│   │       └── McpToolRouter.java
│   └── resources/
│       └── application.yml
└── test/
    └── java/ (unit/integration tests for slicing + plugin mapping)
```

### Structure Rationale

- `core/` isolates deterministic reconstruction logic (graph + slicing + confidence/provenance) from transport/hosting.
- `plugins/` keeps Spring semantics extensible while preventing framework knowledge from contaminating the generic graph engine.

## Architectural Patterns

### Pattern 1: Deterministic → Heuristic → AI Fallback (Chain of Responsibility)

**What:** Resolution proceeds through a chain: deterministic rules first, heuristics second, AI fallback only when gated gaps remain.  
**When to use:** Overload/ambiguity or missing symbol/classpath data is expected early in v1.  
**Trade-offs:** More engineering upfront; much higher reproducibility and debugability.

### Pattern 2: Plugin Registry (Open/Closed Framework Semantics)

**What:** Plugins register stable semantic enrichers that annotate roles/edges in the graph.  
**When to use:** Supporting Spring first, with future event/BPM/domain plugins.  
**Trade-offs:** Ordering/versioning must be stable to avoid non-determinism.

### Pattern 3: Expand-on-demand with Lazy Graph Expansion

**What:** Emit minimal slices first; widen scope via explicit `expand(node_id)` calls.  
**When to use:** Token budgets and developer workflow demand iterative refinement.  
**Trade-offs:** Requires stable node identity and placeholder contracts to prevent drift.

## Data Flow

### Request Flow

```
[MCP Tool Call]
    ↓
[MCP Tool Router]
    ↓
[Context Builder] → [Graph Engine] → [Plugin Layer]
    ↓                                   ↓
[Slicing Engine]  ← evidence/provenance→
    ↓
[Formatter] → [Sliced JSON Context]
    ↓
[Response]
```

### State Management

```
┌────────────────┐
│ Index Store     │ (AST caches, parsed symbols)
└──────┬─────────┘
       ↓
┌────────────────┐
│ Graph Store     │ (nodes/edges + placeholders)
└──────┬─────────┘
       ↓
┌────────────────┐
│ Slice Plans     │ (reproducible slice boundaries)
└────────────────┘
```

### Key Data Flows

1. **Symbol→Graph entry:** parse `Controller.method` and resolve to a `MethodNode`.
2. **Spring semantic enrichment:** annotations become edges (e.g., controller entry points, service dependencies).
3. **Slice assembly:** deterministic traversal chooses relevant nodes; placeholders represent omissions with “next expand” intent.
4. **Expansion:** `expand(node_id)` extends/repairs the slice region and replaces placeholders.

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 0-1k dev queries | Monolith service with persistent AST/index caching and deterministic formatting |
| 1k-100k queries | Add concurrency limits, warmed indexes per repo, and slice result caching keyed by identity+budget |
| 100k+ queries | Split index/graph persistence and introduce job-based expansion with deterministic snapshot hashes |

### Scaling Priorities

1. First bottleneck: repeated AST/index rebuilds → fix with caching + incremental invalidation.
2. Second bottleneck: placeholder replacement drift → fix with stable node identity schema + evidence reconciliation.

## Anti-Patterns

### Anti-Pattern 1: Over-reliance on AI for core graph correctness

**What people do:** infer edges/nodes from text instead of JavaParser + deterministic graph evidence.  
**Why it's wrong:** output becomes non-reproducible and expansions contradict earlier slices.  
**Do this instead:** deterministic/heuristic chain, AI only for gated gap-filling with explicit provenance.

### Anti-Pattern 2: Building the full graph on every query

**What people do:** parse all files and build full connectivity every request.  
**Why it's wrong:** latency/token waste and “minimal slice” promise breaks.  
**Do this instead:** seed from entry points and slice first; expand lazily.

### Anti-Pattern 3: Placeholder drift

**What people do:** placeholders are emitted but cannot be reliably replaced.  
**Why it's wrong:** context never converges; users keep expanding without progress.  
**Do this instead:** store placeholder metadata (node identity, omission rule, index snapshot version/hash).

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| MCP host client | Tool router (stdio MCP server) | Keep tool I/O stable and versioned |
| Maven build artifacts (optional) | Build-time classpath info | Prefer static analysis; never execute untrusted code |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| `Graph Engine ↔ Plugin Layer` | Graph annotations + overlays | Plugins add/annotate edges; core parsing stays untouched |
| `Slicing Engine ↔ Graph Store` | Read-only fetch of nodes/edges + slice plan persistence | Expansion re-slices only affected regions |
| `Formatter ↔ Slice Result` | Structured serialization | Deterministic ordering for stable evaluation |

## Sources

- `.docs/ARCHITECURE_SOLUTION.md` (CRE pipeline + principles)
- MCP stdio server design docs (Java MCP SDK)
- JavaParser approach (AST-based parsing)
- Spring Boot semantics (controller/service wiring conventions)

---
*Architecture research for: CRE (Context Reconstruction Engine)*
*Researched: 2026-03-25*

