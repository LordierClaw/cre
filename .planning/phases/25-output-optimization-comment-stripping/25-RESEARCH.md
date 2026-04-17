# Phase 25: Output Optimization & Comment Stripping - Research

**Researched:** 2025-04-17
**Domain:** Java AST Transformation, Token Optimization, Regex Normalization
**Confidence:** HIGH

## Summary

This research focuses on optimizing the token footprint and readability of the context output in the CRE system. The primary goal is to reduce noise from comments in "skeleton" code while preserving valuable internal comments for "gathered" (full-body) nodes. Additionally, we address formatting issues like consecutive empty lines and broken line breaks caused by AST node removals through regex-based post-processing.

**Primary recommendation:** Implement a context-aware `pruneComments` method that uses Node-to-ID mapping to selectively keep comments, and use `DefaultContextPostProcessor` with multi-line regex patterns to normalize the final output.

## User Constraints

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01: Preservation Scope:** Keep Javadocs and all internal comments (`//`, `/* */`) ONLY for "Gathered" nodes (any node shown with its full body).
- **D-02: Class-Level Javadocs:** Strip class-level Javadocs unless the class itself is a "target" (the primary symbol requested).
- **D-03: Skeleton Pruning:** For non-target/non-gathered nodes (skeletons), strip all internal comments and Javadocs.
- **D-04: Line Collapse:** Collapse multiple consecutive empty lines into a single empty line.
- **D-05: Marker Placement:** Place `<omitted_.../>` markers on their own new line, but without surrounding empty lines to maintain density.
- **D-06: Aggressive Trim:** Apply aggressive whitespace trimming to file blocks, but **exclude gathered functions** to preserve their original indentation and style for the agent.
- **D-07: Line Length:** Do not enforce a maximum line length; keep source lines as they appear.
- **D-08: No Hard Token Limit:** Do not truncate output based on token count; prioritize complete context reconstruction.
- **D-09: Dynamic Node Capping:** Scale the `MAX_GATHER_NODES` limit dynamically based on the requested `depth` to prevent explosion in large projects.
- **D-10: Traversal Priority:** Use Breadth-First Priority to ensure immediate neighbors (depth 1) are always included before deeper branches if a cap is reached.
- **D-11: Preservation over Pretty:** Continue using `LexicalPreservingPrinter` to ensure code accuracy.
- **D-12: Regex Normalization:** Use regex-based post-processing to fix "inline" line break issues and other formatting "bad smells" resulting from node removals.
- **D-13: Hybrid Approach:** Explore using Lexical printing for target/gathered functions while using a cleaner/more efficient method for skeletons if regex cleanup proves insufficient.
- **D-14: Marker Style:** Maintain the current XML-like Tag-style (`<omitted_functions/>`) for clear agent parsing.

### the agent's Discretion
- Specific regex patterns for normalization.
- Formula for dynamic node capping.
- Exact implementation of `pruneComments` logic.

### Deferred Ideas (OUT OF SCOPE)
- **Switch to PrettyPrinter:** Deferred the move to a standard pretty printer to maintain original source fidelity via LexicalPreservingPrinter.
</user_constraints>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JavaParser | 3.25.10 | AST Manipulation | Core of CRE's Java analysis. [VERIFIED: pom.xml] |
| LexicalPreservingPrinter | 3.25.10 | Source preservation | Maintains original formatting for gathered nodes. [VERIFIED: codebase] |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|--------------|
| java.util.regex | (JRE) | Text Normalization | Post-processing the integrated view. [VERIFIED: JRE] |

## Architecture Patterns

### Pattern: Context-Aware Pruning
Instead of a global pruning pass, the system will now pass the "Relevance Context" (list of gathered node IDs and the target FQN) into the pruning logic.

### Pattern: Multi-Stage Post-Processing
1. **AST Pruning:** Remove bodies, comments, and members using JavaParser.
2. **Lexical Printing:** Generate a string that preserves original whitespace as much as possible.
3. **Regex Normalization:** Fix formatting "bad smells" (empty lines, inline markers) that LexicalPreservingPrinter creates when nodes are removed.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Java Code Formatting | Custom Formatter | Regex + Lexical Printer | Full formatting is too complex; regex handles "cleanup" sufficiently. |
| Graph Traversal | Custom Dijkstra | BFS with Distance Map | Standard BFS naturally handles Breadth-First Priority. |

