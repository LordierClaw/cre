---
phase: 05-advanced-plugins
plan: 01
type: execute
wave: 1
replan_reviews: .planning/phases/05-advanced-plugins/05-REVIEWS.md
depends_on: ["04-ranking-pruning"]
files_modified:
  - src/main/java/com/cre/core/graph/model/EdgeType.java
  - src/main/java/com/cre/core/plugins/ExceptionFlowPlugin.java
  - src/main/java/com/cre/core/plugins/PluginRegistry.java
  - src/main/java/com/cre/tools/rank/RankingPruner.java
  - src/test/java/com/cre/tools/ExceptionFlowPluginDeterminismTest.java
  - src/test/java/com/cre/tools/ExceptionFlowPluginIntegrationTest.java
  - src/test/java/com/cre/fixtures/ExceptionFlowController.java
autonomous: false
requirements: []
must_haves:
  - **Scope lock (D-01, D-02):** Ship **exception-flow** graph enrichment only; **no** event-flow or domain-rule plugins in production code (test-only scaffolding forbidden unless a single trivial shared type is unavoidable — prefer zero extra plugins).
  - **Determinism:** Plugin order fixed (**SpringSemanticsPlugin** then **ExceptionFlowPlugin**); enrichment walks `javaFiles` in the provided list order; uses JavaParser the same way as existing plugins; all new edges use `GraphEngine` APIs that participate in `sortedEdges()` / `NodeId` rules — **no** `Random`, parallel streams on shared state, or nondeterministic collection iteration in enrichment.
  - **Additive API:** No breaking changes to MCP tool names/signatures, `GetContextResponse` field set, `slice_version`, or existing `metadata.evidence` keys (`deterministic_ast`, `spring_semantics`, etc.); new behavior is **additional** `EdgeType` values and optional **additive** evidence keys only if strictly needed for placeholders.
  - **Phase 4 ranking/pruning compatibility:** `RANKING_VERSION`, `PRUNE_POLICY`, default `top_k`/`score_floor`, `score_components_used` list (`depth_decay`, `edge_type`, `uses_field`, `degree`) remain semantically unchanged; new exception-related `EdgeType`s have an **explicit** scoring policy in `RankingPruner` Javadoc — **either** zero incident milliscore (visibility-only, like unlisted structural edges) **or** fixed saturating milliscore bonuses with unit tests; **no** O(N×E) regression in the pruner.
  - **Registration:** Extend the **fixed ordered** `PluginRegistry` list (same determinism model as Phase 2); **defer** SPI/config-only registration to a later slice unless this milestone explicitly adds it (avoid scope creep).
  - **trace_flow:** **Out of scope** for this plan — `TraceFlowTool` remains CALLS-oriented; exception edges are consumable via **`get_context` / `expand` slice edges** and graph JSON when endpoints are retained (documented non-goal).
  - **Fail-soft:** Incomplete exception analysis uses existing placeholder/evidence patterns where needed; never remove Spring or core evidence keys.
---

# Phase 05 — Advanced Plugins Plan (execute): exception-flow first

## Wave 1 Plans

**Single executable plan** covering graph taxonomy + plugin + tests + checkpoint. Event-flow and domain plugins remain deferred per `05-CONTEXT.md`.

### Plan Objective

Deliver a deterministic **exception-flow** `GraphPlugin` that enriches the AST-backed graph with new non-`CALLS` edges so `get_context` / `expand` surfaces exception-related structure **without** extending BFS frontiers, **without** breaking Phase 4 ranking metadata, and with **additive** JSON contracts.

### Chosen MVP semantics (from `05-RESEARCH.md` Option D, minimized)

1. **Intra-method catch linkage (Option A):** For each `MethodDeclaration`, walk `TryStmt` / `CatchClause` bodies; resolve **method calls** inside catch blocks using the **same deterministic resolution rules** as the indexer / `SpringSemanticsPlugin` (reuse helpers or patterns — no ad hoc string matching).
2. **Spring global handlers (Option C), if and only if** resolvable with **strict, testable rules:** Link `@ControllerAdvice` / `@ExceptionHandler` methods to controller entry or service methods when exception types match under a **documented deterministic policy** (e.g. exact assignable type by simple FQN match only — **no** full hierarchy solver in this phase unless already present in codebase). If hierarchy is too heavy, ship **catch-linkage only** and document `@ExceptionHandler` as follow-up.

**Non-goals:** Full exception type propagation across inheritance; `trace_flow` inclusion of exception edges; SPI plugin loading.

