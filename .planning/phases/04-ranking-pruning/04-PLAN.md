---
phase: 04-ranking-pruning
plan: 01
type: execute
wave: 1
replan_reviews: .planning/phases/04-ranking-pruning/04-REVIEWS.md
depends_on: ["03-expand-on-demand"]
files_modified:
  - src/main/java/com/cre/tools/rank/RankingPruner.java
  - src/main/java/com/cre/tools/GetContextTool.java
  - src/test/java/com/cre/tools/ContextRankingScoringTest.java
  - src/test/java/com/cre/tools/ContextPruningPolicyTest.java
  - src/test/java/com/cre/tools/GetContextSchemaTest.java
  - src/test/java/com/cre/tools/ExpandToolContractTest.java
  - src/test/java/com/cre/tools/ExpandToolDeterminismTest.java
autonomous: false
requirements: []
must_haves:
  - Weighted structural scoring (D-01, D-02) using deterministic integer signals only — edge-type importance, depth decay from BFS `dist`, variable/field-style relevance via `USES_FIELD` (and graph-local degree caps), versioned by `ranking_version` (e.g. `cre.rank.v1`)
  - **Performance:** incident-edge and `USES_FIELD`-style bonuses computed in **one linear pass** over `graph.sortedEdges()` (aggregate into per-node maps / caps), **O(N + E)** for candidate set size N and edge count E — **no** repeated full-edge scans per candidate (**no** O(N×E) patterns)
  - Top-K + minimum score floor pruning (D-03, D-04); center node always retained; pruning bounded and fail-soft with prior-phase placeholder rules; **explicit pruned-target placeholder rule** (no dangling `target_node_id` after prune — see task 04-02)
  - **Integer safety:** milliscore weights and accumulation use **saturating** addition (or equivalent) to a documented ceiling so scores cannot overflow `int`; weight table + max bound documented in `RankingPruner` Javadoc (D-07 living reference)
  - Stable lexical tie-break on equal scores — nodes by `node_id` string; edges by `(from, to, type)` after filtering to retained endpoints (D-05)
  - Compact ranking metadata only under `metadata` (D-06, D-07) — `ranking_version`, `prune_policy`, `top_k`, `score_floor`, `pruned_count`, `retained_count`, `score_components_used`; no per-node score tables; additive to existing `evidence` / expand keys
  - `GetContextResponse` record fields and MCP tool names/signatures unchanged; `expand` applies one ranking pass on the merged slice after `mergeSlices` (not per half-slice)
  - **Regression coverage:** automated test for **merged-slice then prune** — node present in expand target slice can be pruned after merge; `retained_count` / `nodes[]` / metadata stay consistent (task 04-03)
---

# Phase 04 — Ranking & Pruning Plan (execute)

## Wave 1 Plans

**Terminology (Wave 0 vs Wave 1):** In this repo’s validation docs, **Wave 0** means prerequisite test artifacts and `File Exists` / missing-test rows in `04-VALIDATION.md` (unit files like `ContextRankingScoringTest` before they land). **Wave 1** means this execute plan’s implementation tasks (`04-01`–`04-04`) and the `wave: 1` frontmatter on this file — i.e. coding and integration work, not the planning/research wave. Checkpoint `04-04` toggles `wave_0_complete` in `04-VALIDATION.md` when **validation** Wave 0 checklist + automated rows are satisfied, independent of the YAML `wave` field here.

### Plan Objective

Add a deterministic ranking and pruning pipeline so `get_context` and `expand` return smaller, higher-signal slices without changing the public response type or MCP API. Scores use weighted structural heuristics; retention uses top-K plus a score floor; observability is limited to compact metadata fields agreed in `04-CONTEXT.md`.

### Tasks

<task>
  <name>04-01: Add pure `RankingPruner` with integer structural scoring and top-K + floor policy</name>
  <files>
    <file>src/main/java/com/cre/tools/rank/RankingPruner.java</file>
  </files>
  <read_first>
    <file>.planning/phases/04-ranking-pruning/04-CONTEXT.md</file>
    <file>.planning/phases/04-ranking-pruning/04-RESEARCH.md</file>
    <file>src/main/java/com/cre/core/graph/GraphEngine.java</file>
    <file>src/main/java/com/cre/core/graph/model/EdgeType.java</file>
    <file>src/main/java/com/cre/core/graph/NodeId.java</file>
  </read_first>
  <action>