## Common Pitfalls

### Pitfall 1: Orphan Comment Retention
**What goes wrong:** Comments not associated with any node (orphans) are often missed by `node.getComment()`.
**How to avoid:** Use `cu.getComments()` or `cu.getAllContainedComments()` and check their association via `comment.getCommentedNode()`.

### Pitfall 2: Regex Over-Aggression
**What goes wrong:** `replaceAll("\\s+", " ")` would destroy indentation in gathered functions.
**How to avoid:** Use line-anchored regex (`(?m)^...`) and target specific "bad patterns" like consecutive empty lines or inline markers.

### Pitfall 3: Capping mid-level in BFS
**What goes wrong:** Standard BFS adds all neighbors to the queue. If the limit is 100 and level 2 has 200 nodes, we might take 50 from one branch and 50 from another, losing breadth.
**How to avoid:** Process level-by-level (standard queue-size-loop) or ensure the sort order of neighbors is consistent.

## Code Examples

### Surgical Pruning Implementation
```java
// Logic to be implemented in CreServiceImpl
private void pruneComments(CompilationUnit cu, Set<String> gatheredIds, String targetFqn) {
    List<Comment> allComments = cu.getComments();
    for (Comment comment : allComments) {
        boolean keep = false;
        Optional<Node> associated = comment.getCommentedNode();
        
        if (associated.isPresent()) {
            Node node = associated.get();
            String nodeId = calculateNodeId(node); // Helper to get FQN::Signature
            
            if (gatheredIds.contains(nodeId)) {
                // Keep all comments for gathered nodes
                keep = true;
            } else if (node instanceof ClassOrInterfaceDeclaration cid) {
                // Keep Javadoc for target class
                String fqn = cid.getFullyQualifiedName().orElse("");
                if (fqn.equals(targetFqn) && comment instanceof JavadocComment) {
                    keep = true;
                }
            }
        }
        
        if (!keep) {
            comment.remove();
        }
    }
}
```

### Regex Normalization (DefaultContextPostProcessor)
```java
public String process(String context) {
    // 1. Collapse 3+ newlines into 2 (one empty line)
    context = context.replaceAll("(\\r?\\n\\s*){3,}", "\n\n");
    
    // 2. Fix inline markers (ensure markers are on their own line)
    context = context.replaceAll("([^\\n])\\s*(<omitted_)", "$1\n$2");
    context = context.replaceAll("(<omitted_functions/>)\\s*([^\\n])", "$1\n$2");
    
    // 3. Trim whitespace around file blocks
    context = context.replaceAll("(?s)<file[^>]*>\\s+", m -> m.group(0).trim() + "\n");
    
    return context.trim();
}
```

## Dynamic Node Capping Formula

| Depth | Cap (MAX_GATHER_NODES) | Reasoning |
|-------|------------------------|-----------|
| 1 | 150 | Shallow exploration allows more breadth for immediate context. |
| 2-3 | 100 | Balanced exploration. |
| 4+ | 50 | Deep exploration must be tightly restricted to prevent token explosion. |

**Formula Recommendation:**
`int cap = (depth <= 1) ? 150 : (depth <= 3) ? 100 : 50;`

## Open Questions

1. **How to efficiently calculate Node ID during pruning?**
   - Recommendation: Use a visitor or a pre-pass to map Nodes to IDs, or use a helper that matches the logic in `transformWithRelevance`.
2. **Should we keep license headers?**
   - Recommendation: No, these are usually unneeded for LLM context and should be pruned (they are usually orphan comments at the start of the file).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 21 | Runtime | ✓ | 21.0.10 | — |
| Maven | Build | ✓ | 3.9.6 | — |

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V5 Input Validation | yes | Regex patterns are static and not user-controlled. |

## Sources

### Primary (HIGH confidence)
- `src/main/java/com/cre/core/service/CreServiceImpl.java` - Existing pruning logic.
- `com.github.javaparser:javaparser-core` - Official Javadocs for Comment/Node association.

### Secondary (MEDIUM confidence)
- Web search for "regex collapse multiple empty lines into one java".

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Directly from pom.xml and codebase.
- Architecture: HIGH - Built on top of existing transform patterns.
- Pitfalls: HIGH - Based on known LexicalPreservingPrinter behavior.

**Research date:** 2025-04-17
**Valid until:** 2025-05-17
