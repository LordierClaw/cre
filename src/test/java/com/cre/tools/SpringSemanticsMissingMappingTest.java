package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.graph.NodeId;
import com.cre.testsupport.GraphTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SpringSemanticsMissingMappingTest {

  @Test
  void missing_service_stereotype_still_returns_context() throws Exception {
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

    String resp = new GetContextTool(ctx).execute(node.toString(), 0);
    assertThat(resp).isNotEmpty();
  }
}
