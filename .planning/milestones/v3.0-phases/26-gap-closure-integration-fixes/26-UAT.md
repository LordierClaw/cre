---
status: testing
phase: 26-gap-closure-integration-fixes
source: [25-01-SUMMARY.md, 25-02-SUMMARY.md, 25-03-SUMMARY.md, 26-01-SUMMARY.md, 26-03-SUMMARY.md]
started: 2026-04-17T13:10:00.000Z
updated: 2026-04-17T13:10:00.000Z
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

number: 1
name: Cold Start Smoke Test
expected: |
  Start the MCP server using `stdio`. The server should boot without errors and respond to a `list_tools` request.
awaiting: user response

## Tests

### 1. Cold Start Smoke Test
expected: |
  Start the MCP server using `stdio`. The server should boot without errors and respond to a `list_tools` request.
result: [pending]

### 2. Dynamic Traversal Capping
expected: |
  Call `get_context` for a complex symbol with `depth: 1` then with `depth: 4`.
  The `depth: 1` query should pull in a larger number of immediate nodes (cap 150) compared to the deep `depth: 4` query (cap 50), which should be more selective.
result: [pending]

### 3. Surgical Comment Pruning
expected: |
  Request a symbol in a class with multiple methods and comments.
  The target function should have its Javadocs and internal comments (`//` or `/* */`).
  Neighboring "skeleton" methods (those without bodies) should have ALL comments stripped.
result: [pending]

### 4. Target Class Javadoc Preservation
expected: |
  The class-level Javadoc of the class containing the target symbol should be preserved.
  Class-level Javadocs for other "skeleton" classes (reconstructed but not target) should be stripped.
result: [pending]

### 5. Formatting Cleanup (Newlines)
expected: |
  Observe the output of `get_context`.
  There should be no instances of three or more consecutive newlines (i.e., more than one empty line between code blocks).
result: [pending]

### 6. Marker Alignment
expected: |
  Observe the placement of `<omitted_functions/>` and `<omitted_properties/>` markers.
  Each marker should be on its own line, respecting the current indentation level.
result: [pending]

### 7. Record Optimization
expected: |
  Request a symbol inside a Java `record`.
  The output should show a surgical view of the record: relevant components/methods preserved with their Javadocs, while irrelevant components are omitted.
result: [pending]

### 8. Signature Normalization (FQN Support)
expected: |
  Request a symbol for a method whose source definition uses Fully Qualified Names (FQN) in parameters (e.g., `process(java.lang.String)`).
  The server should successfully find and return the method body even if the query uses simple names (e.g., `process(String)`), or vice-versa.
result: [pending]

## Summary

total: 8
passed: 0
issues: 0
pending: 8
skipped: 0

## Gaps

[none yet]
