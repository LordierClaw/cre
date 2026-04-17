# UAT: Phase 7 — Real-project Ingestion

**Status:** Active
**Phase:** 7
**Session Started:** 2026-03-27

## Objectives
- [x] Verify multi-project support via `project_root` argument in MCP tools.
- [x] Confirm automated directory ingestion (ignoring excluded folders).
- [x] Validate the 2-hour TTL cache behavior.
- [x] Test the `reset_project` tool for manual cache clearing.
- [x] Ensure E2E resolution and context reconstruction on a real project (`cre-test-project`).

## Test Sessions

### Session 1: Real Project E2E Verification
**Goal:** Run the `RealProjectE2ETest` and manually verify tool behavior if possible.

- **Test 1.1: Automated E2E Test Suite**
  - **Action:** Run `mvn test -Dtest=RealProjectE2ETest`.
  - **Expected:** Test passes, confirming ingestion, caching, and isolation.
  - **Result:** PASS (Verified indexing logs and assertions for multiple projects)

- **Test 1.2: Manual Tool Call Verification (Dry Run Logic)**
  - **Action:** Since I cannot easily run the MCP server interactively here, I will verify the tool handler logic in `CreMcpServer.java` and `ProjectManager.java`.
  - **Expected:** Code review confirms `project_root` is passed correctly and cache is utilized.
  - **Result:** PASS (Verified that `CreMcpServer` correctly handles `project_root` and `ProjectManager` implements TTL cache correctly).

## Diagnosis & Fixes
*To be filled if issues are found.*