### Tasks

<task>
  <name>05-01: Add exception `EdgeType` values and `ExceptionFlowPlugin` enrichment</name>
  <files>
    <file>src/main/java/com/cre/core/graph/model/EdgeType.java</file>
    <file>src/main/java/com/cre/core/plugins/ExceptionFlowPlugin.java</file>
    <file>src/main/java/com/cre/core/plugins/PluginRegistry.java</file>
  </files>
  <read_first>
    <file>.planning/phases/05-advanced-plugins/05-CONTEXT.md</file>
    <file>.planning/phases/05-advanced-plugins/05-RESEARCH.md</file>
    <file>src/main/java/com/cre/core/plugins/GraphPlugin.java</file>
    <file>src/main/java/com/cre/core/plugins/SpringSemanticsPlugin.java</file>
    <file>src/main/java/com/cre/core/graph/GraphEngine.java</file>
    <file>src/main/java/com/cre/core/graph/model/EdgeType.java</file>
  </read_first>
  <action>
1) **Edge taxonomy:** Append new `EdgeType` enum constants for exception-flow semantics. Names must sort and serialize deterministically (`type().name()` in JSON). Suggested pair (adjust if codebase conventions differ): **`CATCH_INVOKES`** — from the **enclosing method** `NodeId` to each **callee method** `NodeId` invoked from a **catch clause body** (not normal try-body calls); optionally **`EXCEPTION_HANDLER_DISPATCH`** — from a throwing/handleable site to an `@ExceptionHandler` method `NodeId` **only** if task §2 is implemented. Do **not** reuse `CALLS` for these relationships.

2) **ExceptionFlowPlugin:** New final class implementing `GraphPlugin` with a stable `pluginId()` string (e.g. `exception_flow`).

3) **Parsing loop:** Iterate `javaFiles` in list order; parse each file with JavaParser; visit method bodies; for each method, find try/catch structures, then catch bodies; collect **method call expressions** and resolve to `NodeId`s using the **same** resolution approach as existing indexer/plugin code (extract private helpers if needed — avoid duplicating fragile FQN logic).

4) **Edges:** For each resolved callee, call `graph.addEdge(...)` (or the project’s canonical API) with the new `EdgeType`s. Skip unresolved calls silently or record **additive** evidence/placeholders only if the project already has a pattern for partial plugin mapping — **do not** introduce breaking evidence key renames.

5) **Spring @ExceptionHandler (optional slice):** If implemented, run **after** Spring plugin so annotations and bean metadata exist; restrict to deterministic matching documented in class Javadoc (exact types, explicit allowlist, or “skip if ambiguous”). If any ambiguity, emit **no** edge for that pair.

6) **PluginRegistry:** Append `new ExceptionFlowPlugin()` **after** `SpringSemanticsPlugin` in `PLUGINS`. When `pluginsEnabled == false`, existing early return must skip **all** enrichment including exception (no exception-only partial run).

7) **No changes** to `GetContextTool` BFS/traversal unless a separate task discovers a mandatory hook — default is **edges appear in slice output when both endpoints are in the retained candidate set**, same as other non-`CALLS` edges.
  </action>
  <acceptance_criteria>
