# Phase 05 execution summary

## 05-01: Exception `EdgeType` + `ExceptionFlowPlugin`

**Done:** Added `EdgeType.CATCH_INVOKES` (catch-body call linkage from enclosing method to resolved callee). Implemented `ExceptionFlowPlugin` using the same `MethodCallExpr` resolution rules as `JavaAstIndexer` for deterministic `NodeId`s. Registered after `SpringSemanticsPlugin` in `PluginRegistry`. Scope: intra-method catch linkage only; `@ExceptionHandler` / `EXCEPTION_HANDLER_DISPATCH` deferred.

**Verify:** `mvn -q -DskipITs compile` — exit 0.

## 05-02: RankingPruner policy for `CATCH_INVOKES`

**Done:** Documented zero incident milliscore for `CATCH_INVOKES` in class Javadoc; explicit `aggregateBonuses` branch (no bonus). `RANKING_VERSION` unchanged.

**Verify:** `mvn -q -DskipITs test -Dtest=ContextRankingScoringTest` — exit 0.

## 05-03: Fixtures and tests

**Done:** Added `ExceptionFlowController` fixture (catch invokes `userService.getUser`), `ExceptionFlowTestSupport` for deterministic multi-file loads, `ExceptionFlowPluginDeterminismTest` / `ExceptionFlowPluginIntegrationTest`, extended `GetContextSchemaTest` (additive metadata check with exception fixture) and `PluginsEnabledDisabledTest` (no `CATCH_INVOKES` when plugins off). Integration test uses `get_context` depth `1` so the callee is in the BFS candidate set and `CATCH_INVOKES` can appear in slice edges (depth `0` retains only the center).

**Verify:** `mvn -q -DskipITs test -Dtest=ExceptionFlowPluginDeterminismTest,ExceptionFlowPluginIntegrationTest,GetContextSchemaTest,ExpandToolContractTest,ContextRankingScoringTest` — exit 0; `mvn -q -DskipITs test` — exit 0.
