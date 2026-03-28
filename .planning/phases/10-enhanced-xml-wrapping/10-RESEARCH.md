# Phase 10: Enhanced XML Context Wrapping - Research

**Researched:** 2026-03-28
**Domain:** Code Transformation and XML Packaging
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Wrap the entire code block of each file in a tag named `ClassName.MemberName` (e.g., `<LoanService.borrowBook>`).
- **D-02:** Wrap the class definition itself (annotations + body) in a tag named after the `ClassName` (e.g., `<LoanService>`).
- **D-03:** The package declaration and imports stay inside the outer anchor tag but outside the class tag.
- **D-04:** **Imports:** If any imports are pruned, use a single `<ommitted_import/>` tag at the end of the import block.
- **D-05:** **Properties:** If any fields are pruned, use a single `<ommitted_properties/>` tag where the first pruned field was located.
- **D-06:** **Functions:** If any methods are pruned (that are not individually tagged as call-site omissions), use a single `<ommitted_functions/>` tag at the end of the class body.
- **D-07:** For pruned method call expressions *within* retained code, use `<ommitted_code id="ommitted_NN" description=""/>`.
- **D-08:** The `description` attribute must be present but left empty (`""`).
- **D-09:** IDs must be short sequential strings (e.g., `ommitted_01`) mapped in the existing `node_id_map`.
- **D-10:** Tags must be on their own lines and follow the indentation of the code they replace.

### the agent's Discretion
- Implementation of import pruning logic (not specified in graph indexing).
- Heuristics for deriving `ClassName.MemberName` for non-anchor files.
- Integration of `LexicalPreservingPrinter` for indentation preservation.

### Deferred Ideas (OUT OF SCOPE)
- Populating the `description` attribute in `<ommitted_code/>` (deferred to future AI/DocBlock analysis).
- Dynamic pruning of nested classes (out of scope for Phase 10).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| REQ-10.1 | Multi-layer XML wrapping | D-01, D-02 provide the structural requirements. `IntegratedViewBuilder` will be extended to handle this. |
| REQ-10.2 | Grouped Omissions | D-04, D-05, D-06 require new logic in `IntegratedViewBuilder` to track and emit category tags instead of individual node markers. |
| REQ-10.3 | Omitted Code format | D-07, D-08, D-09 specify the new regex and mapping logic for call-site omissions. |
| REQ-10.4 | Indentation Preservation | D-10 requires using JavaParser's `LexicalPreservingPrinter`. |
</phase_requirements>

## Summary

This phase focuses on refining the output of the `get_context` and `expand` tools. The current output uses simple individual tags (e.g., `<ommitted_01/>`) which can lead to verbose XML if many fields or methods are pruned. Phase 10 introduces semantic grouping (e.g., `<ommitted_properties/>`) and hierarchical wrapping to improve readability for LLMs.

**Primary recommendation:** Extend `IntegratedViewBuilder` to use `LexicalPreservingPrinter` and implement category-based grouping of pruned members. Use a multi-pass regex approach to transform Java-valid markers (inserted during AST transformation) into the final semantic XML tags.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JavaParser | 3.28.0 | AST transformation | Project standard for Java code manipulation. |
| LexicalPreservingPrinter | 3.28.0 | Indentation preservation | Built-in JavaParser tool that maintains original source formatting for unmodified parts. |

### Version verification:
```bash
mvn dependency:list | grep javaparser
# Verified version: 3.28.0
```

## Architecture Patterns

### Pattern 1: Multi-Pass Marker Transformation
Instead of trying to emit XML directly from JavaParser (which is difficult while keeping valid Java AST), continue the pattern of replacing nodes with Java-valid markers (dummy `FieldDeclaration`s) and then using regex to transform those markers into XML.

