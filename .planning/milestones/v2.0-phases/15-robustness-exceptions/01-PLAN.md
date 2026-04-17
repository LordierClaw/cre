# Phase 15: Robustness & Exceptions

## Objective
Standardize error handling, improve null safety, and refine comment processing for better token efficiency and reliability.

## Context
- Current implementation uses a mix of standard Java exceptions.
- All comments are currently preserved, which is token-inefficient.
- Null safety is inconsistent across the service layer.

## Requirements

### R1: Custom Exception Hierarchy
- [ ] Create `com.cre.core.exception.CreException` (checked base class).
- [ ] Create `ProjectNotFoundException`, `SymbolNotFoundException`, `IndexingException`.
- [ ] Refactor `CreService` and `CreServiceImpl` to throw these exceptions.
- [ ] Update `CreController` and `McpConfig` to catch and map these to descriptive MCP error responses.

### R2: Selective Comment Pruning
- [ ] Implement logic to keep only Javadoc-style comments (`/** ... */`).
- [ ] Strip all other inline (`//`) and block (`/* ... */`) comments.
- [ ] Ensure pruning happens before `LexicalPreservingPrinter` or via an AST transformation.

### R3: Null Safety & Validation
- [ ] Update internal methods to return `Optional<T>` where appropriate.
- [ ] Use `Objects.requireNonNull` for all required method parameters.
- [ ] Standardize file accessibility checks.

## Implementation Steps

### Step 1: Exception Hierarchy
1. Define `CreException` and its subclasses.
2. Update `CreService` interface signatures.
3. Refactor `CreServiceImpl` to use new exceptions.
4. Update `McpConfig` and `CreController` error handling.

### Step 2: Comment Pruning
1. Create a `CommentPruner` utility (or integrate into `CreServiceImpl`).
2. Implement AST-level comment removal (keeping only `JavadocComment`).
3. Update `buildIntegratedView` and `getFileStructure` to apply comment pruning.

### Step 3: Null Safety Refactor
1. Audit `CreServiceImpl` and `ProjectManager` for nullable returns.
2. Wrap results in `Optional`.
3. Add `Objects.requireNonNull` checks at all entry points.

## Verification Plan

### Automated Tests
- `CreServiceTest`: Add tests for exception cases (symbol not found, project not found).
- `CreServiceTest`: Add tests to verify only Javadocs are preserved in the output.
- `RealProjectE2ETest`: Verify ingestion still works with new exception handling.

### Manual Verification
- Trigger an error (e.g., call `get_context` with a fake node) and verify the MCP error message is descriptive.
- Verify `get_context` output length is reduced after non-Javadoc comment removal.

## Success Criteria
- [ ] `CreException` hierarchy is used throughout the service layer.
- [ ] Only Javadoc comments are present in `get_context` and `get_file_structure` output.
- [ ] No `null` is returned from methods that could reasonably return `Optional`.
- [ ] All tests pass.
