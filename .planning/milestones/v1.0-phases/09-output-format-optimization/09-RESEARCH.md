# Phase 09: Output Format Optimization - Research

**Researched:** 2026-03-28
**Domain:** Code Slicing, JSON Response Formatting, AST Manipulation
**Confidence:** HIGH

## Summary

The goal of Phase 09 is to transform the `get_context` tool's output from a relational, multi-list JSON structure into a more compact, integrated, and readable file-centric view. This approach reduces token usage by eliminating redundant metadata lists and replacing omitted code with XML-style placeholders.

**Primary recommendation:** Implement an `IntegratedViewBuilder` that uses JavaParser to reconstruct class structures per file, replacing pruned nodes and method calls to pruned nodes with `<ommitted_NN/>` tags, and providing a `node_id_map` for resolution.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** The output will group code by file, reconstructing the class structure (package, imports, class/field declarations) for included nodes.
- **D-02:** Multiple methods from the same file will be shown within their shared class context.
- **D-05:** Remove the redundant `nodes` and `edges` lists from the response; the integrated code and a `node_id_map` are sufficient for both humans and agents.
- **D-03:** Use a single unified tag type `<ommitted_NN/>` for all omissions.
- **D-04:** Replace omitted methods, fields, and individual method call expressions (when the callee is omitted) with these XML-style placeholders.
- **D-06:** Provide a `node_id_map` in the response to resolve short IDs (like `ommitted_01`) back to their full NodeId (FQN/signature) for subsequent `expand` or `trace_flow` calls.
- **D-07:** The entire response must remain valid JSON. The structure should be easy for a human to scan and for an LLM to parse and reason about.
- **D-08:** Omit full paths of classes in the code snippets; use simple names where possible, relying on the package declaration and imports for context.

### the agent's Discretion
- Implementation of the `IntegratedViewBuilder` and how it handles multiple types in a single file or inner classes.
- Exact structure of the `integrated_files` JSON array (suggested structure below).
- How to manage the sequential numbering and stability of `ommitted_NN` IDs within a single request.

### Deferred Ideas (OUT OF SCOPE)
- None.

</user_constraints>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JavaParser | 3.28.0 | AST parsing and manipulation | Used throughout the project for indexing; provides robust AST reconstruction. |
| Jackson | (from Spring Boot) | JSON Serialization | Standard for Spring Boot; used for current tool responses. |

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/cre/
├── tools/
│   ├── GetContextTool.java       # Main tool entry point
│   ├── IntegratedViewBuilder.java # NEW: Builds the integrated file-centric view
│   └── model/
│       ├── GetContextResponse.java # UPDATED: New integrated schema
│       └── IntegratedFile.java    # NEW: Data model for integrated file view
```

### Pattern 1: Integrated View Builder
**What:** A specialized class that takes the set of retained and pruned `NodeId`s and reconstructs the source files.
**When to use:** During `get_context` and `expand` execution to format the final response.
**Example:**
```java
public class IntegratedViewBuilder {
    private final Map<String, String> nodeIdMap = new LinkedHashMap<>();
    private int counter = 1;