Create `src/main/java/com/cre/tools/rank/RankingPruner.java` as a final class with no randomness and no unordered collections in the scoring/selection path.

1) Define compile-time constants:
   - `RANKING_VERSION = "cre.rank.v1"`
   - `PRUNE_POLICY = "top_k_floor"`
   - Default `DEFAULT_TOP_K` and `DEFAULT_SCORE_FLOOR` as positive integers (milliscore-style scale to avoid JSON floats); document defaults in class Javadoc in one line each.

2) Implement **weighted structural scoring** (D-01, D-02): for each candidate `NodeId` in the BFS `dist` map, compute a single non-negative `int` score from fixed integer weights combining:
   - depth decay from `dist.get(id)` relative to center (smaller hop → higher contribution),
   - bonuses for structurally important `EdgeType` values (e.g. `SERVICE_LAYER`, `ENTRY_POINT`, `CALLS` participation) and capped outgoing `USES_FIELD` counts — **without** scanning `graph.sortedEdges()` once per candidate. **Required algorithm:** one deterministic linear pass over `graph.sortedEdges()` (already sorted) to build **per-node aggregates** (e.g. `Map<NodeId,Integer>` incident bonus sums, or separate maps for `USES_FIELD`-out counts and incident-type bonuses), then **O(|C|)** final score assembly per candidate from `dist` + lookups into those maps. Total **O(N + E)** for N = |C| and E = |edges|; **do not** implement O(N×E) “for each candidate, scan all edges.”
   - optional bounded fan-in/fan-out from `outgoingCalls` / `incomingCalls` where already sorted (still O(1) or O(degree) per node, not full-graph rescans).

2b) **Integer bounds / overflow (review):** choose fixed small integer weights; combine components with **saturating add** to a documented `MAX_MILLISCORE` (or saturate at `Integer.MAX_VALUE`) so pathological graphs cannot throw on `int` overflow. Document in class Javadoc a **small table**: each `EdgeType` / component weight, depth decay formula, and the **maximum representable score** after saturation. Prefer no `float`/`double` in scoring paths.

3) Implement **retention** (D-03, D-04) as **one** ordered, deterministic procedure. Let input **candidate** set be `C` (typically `dist.keySet()`), `N = |C|`, and `center ∈ C`. Compute a non-negative integer `score(n)` for every `n ∈ C`. Sort `C` into list `L` by `(score descending, node_id.toString() ascending)` (D-05). Build `retained` in this exact order:
   - **Step A:** insert `center` into `retained` unconditionally (protected — kept even if `score(center) < scoreFloor`).
   - **Step B:** walk `L` from first to last; for each `n` in `L`, if `n` is already in `retained`, continue; else if `score(n) >= scoreFloor` **and** `|retained| < topK`, add `n` to `retained`.
   - **Counts (must match metadata):** `retained_count = |retained|`; `pruned_count = N - retained_count` (equivalently: candidates not selected into `retained`). Invariant: `retained_count + pruned_count = N`; every candidate is either retained or pruned, never both.

4) Expose a single package-visible or public `prune(...)` method returning a record or small DTO: retained node set, `prunedCount`, `retainedCount`, `List` of component name strings for `score_components_used` (e.g. `depth_decay`, `edge_type`, `uses_field`, `degree` — fixed list matching what the implementation actually uses).

5) Do **not** reference MCP, JSON, or `GetContextResponse` in this class.
  </action>
  <acceptance_criteria>