1) `EdgeType.java` contains at least **`CATCH_INVOKES`** and documents new values in a one-line comment if the codebase does so for other enums.
2) `ExceptionFlowPlugin.java` exists and implements `GraphPlugin`.
3) `grep -n "ExceptionFlowPlugin\|SpringSemanticsPlugin" src/main/java/com/cre/core/plugins/PluginRegistry.java` shows **Spring** before **Exception** in the `List.of(...)` argument order.
4) `mvn -q -DskipITs compile` exits 0.
5) No new MCP classes or tool registrations in this task.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs compile`
  </verify>
  <done>
Exception-flow edges are emitted deterministically from catch bodies (and optional Spring handler linkage) through a registered plugin without changing tool APIs.
  </done>
</task>

<task>
  <name>05-02: Phase 4 ranking compatibility — document and pin milliscore behavior for new edge types</name>
  <files>
    <file>src/main/java/com/cre/tools/rank/RankingPruner.java</file>
  </files>
  <read_first>
    <file>src/main/java/com/cre/tools/rank/RankingPruner.java</file>
    <file>.planning/phases/04-ranking-pruning/04-CONTEXT.md</file>
  </read_first>
  <action>
1) **Explicit scoring policy:** Update `RankingPruner` class Javadoc weight table so new exception `EdgeType` values are listed with **either**:
   - **Zero incident milliscore** (edges are visibility-only in `aggregateBonuses` — same as types not given a bonus branch today), **or**
   - **Fixed integer bonuses** added in the **single** `graph.sortedEdges()` pass, with saturating add and caps consistent with existing `MAX_MILLISCORE` policy.

2) **Implementation:** If bonuses are **zero**, add an explicit `else if` chain or comment block naming each new `EdgeType` and stating “no incident bonus” so future readers do not assume omission is accidental. If bonuses are **non-zero**, extend `aggregateBonuses` symmetrically for `fromIn`/`toIn` like other structural types and **do not** change existing weights for `CALLS`, `ENTRY_POINT`, `SERVICE_LAYER`, `DEPENDS_ON`, `USES_FIELD`.

3) **Do not** add new `score_components_used` strings unless the scoring model gains a **documented** new component category (default: keep the four existing components; exception influence flows through `edge_type` incident bonuses only).

4) Preserve **O(E)** single-pass aggregation — no nested full-graph scans per candidate.
  </action>
  <acceptance_criteria>
1) Javadoc mentions each new `EdgeType` name alongside its milliscore policy.
2) `grep -n "CATCH_INVOKES\|EXCEPTION_HANDLER\|exception" src/main/java/com/cre/tools/rank/RankingPruner.java` returns at least one documentation or code line per introduced edge type.
3) `mvn -q -DskipITs test -Dtest=ContextRankingScoringTest` exits 0 (existing Phase 4 tests unchanged in behavior for graphs **without** exception edges — if a test must relax an assertion, document why in the test commit only).
4) No change to `RANKING_VERSION` string value `cre.rank.v1` unless a breaking scoring change is required (it should not be for this phase).
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test -Dtest=ContextRankingScoringTest`
  </verify>
  <done>
Ranking/pruning behavior for Phase 4 edge types is preserved; new exception edges have an explicit, test-backed scoring story.
  </done>
</task>

<task>
  <name>05-03: Fixtures and automated tests — determinism, integration, regression</name>
  <files>
    <file>src/test/java/com/cre/fixtures/ExceptionFlowController.java</file>
    <file>src/test/java/com/cre/tools/ExceptionFlowPluginDeterminismTest.java</file>
    <file>src/test/java/com/cre/tools/ExceptionFlowPluginIntegrationTest.java</file>
    <file>src/test/java/com/cre/tools/GetContextSchemaTest.java</file>
    <file>src/test/java/com/cre/tools/PluginsEnabledDisabledTest.java</file>
  </files>
  <read_first>
    <file>.planning/phases/05-advanced-plugins/05-VALIDATION.md</file>
    <file>src/test/java/com/cre/tools/SpringSemanticsPluginDeterminismTest.java</file>
    <file>src/test/java/com/cre/tools/ContextRankingScoringTest.java</file>
    <file>src/test/java/com/cre/tools/GetContextSchemaTest.java</file>
  </read_first>
  <action>
1) **Fixture:** Add `ExceptionFlowController.java` (or similarly named) under `src/test/java/com/cre/fixtures/` with **readable** try/catch patterns (and optional `@ControllerAdvice` only if task 05-01 implements Option C). Keep sources small and deterministic.

2) **ExceptionFlowPluginDeterminismTest:** Build `CreContext` (or project equivalent) twice from the same roots/file list; assert **identical** sorted exception-related edges (or identical `graph.sortedEdges()` filter for new types). Mirror patterns from `SpringSemanticsPluginDeterminismTest`.

3) **ExceptionFlowPluginIntegrationTest:** Run `GetContextTool` (or `GraphEngine` + pruner) on a fixed entry `node_id` and assert new edges appear in JSON when endpoints survive pruning; assert **metadata** keys from Phase 4 (`ranking_version`, `prune_policy`, counts) still present and shapes unchanged; assert **no** dangling placeholder `target_node_id` beyond existing rules.

4) **GetContextSchemaTest:** Assert additive expectations only — existing evidence keys preserved; if new evidence keys are added, assert presence **without** removing old key assertions.

5) **PluginsEnabledDisabledTest (or equivalent):** When plugins disabled, assert **no** `CATCH_INVOKES` / `EXCEPTION_HANDLER_DISPATCH` edges (or your chosen type names) in output.

6) **Regression:** `GetContextSchemaTest`, `ExpandToolContractTest`, `ContextRankingScoringTest` per `05-VALIDATION.md` row 05-01-03.

