package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.graph.NodeId;
import com.cre.testsupport.GraphTestSupport;
import com.cre.tools.model.GetContextResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SpringSemanticsMissingMappingTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void missing_service_stereotype_causes_semantics_incomplete_and_placeholder() throws Exception {
    Path root = Path.of(System.getProperty("user.dir", "."));
    Path javaRoot = root.resolve("src/test/java");

    CreContext ctx =
        CreContext.fromJavaSourceRoot(
            javaRoot,
            true,
            javaRoot.resolve("com/cre/fixtures/MissingService.java"),
            javaRoot.resolve("com/cre/fixtures/MissingServiceImpl.java"),
            javaRoot.resolve("com/cre/fixtures/UserControllerMissingServiceMapping.java"));

    NodeId node =
        GraphTestSupport.requireMethod(
            ctx.graph(), "com.cre.fixtures.UserControllerMissingServiceMapping", "call(String)");

    GetContextResponse resp = new GetContextTool(ctx.graph()).execute(node.toString(), 0);
    JsonNode tree = mapper.valueToTree(resp);

    assertThat(tree.path("metadata").path("evidence").path("spring_semantics").asBoolean()).isFalse();
    assertThat(tree.path("metadata").path("evidence").path("gated_fallback").asBoolean()).isTrue();

    boolean found = false;
    for (JsonNode ph : tree.path("placeholders")) {
      if ("missing_spring_mapping".equals(ph.path("kind").asText())) {
        found = true;
        assertThat(ph.path("likely_next_tool").asText()).isEqualTo("expand");
        assertThat(ph.path("slice_boundary").asText()).isNotBlank();
      }
    }
    assertThat(found).isTrue();
  }
}

