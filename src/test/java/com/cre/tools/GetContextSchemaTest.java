package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.testsupport.GraphTestSupport;
import com.cre.tools.model.GetContextResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GetContextSchemaTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void get_context_json_has_slice_metadata_and_placeholder_fields() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext();
    var node =
        GraphTestSupport.requireMethod(
            ctx.graph(), "com.cre.fixtures.UserController", "getUser(String)");

    GetContextResponse resp = new GetContextTool(ctx.graph()).execute(node.toString(), 0);
    JsonNode tree = mapper.valueToTree(resp);

    assertThat(tree.path("slice_version").asText()).isEqualTo(GetContextTool.SLICE_VERSION);

    JsonNode evidence = tree.path("metadata").path("evidence");
    assertThat(evidence.path("deterministic_ast").asBoolean()).isTrue();
    assertThat(evidence.path("spring_semantics").asBoolean()).isTrue();
    assertThat(evidence.path("heuristic_repair").asBoolean()).isFalse();
    assertThat(evidence.path("gated_fallback").asBoolean()).isFalse();

    assertThat(tree.path("sliced_code").isArray()).isTrue();
    assertThat(tree.path("nodes").isArray()).isTrue();
    assertThat(tree.path("edges").isArray()).isTrue();

    assertThat(tree.path("placeholders").isArray()).isTrue();
    assertThat(tree.path("placeholders").size()).isGreaterThan(0);

    JsonNode ph = tree.path("placeholders").get(0);
    assertThat(ph.path("kind").asText()).isNotBlank();
    assertThat(ph.path("reason").asText()).isNotBlank();
    assertThat(ph.path("likely_next_tool").asText()).isEqualTo("expand");
    assertThat(ph.path("slice_boundary").asText()).isNotBlank();
  }
}
