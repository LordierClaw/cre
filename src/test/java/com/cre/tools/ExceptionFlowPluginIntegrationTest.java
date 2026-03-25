package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.graph.NodeId;
import com.cre.testsupport.ExceptionFlowTestSupport;
import com.cre.testsupport.GraphTestSupport;
import com.cre.tools.model.GetContextResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ExceptionFlowPluginIntegrationTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void get_context_includes_catch_invokes_edges_and_phase4_metadata() throws Exception {
    var ctx = ExceptionFlowTestSupport.load(true);
    NodeId entry =
        GraphTestSupport.requireMethod(
            ctx.graph(), "com.cre.fixtures.ExceptionFlowController", "risky(String)");

    GetContextResponse resp = new GetContextTool(ctx.graph()).execute(entry.toString(), 1);
    JsonNode tree = mapper.valueToTree(resp);

    assertThat(tree.path("metadata").path("ranking_version").asText()).isEqualTo("cre.rank.v1");
    assertThat(tree.path("metadata").path("prune_policy").asText()).isEqualTo("top_k_floor");
    assertThat(tree.path("metadata").path("retained_count").asInt()).isGreaterThan(0);
    assertThat(tree.path("metadata").path("pruned_count").asInt()).isGreaterThanOrEqualTo(0);
    assertThat(tree.path("metadata").path("score_components_used").isArray()).isTrue();

    boolean hasCatchInvokes = false;
    for (JsonNode e : tree.path("edges")) {
      if ("CATCH_INVOKES".equals(e.path("type").asText())) {
        hasCatchInvokes = true;
        break;
      }
    }
    assertThat(hasCatchInvokes).isTrue();
  }
}
