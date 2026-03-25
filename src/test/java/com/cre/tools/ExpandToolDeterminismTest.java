package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.graph.NodeId;
import com.cre.testsupport.GraphTestSupport;
import com.cre.tools.model.GetContextResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExpandToolDeterminismTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void expand_is_deterministic_for_nodes_edges_and_sliced_code_ordering() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(true);
    NodeId target =
        GraphTestSupport.requireMethod(ctx.graph(), "com.cre.fixtures.UserService", "getUser(String)");

    GetContextResponse a = new GetContextTool(ctx.graph()).expand(target.toString());
    GetContextResponse b = new GetContextTool(ctx.graph()).expand(target.toString());

    JsonNode ta = mapper.valueToTree(a);
    JsonNode tb = mapper.valueToTree(b);

    assertThat(nodeIds(ta.path("nodes"))).isEqualTo(nodeIds(tb.path("nodes")));
    assertThat(edgeTuples(ta.path("edges"))).isEqualTo(edgeTuples(tb.path("edges")));
    assertThat(nodeIds(ta.path("sliced_code"))).isEqualTo(nodeIds(tb.path("sliced_code")));
    assertThat(ta.path("metadata").path("ranking_version").asText()).isEqualTo("cre.rank.v1");
    assertThat(tb.path("metadata").path("ranking_version").asText()).isEqualTo("cre.rank.v1");
  }

  @Test
  void post_merge_pruning_keeps_metadata_and_placeholder_targets_consistent() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(true);
    NodeId target =
        GraphTestSupport.requireMethod(ctx.graph(), "com.cre.fixtures.UserService", "getUser(String)");

    GetContextResponse expanded = new GetContextTool(ctx.graph()).expand(target.toString());
    JsonNode tree = mapper.valueToTree(expanded);
    int retained = tree.path("metadata").path("retained_count").asInt();
    int nodeCount = tree.path("nodes").size();
    assertThat(retained).isEqualTo(nodeCount);

    var nodeIds = tree.path("nodes").findValuesAsText("node_id");
    for (JsonNode ph : tree.path("placeholders")) {
      String targetNodeId = ph.path("target_node_id").asText("");
      if (!targetNodeId.isBlank()) {
        assertThat(nodeIds).contains(targetNodeId);
      }
    }
  }

  private static List<String> nodeIds(JsonNode arr) {
    return arr.findValuesAsText("node_id");
  }

  private static List<String> edgeTuples(JsonNode arr) {
    if (!arr.isArray() || arr.isEmpty()) {
      return List.of();
    }
    java.util.ArrayList<String> out = new java.util.ArrayList<>();
    for (JsonNode e : arr) {
      out.add(
          e.path("from").asText()
              + "|"
              + e.path("to").asText()
              + "|"
              + e.path("type").asText());
    }
    return out;
  }
}