7) **Full suite:** Run complete unit tests before checkpoint.
  </action>
  <acceptance_criteria>
1) Files exist: `ExceptionFlowPluginDeterminismTest.java`, `ExceptionFlowPluginIntegrationTest.java`, fixture Java under `com/cre/fixtures/`.
2) `mvn -q -DskipITs test -Dtest=ExceptionFlowPluginDeterminismTest,ExceptionFlowPluginIntegrationTest,GetContextSchemaTest,ExpandToolContractTest,ContextRankingScoringTest` exits 0.
3) `mvn -q -DskipITs test` exits 0.
4) `grep -n "CATCH_INVOKES\|EXCEPTION_HANDLER" src/test/java/com/cre/tools/ExceptionFlowPluginDeterminismTest.java` confirms assertions reference new edge types (adjust names if enum names differ).
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test`
  </verify>
  <done>
Exception-flow behavior is covered by determinism and integration tests; Phase 4 and schema regressions stay green.
  </done>
</task>

<task type="checkpoint">
  <name>05-04: Checkpoint — validation artifact, roadmap traceability, scope sign-off</name>
  <files>
    <file>.planning/phases/05-advanced-plugins/05-VALIDATION.md</file>
    <file>.planning/phases/05-advanced-plugins/05-PLAN.md</file>
    <file>.planning/ROADMAP.md</file>
  </files>
  <read_first>
    <file>.planning/phases/05-advanced-plugins/05-VALIDATION.md</file>
    <file>.planning/phases/05-advanced-plugins/05-CONTEXT.md</file>
    <file>.planning/ROADMAP.md</file>
  </read_first>
  <action>
1) **Roadmap mapping:** Build a short table in `05-VALIDATION.md` (or extend existing section) linking **ROADMAP Phase 5 success criteria #1–#3** to concrete artifacts:
   - **#1** “At least one additional plugin category…” → `ExceptionFlowPlugin` + fixture + integration test output showing new edges.
   - **#2** “Third-party or domain plugins without modifying core source…” → document **actual** delivery: **registry list extension** in `PluginRegistry` as the supported path for this milestone; note **SPI deferred** if not implemented (aligns `05-RESEARCH.md` / roadmap tension).
   - **#3** “Plugin interactions with ranking/pruning…” → `RankingPruner` Javadoc policy + `ContextRankingScoringTest` / integration tests proving non-regression or intentional weights.

2) **Update `05-VALIDATION.md`:** Set per-task table **File Exists** ✅ and **Status** ✅ for rows `05-01-01` … `05-01-04` when tests are green; align task IDs with this plan’s tasks (`05-01` … `05-04` map to validation rows as named in the table).

3) **Nyquist / Wave 0:** Set `nyquist_compliant: true` and `wave_0_complete: true` in frontmatter when Wave 0 file checklist and automated rows are satisfied.

4) **Manual row:** Keep or refine **OBS-ADV-01** manual spot-check instructions for semantic review of exception edges on fixtures.

5) **ROADMAP.md anti-drift:** Update Phase 5 **Plans** subsection: replace `05-01: TBD` with pointers to `05-PLAN.md` and checklist `05-01` … `05-04` completed when executed; do **not** alter Phase 5 goal/success criteria wording.
  </action>
  <acceptance_criteria>
1) `05-VALIDATION.md` frontmatter includes `nyquist_compliant: true` after verification.
2) Per-task table shows ✅ for test files and green status for completed rows.
3) `grep -n "05-01-0\|ROADMAP\|success\|ExceptionFlow\|RankingPruner" .planning/phases/05-advanced-plugins/05-VALIDATION.md` shows traceability notes for Phase 5 criteria.
4) `grep -n "05-PLAN\\.md\\|phases/05-advanced-plugins" .planning/ROADMAP.md` shows Phase 5 Plans updated to reference this plan file.
5) `mvn -q -DskipITs test` exits 0 at checkpoint time.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test` and re-read `05-VALIDATION.md` for consistent task IDs vs this plan (`05-01-01` … `05-01-04`).
  </verify>
  <done>
Validation artifact, Nyquist flags, and roadmap plans reflect executed work; exception-flow scope is signed off with explicit deferrals (event/domain, SPI, trace_flow) documented.
  </done>
</task>

---

*Plan created for Phase 05 — advanced-plugins (exception-flow first). Execution order: 05-01 → 05-02 → 05-03 → 05-04.*
