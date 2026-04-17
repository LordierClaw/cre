---
phase: 25
plan: 01
subsystem: Core Service
tags: [optimization, comment-stripping, traversal-capping, token-reduction]
requires: [OPT-01]
provides: [dynamic-traversal-capping, surgical-comment-pruning]
affects: [CreServiceImpl]
tech-stack: [Java, JavaParser]
key-files: [src/main/java/com/cre/core/service/CreServiceImpl.java]
decisions:
  - Dynamic node capping based on depth (D-09)
  - Surgical comment pruning based on gathered vs skeleton nodes (D-01, D-02, D-03)
metrics:
  duration: 40m
  completed_date: "2026-04-17"
---

# Phase 25 Plan 01: Output Optimization - Comment Stripping Summary

## One-liner
Implemented depth-aware traversal capping and surgical comment pruning in `CreServiceImpl` to optimize token usage and context relevance.

## Key Changes

### Dynamic Traversal Capping
- Refactored `gatherNodesOrdered` to use a dynamic node limit (`cap`) based on the requested `depth`.
- Formula: `depth <= 1 -> 150`, `depth <= 3 -> 100`, `else -> 50`.
- This ensures high-breadth exploration for shallow queries while preventing token explosion for deep queries in large projects.

### Surgical Comment Pruning
- Implemented a sophisticated `pruneComments` logic that differentiates between "Gathered" nodes (full body) and "Skeleton" nodes.
- **Rules applied:**
  - **Gathered Nodes:** ALL comments (Javadoc and internal) are preserved to maintain full context.
  - **Target Class:** Class-level Javadoc is preserved for the primary symbol requested.
  - **Skeleton Nodes:** All comments and Javadocs are stripped to save tokens.
- Added `calculateNodeId` helper to match nodes against the `gathered` set using the canonical `fqn::signature` format.

### Service Flow Integration
- Updated `getContext` and `buildIntegratedView` to pass the `startNodeId` throughout the view construction process.
- Ensured `LexicalPreservingPrinter.setup(cu)` is called *before* pruning to maintain formatting fidelity.
- Updated `getFileStructure` to use the same surgical pruning logic (defaulting to stripping all comments except target class Javadoc).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Resolved compilation errors in `pruneComments`**
- **Found during:** Verification (mvn compile)
- **Issue:** Ambiguity in JavaParser generic types (`TypeDeclaration`, `BodyDeclaration`) when used in lambdas, leading to `Object` inference.
- **Fix:** Switched to explicit `for` loops and typed lambdas.
- **Files modified:** `src/main/java/com/cre/core/service/CreServiceImpl.java`
- **Commit:** `73f38ed`

## Verification Results

### Automated Tests
- `mvn clean compile`: **PASSED**
- `CreServiceImplTest`: **PASSED** (2 tests)
- `ContextOptionsE2ETest`: **PASSED** (3 tests)

### Manual Verification
- Visual inspection of `CreServiceImpl.java` changes confirms correct wiring and logic implementation.

## Self-Check: PASSED
- Created files exist: N/A (modified existing)
- Commits exist: YES (2bd3978, c116c11, 8bfb2cd, 73f38ed)
