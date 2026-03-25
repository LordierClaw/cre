package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.graph.NodeId;
import com.cre.testsupport.GraphTestSupport;
import com.cre.tools.model.GetContextResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ExpandToolContractTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void expand_has_get_context_response_shape_and_metadata_fields() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(true);
    NodeId target =
        GraphTestSupport.requireMethod(ctx.graph(), "com.cre.fixtures.UserService", "getUser(String)");

    GetContextResponse resp = new GetContextTool(ctx.graph()).expand(target.toString());
    JsonNode tree = mapper.valueToTree(resp);

    assertThat(tree.path("slice_version").asText()).isEqualTo(GetContextTool.SLICE_VERSION);
    assertThat(tree.path("metadata").path("evidence").isObject()).isTrue();
    assertThat(tree.path("metadata").path("expansion_mode").asText()).isNotBlank();
    assertThat(tree.path("metadata").path("derived_anchor").asText()).isNotBlank();
    assertThat(tree.path("metadata").path("expansion_limit_reason").asText()).isNotBlank();

    assertThat(tree.path("nodes").isArray()).isTrue();
    assertThat(tree.path("edges").isArray()).isTrue();
    assertThat(tree.path("sliced_code").isArray()).isTrue();
    assertThat(tree.path("placeholders").isArray()).isTrue();
  }

  @Test
  void expand_unknown_node_is_fail_soft_with_missing_node_placeholder() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(true);
    GetContextResponse resp = new GetContextTool(ctx.graph()).expand("unknown::node::id");
    JsonNode tree = mapper.valueToTree(resp);

    boolean hasMissing =
        tree.path("placeholders").findValuesAsText("kind").stream()
            .anyMatch(k -> "missing_node".equals(k));
    assertThat(hasMissing).isTrue();
  }

  @Test
  void expand_merges_anchor_center_and_removes_replaced_depth_limit_placeholder() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(true);
    NodeId anchor =
        GraphTestSupport.requireMethod(
            ctx.graph(), "com.cre.fixtures.UserController", "getUser(String)");
    NodeId target =
        GraphTestSupport.requireMethod(ctx.graph(), "com.cre.fixtures.UserService", "getUser(String)");

    GetContextResponse baseline = new GetContextTool(ctx.graph()).execute(anchor.toString(), 0);
    JsonNode baselineTree = mapper.valueToTree(baseline);
    boolean baselineHasTargetDepthLimit =
        baselineTree.path("placeholders").findValuesAsText("target_node_id").stream()
            .anyMatch(id -> target.toString().equals(id));
    assertThat(baselineHasTargetDepthLimit).isTrue();

    GetContextResponse expanded = new GetContextTool(ctx.graph()).expand(target.toString());
    JsonNode expandedTree = mapper.valueToTree(expanded);

    assertThat(expandedTree.path("metadata").path("expansion_mode").asText()).isEqualTo("merged");
    assertThat(expandedTree.path("metadata").path("derived_anchor").asText()).isEqualTo(anchor.toString());

    var expandedNodeIds = expandedTree.path("nodes").findValuesAsText("node_id");
    assertThat(expandedNodeIds).contains(anchor.toString());
    assertThat(expandedNodeIds).contains(target.toString());

    boolean stillHasDepthLimitForTarget =
        expandedTree.path("placeholders").findValuesAsText("target_node_id").stream()
            .anyMatch(id -> target.toString().equals(id));
    assertThat(stillHasDepthLimitForTarget).isFalse();
  }

  @Test
  void expand_preserves_missing_spring_mapping_when_semantics_incomplete() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(false);
    NodeId target =
        GraphTestSupport.requireMethod(ctx.graph(), "com.cre.fixtures.UserService", "getUser(String)");

    GetContextResponse resp = new GetContextTool(ctx.graph()).expand(target.toString());
    JsonNode tree = mapper.valueToTree(resp);

    assertThat(tree.path("metadata").path("evidence").path("spring_semantics").asBoolean()).isFalse();
    assertThat(tree.path("metadata").path("evidence").path("gated_fallback").asBoolean()).isTrue();

    boolean hasMissing =
        tree.path("placeholders").findValuesAsText("kind").stream()
            .anyMatch(k -> "missing_spring_mapping".equals(k));
    assertThat(hasMissing).isTrue();
  }
}