1) `grep -n "cre.rank.v1" src/main/java/com/cre/tools/rank/RankingPruner.java` returns at least one match.
2) `grep -n "top_k_floor\|PRUNE_POLICY" src/main/java/com/cre/tools/rank/RankingPruner.java` shows `top_k_floor` string used as policy id.
3) `grep -n "RankingPruner" src/main/java/com/cre/tools/rank/RankingPruner.java` and file exists under `com/cre/tools/rank/`.
4) No `java.util.Random`, `HashSet` iteration order, or `stream().parallel()` in `RankingPruner.java` (verify with `grep -E "Random|parallel\\(" src/main/java/com/cre/tools/rank/RankingPruner.java` exits 1 or empty).
5) Javadoc contains an explicit **weight table** (edge types / components) and **overflow policy** (saturating add + max score), grep-verifiable: `grep -nE "MAX_MILLISCORE|saturat|weight" src/main/java/com/cre/tools/rank/RankingPruner.java` returns relevant lines.
6) **No O(N×E) edge rescans:** scoring path uses **one** aggregation over `graph.sortedEdges()` (verify: `grep -n "sortedEdges" src/main/java/com/cre/tools/rank/RankingPruner.java` — at most **one** `for`/stream iteration site over `sortedEdges()` in score computation, or a single private method called once per `prune` such as `accumulateIncidentBonuses`; nested per-candidate full-edge loops are **forbidden**).
7) `mvn -q -DskipITs test -Dtest=ContextRankingScoringTest,ContextPruningPolicyTest` passes after task 04-03 adds those tests (may be deferred until 04-03; if 04-01 is committed alone, `mvn -q -DskipITs compile` exits 0).
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs compile` (before tests land); after 04-03: `mvn -q -DskipITs test -Dtest=ContextRankingScoringTest,ContextPruningPolicyTest`
  </verify>
  <done>
Deterministic integer scoring (O(N+E) edge aggregation, saturating milliscore math) and top-K + floor selection live in a testable pure module with version and policy identifiers.
  </done>
</task>

<task>
  <name>04-02: Integrate pruning into `GetContextTool` and attach compact metadata (single path for `expand`)</name>
  <files>
    <file>src/main/java/com/cre/tools/GetContextTool.java</file>
  </files>
  <read_first>
    <file>src/main/java/com/cre/tools/GetContextTool.java</file>
    <file>src/main/java/com/cre/tools/rank/RankingPruner.java</file>
    <file>.planning/phases/04-ranking-pruning/04-RESEARCH.md</file>
  </read_first>
  <action>
1) After BFS completes and `dist` / full candidate set is known, and **before** building `nodes`, `edgesOut`, and `sliced` lists in `buildSlice`, call `RankingPruner.prune(...)` with `candidates = dist.keySet()`, `center` = BFS root, same `dist` and `graph`, using server-side defaults for `topK` and `scoreFloor` (constants aligned with `RankingPruner` defaults).

2) Replace the `included` list used for materialization with `retained` from the pruner, sorted by `node_id` string ascending (preserve existing global sort order for arrays).

3) Filter `edgesOut` to edges where both endpoints are in `retained`; keep iteration order from `graph.sortedEdges()` so edge ordering remains `(from, to, type)` lexicographic as today.

4) Filter `sliced` and `nodes` to retained ids only (same FIELD skip rules as current code).

5) **Placeholder rule for pruned targets (review, D-04):** After pruning, **no** placeholder may reference a `target_node_id` that is absent from `retained` / `nodes[]`. Concretely:
   - For `depth_limit`, `budget_limit`, and any other placeholder carrying `target_node_id` (or equivalent) that points at a **pruned** node: **drop** that placeholder from the emitted list (the target is not “omitted by depth/budget” in the visible slice — it was removed by ranking policy). Do **not** leave dangling references.
   - Do **not** introduce a new MCP field or placeholder **type** in this phase unless required by existing schema — **prefer drop-only** for pruned targets to preserve additive API behavior; if a future `pruning_limit` type is desired, defer to a later phase unless `Placeholder` already supports a generic reason string without schema break.
   - Continue to reuse existing `shouldKeepPlaceholder` / merge semantics where they still apply: e.g. `depth_limit` drops when `target_node_id` is **present** in retained nodes; do not strip `missing_node` or `missing_spring_mapping` incorrectly.
   - Add a **grep-friendly** comment in `GetContextTool` near placeholder filtering documenting “pruned target → dropped” for code review.

6) Extend `metadata()` merge path: start from `metadata()` as today (`evidence` preserved), then put additive keys at top level of the same `metadata` map:
   - `ranking_version` (string)
   - `prune_policy` = `top_k_floor`
   - `top_k`, `score_floor` (JSON numbers as integers)
   - `pruned_count`, `retained_count`
   - `score_components_used` — array of strings or compact map of names only, matching pruner output (D-06, D-07)

7) For `expand(String nodeIdRaw)`: after `mergeSlices(center.response(), target.response())`, run **one** pruning/materialization pass on the merged node id set if the implementation re-materializes from merged ids — i.e. do not rank center and target slices separately; final response lists and ranking metadata reflect the merged retained set only. If refactor requires re-invoking an internal `buildSlice`-like materialization from a merged node-id set, extract a private helper so `execute` and `expand` share the same post-prune pipeline.

8) Do not add new public methods on MCP; do not change `GetContextResponse` field list; do not rename `slice_version`.
  </action>
  <acceptance_criteria>
1) `grep -n "RankingPruner" src/main/java/com/cre/tools/GetContextTool.java` shows import and usage.
2) `grep -E "ranking_version|pruned_count|retained_count|score_floor|top_k|prune_policy|score_components_used" src/main/java/com/cre/tools/GetContextTool.java` matches all seven key names at least once across metadata assembly.
3) `grep -n "mergeSlices" src/main/java/com/cre/tools/GetContextTool.java` — pruning for expand occurs after merge (either by grep for comment "after merge" near ranking call or by structure: single helper used post-merge).
4) `grep -nE "pruned target|target_node_id.*retained|dangling" src/main/java/com/cre/tools/GetContextTool.java` finds the documented pruned-placeholder rule (comment or helper name).
5) `mvn -q -DskipITs test -Dtest=GetContextSchemaTest,ExpandToolContractTest,ExpandToolDeterminismTest` exits 0 after task 04-03 updates expectations if needed.
6) No new MCP tool registrations in `CreMcpServer`: tools are declared via chained `tool(...)` calls under `.tools(` (there is no `register` API). Verify the tool surface is unchanged — exactly **four** registration call sites matching `^[[:space:]]+tool\\(` (lines ~45–69) and tool names remain `get_context`, `expand`, `find_implementations`, `trace_flow` in that order: `grep -E '^[[:space:]]+tool\\(' src/main/java/com/cre/mcp/CreMcpServer.java | wc -l` outputs `4`; optional `git diff` shows no new `tool(` in the `.tools(` block.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test -Dtest=GetContextSchemaTest,ExpandToolContractTest,ExpandToolDeterminismTest`
  </verify>
  <done>
Both `get_context` and `expand` return pruned slices with compact ranking telemetry and unchanged response envelope.
  </done>
</task>

<task>
  <name>04-03: Automated tests — unit ranking/pruning + contract/determinism regression</name>
  <files>
    <file>src/test/java/com/cre/tools/ContextRankingScoringTest.java</file>
    <file>src/test/java/com/cre/tools/ContextPruningPolicyTest.java</file>
    <file>src/test/java/com/cre/tools/GetContextSchemaTest.java</file>
    <file>src/test/java/com/cre/tools/ExpandToolDeterminismTest.java</file>
  </files>
  <read_first>
    <file>.planning/phases/04-ranking-pruning/04-VALIDATION.md</file>
    <file>src/test/java/com/cre/tools/ExpandToolBoundsTest.java</file>
    <file>src/test/java/com/cre/tools/ExpandToolContractTest.java</file>
    <file>src/main/java/com/cre/tools/rank/RankingPruner.java</file>
  </read_first>
  <action>
1) Add `ContextRankingScoringTest.java`: build or reuse a small `GraphEngine` fixture (pattern from `ExpandToolBoundsTest` / `CreContext`) with at least two candidate nodes at equal score to assert tie-break orders by lexicographic `node_id` ascending; assert scores are integers and deterministic across two invocations. **Overflow / saturation (review):** add at least one test that exercises heavy cumulative bonuses (e.g. many incident edges or maxed components) and asserts **no uncaught overflow** — final score equals expectation under **saturating** arithmetic (same `int` result on repeated calls), matching `RankingPruner` policy.

2) Add `ContextPruningPolicyTest.java`: assert `top_k` truncation removes lowest-ranked nodes after floor filter; assert `score_floor` excludes low scores; assert `center` remains even when below floor; assert `pruned_count` + `retained_count` arithmetic matches candidate set size.

3) Extend `GetContextSchemaTest`: assert `metadata.ranking_version`, `metadata.prune_policy`, numeric `top_k`, `score_floor`, `pruned_count`, `retained_count`, and presence of `score_components_used`; assert existing `metadata.evidence` keys still present and `slice_version` unchanged.

4) Extend `ExpandToolDeterminismTest`: (a) two `expand` calls return identical JSON for `nodes`/`edges`/`sliced_code` ordering and new ranking metadata keys. **(b) Merged-slice prune regression (review):** add a dedicated test case (method Javadoc names the scenario) where **after** `mergeSlices`, the merged candidate set is large enough that **post-merge pruning** removes at least one node that **only** came from the **target** expand slice (i.e. would have been retained in isolation but is dropped after union + single prune pass). Assert: `metadata.retained_count ==` final `nodes` array size; every `target_node_id` on emitted placeholders (if any) appears in `nodes[]`; **no** placeholder references a pruned id. This locks **single pass after merge** + accounting consistency.

