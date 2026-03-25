package com.cre.core.ast;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.NodeId;
import com.cre.testsupport.GraphTestSupport;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NodeIdStabilityTest {

  @Test
  void sameMethodAcrossTwoParses_yieldsEqualNodeId() throws Exception {
    Path root = Path.of("src/test/java");
    Path file = root.resolve("com/cre/fixtures/UserService.java");

    GraphEngine g1 = new GraphEngine();
    new JavaAstIndexer(g1, root).index(file);

    GraphEngine g2 = new GraphEngine();
    new JavaAstIndexer(g2, root).index(file);

    NodeId a =
        GraphTestSupport.requireMethod(g1, "com.cre.fixtures.UserService", "getUser(String)");
    NodeId b =
        GraphTestSupport.requireMethod(g2, "com.cre.fixtures.UserService", "getUser(String)");

    assertThat(a).isEqualTo(b);
  }
}