**Example Marker Mapping:**
- `int CRE_OM_IMPORT;` -> `<ommitted_import/>`
- `int CRE_OM_PROPS;` -> `<ommitted_properties/>`
- `int CRE_OM_FUNCS;` -> `<ommitted_functions/>`
- `CRE_OM_CODE_ommitted_01` -> `<ommitted_code id="ommitted_01" description=""/>`

### Pattern 2: Post-Processing Hierarchical Wrapping
The `IntegratedViewBuilder.build()` method will process the final string produced by the printer to insert hierarchical tags (`<ClassName.MemberName>` and `<ClassName>`). This is safer than manipulating the AST for these specific wraps as they cross standard Java structure boundaries (e.g., wrapping package/imports).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Indentation | Custom space counters | `LexicalPreservingPrinter` | Handles complex formatting rules (tabs vs spaces, multi-line declarations) automatically. |
| XML Escaping | Manual `replace("&", "&amp;")` | Standard XML tools (if needed) | Though for code, we are mostly wrapping, so escaping the code itself might be counter-productive for LLMs. |

## Common Pitfalls

### Pitfall 1: Breaking LexicalPreservingPrinter
**What goes wrong:** Lexical preservation fails if the AST is changed in a way that doesn't map clearly to the original source.
**How to avoid:** Call `LexicalPreservingPrinter.setup(cu)` immediately after parsing and use `node.replace()` or `node.remove()` rather than reconstructing large parts of the tree.

### Pitfall 2: Marker Collision
**What goes wrong:** `CRE_OM_CODE_ommitted_01` regex matching `ommitted_01` partially.
**How to avoid:** Use specific prefixes and anchor regex patterns (e.g., `\bCRE_OM_CODE_(ommitted_\d+)\b`).

## Code Examples

### Lexical Preserving Print Pattern
```java
// Source: https://javaparser.org/lexical-preservation-with-javaparser/
CompilationUnit cu = StaticJavaParser.parse(source);
LexicalPreservingPrinter.setup(cu);
// ... modifications ...
String result = LexicalPreservingPrinter.print(cu);
```

### Hierarchical Tag Insertion (Conceptual)
```java
// Logic for D-01 and D-02
String code = LexicalPreservingPrinter.print(cu);
String anchorTag = getAnchorTagName(anchor);
code = "<" + anchorTag + ">\n" + code + "\n</" + anchorTag + ">";
```

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java | Runtime | ✓ | 21 | — |
| Maven | Build | ✓ | 3.8.7 | — |
| JavaParser | AST transformation | ✓ | 3.28.0 | — |

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + AssertJ |
| Config file | `pom.xml` |
| Quick run command | `mvn test -Dtest=IntegratedViewBuilderTest` |
| Full suite command | `mvn test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command |
|--------|----------|-----------|-------------------|
| REQ-10.1 | Hierarchical XML tags (D-01, D-02) | Unit | `mvn test -Dtest=IntegratedViewBuilderTest` |
| REQ-10.2 | Grouped omission tags (D-04, D-05, D-06) | Unit | `mvn test -Dtest=IntegratedViewBuilderTest` |
| REQ-10.3 | New omitted_code format (D-07, D-08) | Unit | `mvn test -Dtest=IntegratedViewBuilderTest` |
| REQ-10.4 | Preserved indentation | Unit | `mvn test -Dtest=IntegratedViewBuilderTest` |

### Wave 0 Gaps
- None — existing test infrastructure covers all phase requirements.

## Sources

### Primary (HIGH confidence)
- `src/main/java/com/cre/tools/IntegratedViewBuilder.java` - Existing transformation logic.
- `src/main/java/com/cre/core/ast/JavaAstIndexer.java` - Used for signature resolution.
- JavaParser Official Documentation - `LexicalPreservingPrinter` usage.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - JavaParser is well-integrated.
- Architecture: HIGH - Marker + Regex is a proven pattern in this repo.
- Pitfalls: MEDIUM - Indentation with grouping tags needs careful testing.

**Research date:** 2026-03-28
**Valid until:** 2026-04-27