5) If `ExpandToolContractTest` asserts exact metadata key count, update to allow additive ranking keys only.

6) **Optional unit:** assert **placeholder drop** when a `depth_limit`-style placeholder would point at a pruned node — either in `ContextPruningPolicyTest` with a tiny graph + direct pruner/tool hook, or via integration if easier — so the rule in task 04-02 is not only documented in source.

7) **ROADMAP Phase 4 success criterion #3** (“Measured context size or noise metrics improve on representative fixtures without breaking v1 API contracts”): add **measurable before/after assertions** in tests — minimum bar (pick one approach and document the chosen fixture + parameters in the test class Javadoc):
   - **Accounting identity:** for a fixed fixture call, assert `metadata.retained_count + metadata.pruned_count` equals the BFS candidate count `N` for that same `node_id` / `depth` (same graph), proving counts partition the candidate set; **and**
   - **Strict improvement when pruning applies:** assert `metadata.pruned_count >= 1` (or `metadata.retained_count < N`) when `top_k` is set strictly below `N` on a fixture where `N > 1` (e.g. policy test graph or default fixture with depth chosen so `N` exceeds default `top_k`); **or** compare **before** = node list length (or `retained_count`) with **pruning off** (`top_k` large / floor `0` per test harness) vs **after** with defaults — assert **after** ≤ **before** and **after** < **before** when pruning is active.
   - **API contract:** existing assertions that `GetContextResponse` / JSON shape for v1 fields is unchanged remain green (no new required fields on the record; additive `metadata` keys only).
  </action>
  <acceptance_criteria>
