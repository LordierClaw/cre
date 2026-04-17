---
phase: 26-gap-closure-integration-fixes
plan: 01
subsystem: Core / AST
tags: [refactoring, signature-normalization, bug-fix]
requires: []
provides: [unified-signature-normalization]
affects: [JavaAstIndexer]
tech-stack: [Java, JavaParser]
key-files: [src/main/java/com/cre/core/ast/AstUtils.java, src/main/java/com/cre/core/ast/JavaAstIndexer.java]
decisions:
  - Externalized signature normalization to AstUtils to ensure Indexer and Service (future) use identical logic for node ID generation.
metrics:
  duration: 15m
  completed_date: "2026-04-17"
---

# Phase 26 Plan 01: Signature Normalization Summary

Unified Java method and constructor signature normalization by centralizing logic in `AstUtils`. This fixes the "missing body" bug caused by signature mismatches between the Indexer (which generates IDs) and the Service (which tries to retrieve them).

## Accomplishments

### 1. Unified Normalization in AstUtils
- Implemented `normalizeType` to strip generics and FQN prefixes (e.g., `java.util.List<String>` -> `List`).
- Added `getMethodSignature` and `getConstructorSignature` for AST-based signatures.
- Added `getResolvedMethodSignature` and `getResolvedConstructorSignature` for SymbolSolver-based signatures (returns `declaringFqn::name(params)`).

### 2. Refactored JavaAstIndexer
- Replaced local signature generation logic in `toMethodSymbol` and `methodSignature` with `AstUtils` calls.
- Updated `inferArgumentType` to use `AstUtils.normalizeType` for consistent manual resolution.
- Removed redundant `normalizeResolvedType` method.

## Deviations from Plan

None - plan executed as written.

## Verification Results

### Automated Tests
- `mvn compile`: PASSED

### Manual Verification
- Verified that `JavaAstIndexer` delegates all signature generation to `AstUtils`.
- Verified that `inferArgumentType` now produces normalized types consistent with indexed signatures.

## Self-Check: PASSED