    public List<IntegratedFile> build(Set<NodeId> retained, Set<NodeId> pruned, GraphEngine graph) {
        // 1. Group nodes by sourceOrigin
        // 2. For each file, parse CompilationUnit
        // 3. Walk AST and replace members/calls not in 'retained' with placeholders
        // 4. Record mappings in nodeIdMap
    }
}
```

### Pattern 2: Call Expression Replacement
**What:** Replacing `MethodCallExpr` in the AST with a placeholder if the callee is not retained.
**Why:** Directly addresses D-04 and significantly reduces token counts for deep logic.
**Implementation:** Re-use `JavaAstIndexer` resolution logic to identify callees, then replace the `MethodCallExpr` with a special `NameExpr` or string that is later transformed into `<ommitted_NN/>`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| AST Reconstruction | Manual string concatenation for classes | `JavaParser` `node.toString()` | Handles indentation, syntax, and nested structures correctly. |
| File Path Handling | Manual string split for FQNs | `NodeId` and `Path` API | Ensures cross-platform compatibility and correct origin mapping. |

## Common Pitfalls

### Pitfall 1: Repetitive File Parsing
**What goes wrong:** `get_context` becomes slow as it re-parses every file involved in a slice.
**How to avoid:** Implement a request-scoped or short-lived LRU cache for `CompilationUnit` objects.

### Pitfall 2: Syntactic Validity vs. Readability
**What goes wrong:** Replacing calls with XML tags might break some LLM parsers if not formatted clearly.
**How to avoid:** Ensure the resulting "integrated code" remains readable. Use a placeholder like `_OMITTED_01_` during AST manipulation and perform a final search-and-replace to XML tags to avoid JavaParser's identifier restrictions.

### Pitfall 3: Inner Classes and Multiple Types
**What goes wrong:** `NodeId` maps to a member of an inner class, but the builder only processes top-level types.
**How to avoid:** Ensure the AST visitor/walker recurses into all `TypeDeclaration`s within a `CompilationUnit`.

## Code Examples

### Integrated File View (Proposed JSON)
```json
{
  "slice_version": "cre.slice.v2",
  "metadata": { "evidence": { ... }, "ranking": { ... } },
  "integrated_files": [
    {
      "origin": "src/main/java/com/cre/tools/GetContextTool.java",
      "package": "com.cre.tools",
      "imports": [ "import com.cre.core.graph.NodeId;", "..." ],
      "code": "public class GetContextTool {\n    <ommitted_01/>\n\n    public GetContextResponse execute(String nodeIdRaw, int depth) {\n        return <ommitted_02/>;\n    }\n}"
    }
  ],
  "node_id_map": {
    "ommitted_01": "com.cre.tools.GetContextTool::SLICE_VERSION::src/main/java/com/cre/tools/GetContextTool.java",
    "ommitted_02": "com.cre.tools.GetContextTool::buildSlice(String,int,int,boolean)::src/main/java/com/cre/tools/GetContextTool.java"
  }
}
```

### JavaParser Node Replacement
```java
// Simplified replacement logic
methodBody.findAll(MethodCallExpr.class).forEach(call -> {
    NodeId calleeId = resolve(call); // Re-use indexer logic
    if (!retained.contains(calleeId)) {
        String id = getOrGenerateOmittedId(calleeId);
        call.replace(new NameExpr("CRE_OMITTED_" + id));
    }
});
```

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 21 | Runtime | ✓ | 21.x | — |
| Maven | Build | ✓ | 3.x | — |

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + AssertJ |
| Config file | pom.xml |
| Quick run command | `mvn test -Dtest=GetContextSchemaTest` |
| Full suite command | `mvn test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PH9-01 | integrated_files structure | unit | `mvn test -Dtest=GetContextSchemaTest` | ❌ Update existing |
| PH9-02 | node_id_map resolution | unit | `mvn test -Dtest=GetContextSchemaTest` | ❌ Update existing |
| PH9-03 | call replacement | integration | `mvn test -Dtest=RealProjectE2ETest` | ❌ Update existing |

### Wave 0 Gaps
- [ ] `src/test/java/com/cre/tools/GetContextSchemaTest.java` — Needs update for new JSON structure.
- [ ] `src/test/java/com/cre/e2e/RealProjectE2ETest.java` — Needs update to verify integrated code content.

## Sources

### Primary (HIGH confidence)
- `src/main/java/com/cre/tools/GetContextTool.java` - Current implementation analysis.
- `src/main/java/com/cre/core/ast/JavaAstIndexer.java` - AST resolution patterns.
- `09-CONTEXT.md` - User implementation decisions.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - JavaParser and Jackson are already integrated.
- Architecture: HIGH - File-centric reconstruction is a standard pattern for code intelligence.
- Pitfalls: MEDIUM - Syntactic replacement in AST requires careful testing but is well-supported by JavaParser.

**Research date:** 2026-03-28
**Valid until:** 2026-04-28