1) Files exist: `src/test/java/com/cre/tools/ContextRankingScoringTest.java`, `src/test/java/com/cre/tools/ContextPruningPolicyTest.java`.
2) `grep -l "ContextRankingScoringTest\|ContextPruningPolicyTest" pom.xml` not required — Maven default surefire picks `*Test.java`.
3) `mvn -q -DskipITs test -Dtest=ContextRankingScoringTest,ContextPruningPolicyTest,GetContextSchemaTest,ExpandToolDeterminismTest,ExpandToolContractTest` exits 0.
4) `GetContextSchemaTest` contains assertion lines for `ranking_version` and `pruned_count` (grep-verifiable: `grep -n "ranking_version\|pruned_count" src/test/java/com/cre/tools/GetContextSchemaTest.java`).
5) Criterion #3 coverage: at least one of `ContextPruningPolicyTest`, `GetContextSchemaTest`, or a dedicated metrics test implements task `04-03` action §7 (fixture Javadoc + assertions); verify with `grep -nE 'pruned_count|retained_count' src/test/java/com/cre/tools/ContextPruningPolicyTest.java src/test/java/com/cre/tools/GetContextSchemaTest.java` (add the metrics test path to the grep if used) — output must include lines tied to the accounting identity and/or strict-improvement checks.
6) **Merged-slice prune:** `grep -nE 'mergeSlices|retained_count|target slice|post-merge' src/test/java/com/cre/tools/ExpandToolDeterminismTest.java` shows the merged-prune scenario documented (Javadoc or comment) **and** assertions tying `retained_count` to `nodes` size for that case (or adjacent method block clearly labeled).
7) **Overflow test:** `grep -nE 'saturat|overflow|MAX_|heavy' src/test/java/com/cre/tools/ContextRankingScoringTest.java` (or `ContextPruningPolicyTest.java` if overflow test lives there) returns at least one line for the saturation/overflow regression.
8) Full suite: `mvn -q -DskipITs test` exits 0.
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test`
  </verify>
  <done>
Phase 4 behavior is covered by unit tests for scoring/policy (including saturation), integration tests for schema and determinism, and a **merged expand → single prune** regression in `ExpandToolDeterminismTest`.
  </done>
</task>

<task type="checkpoint">
  <name>04-04: Checkpoint — map automation to `04-VALIDATION.md` and roadmap success criteria</name>
  <files>
    <file>.planning/phases/04-ranking-pruning/04-VALIDATION.md</file>
    <file>.planning/phases/04-ranking-pruning/04-PLAN.md</file>
    <file>.planning/ROADMAP.md</file>
  </files>
  <read_first>
    <file>.planning/phases/04-ranking-pruning/04-VALIDATION.md</file>
    <file>.planning/phases/04-ranking-pruning/04-CONTEXT.md</file>
    <file>.planning/ROADMAP.md</file>
  </read_first>
  <action>
1) Build a table mapping each roadmap Phase 4 success criterion (noise/heuristics/ordering/metrics) to concrete automated tests and metadata fields:
   - Criterion 1 (heuristic prioritization observable): point to `ContextPruningPolicyTest` + `retained_count`/`pruned_count` in schema test.
   - Criterion 2 (variable/field-style signals): point to `score_components_used` including field-related component name + `ContextRankingScoringTest` if `USES_FIELD` exercised.
   - Criterion 3 (size/noise improvement without API break): point to the **explicit** test assertions from task `04-03` §7 (accounting identity `retained_count + pruned_count == N` and strict improvement `pruned_count >= 1` or before/after node counts) plus unchanged `GetContextResponse` / schema tests proving no v1 API break; include **`ExpandToolDeterminismTest` merged-slice post-prune** case (§4b) for end-to-end accounting + no dangling placeholder refs.

2) Update `.planning/phases/04-ranking-pruning/04-VALIDATION.md` per-task table: set **File Exists** to ✅ for `ContextRankingScoringTest.java` and `ContextPruningPolicyTest.java`, set **Status** to ✅ green for rows 04-01-01 … 04-01-04 when `mvn -q -DskipITs test` is green.

3) Set YAML frontmatter `nyquist_compliant: true` and `wave_0_complete: true` in `04-VALIDATION.md` when all automated rows are green and **validation Wave 0** test-file checklist is satisfied (this flag does **not** refer to this plan’s YAML `wave: 1` — see Terminology above).

4) Confirm manual-only row OBS-01 remains documented for human spot-check of compact metadata (no automation change required).

5) **Roadmap reference update (avoid plan drift):** Edit `.planning/ROADMAP.md` Phase 4 **Plans** subsection so it no longer lists only `04-01: TBD` — add pointers to `.planning/phases/04-ranking-pruning/04-PLAN.md` and mark each plan line `04-01`…`04-04` as completed (or checked) in lockstep with executed tasks, keeping Phase 4 success criteria text unchanged.
  </action>
  <acceptance_criteria>
1) `04-VALIDATION.md` frontmatter contains `nyquist_compliant: true` after verification.
2) Per-task table lists `ContextRankingScoringTest` / `ContextPruningPolicyTest` with File Exists ✅.
3) `grep -n "04-01-0" .planning/phases/04-ranking-pruning/04-VALIDATION.md` shows updated status markers for completed tasks.
4) Document in `04-VALIDATION.md` (short bullet list) explicit mapping from ROADMAP Phase 4 success criteria #1–#3 to test class names and the criterion #3 metric lines (grep: `grep -n "ROADMAP\|success\|ContextRanking\|GetContextSchema\|pruned_count\|retained_count" .planning/phases/04-ranking-pruning/04-VALIDATION.md` returns matching lines).

5) `grep -n "04-PLAN\\.md\\|phases/04-ranking-pruning" .planning/ROADMAP.md` shows Phase 4 Plans updated to reference this plan file (anti-drift check).
  </acceptance_criteria>
  <verify>
Run: `mvn -q -DskipITs test` and re-read `04-VALIDATION.md` for consistent task IDs vs this plan (`04-01-01` … `04-01-04`).
  </verify>
  <done>
Validation artifact reflects executed tests, roadmap traceability, and sign-off readiness for Phase 4.
  </done>
</task>

---

*Plan created for Phase 04 — ranking-pruning. Execution order: 04-01 → 04-02 → 04-03 → 04-04.*
