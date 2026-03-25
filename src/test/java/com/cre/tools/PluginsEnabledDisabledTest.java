package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.graph.NodeId;
import com.cre.testsupport.GraphTestSupport;
import com.cre.tools.model.GetContextResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PluginsEnabledDisabledTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void plugins_enabled_has_spring_semantics_true_and_no_missing_mapping_placeholder() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(true);
    NodeId node =
        GraphTestSupport.requireMethod(
            ctx.graph(), "com.cre.fixtures.UserController", "getUser(String)");

    GetContextResponse resp = new GetContextTool(ctx.graph()).execute(node.toString(), 0);
    JsonNode tree = mapper.valueToTree(resp);

    assertThat(tree.path("metadata").path("evidence").path("spring_semantics").asBoolean()).isTrue();

    boolean hasMissing =
        tree.path("placeholders").findValuesAsText("kind").stream()
            .anyMatch(k -> "missing_spring_mapping".equals(k));
    assertThat(hasMissing).isFalse();
  }

  @Test
  void plugins_disabled_has_spring_semantics_false_and_missing_mapping_placeholder() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(false);
    NodeId node =
        GraphTestSupport.requireMethod(
            ctx.graph(), "com.cre.fixtures.UserController", "getUser(String)");

    GetContextResponse resp = new GetContextTool(ctx.graph()).execute(node.toString(), 0);
    JsonNode tree = mapper.valueToTree(resp);

    assertThat(tree.path("metadata").path("evidence").path("spring_semantics").asBoolean()).isFalse();
    assertThat(tree.path("metadata").path("evidence").path("gated_fallback").asBoolean()).isTrue();

    JsonNode phs = tree.path("placeholders");
    assertThat(phs.isArray()).isTrue();
    assertThat(phs.size()).isGreaterThan(0);

    boolean found = false;
    for (JsonNode ph : phs) {
      if ("missing_spring_mapping".equals(ph.path("kind").asText())) {
        found = true;
        assertThat(ph.path("likely_next_tool").asText()).isEqualTo("expand");
        assertThat(ph.path("slice_boundary").asText()).isNotBlank();
      }
    }
    assertThat(found).isTrue();
  }
}

