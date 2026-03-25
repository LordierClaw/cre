package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.NodeId;
import com.cre.core.graph.model.EdgeType;
import com.cre.core.graph.model.GraphEdge;
import com.cre.core.graph.model.GraphNode;
import com.cre.core.graph.model.NodeKind;
import com.cre.tools.model.GetContextResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ExpandToolBoundsTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void expand_emits_depth_limit_placeholders_when_truncated_by_depth() throws Exception {
    GraphEngine g = new GraphEngine();

    NodeId n0 = new NodeId("t.Chain", "m0()", "mem");
    NodeId n1 = new NodeId("t.Chain", "m1()", "mem");
    NodeId n2 = new NodeId("t.Chain", "m2()", "mem");
    NodeId n3 = new NodeId("t.Chain", "m3()", "mem");
    NodeId n4 = new NodeId("t.Chain", "m4()", "mem");

    g.addNode(new GraphNode(n0, NodeKind.METHOD, "m0", "m0"));
    g.addNode(new GraphNode(n1, NodeKind.METHOD, "m1", "m1"));
    g.addNode(new GraphNode(n2, NodeKind.METHOD, "m2", "m2"));
    g.addNode(new GraphNode(n3, NodeKind.METHOD, "m3", "m3"));
    g.addNode(new GraphNode(n4, NodeKind.METHOD, "m4", "m4"));

    g.addEdge(new GraphEdge(n0, n1, EdgeType.CALLS));
    g.addEdge(new GraphEdge(n1, n2, EdgeType.CALLS));
    g.addEdge(new GraphEdge(n2, n3, EdgeType.CALLS));
    g.addEdge(new GraphEdge(n3, n4, EdgeType.CALLS));

    GetContextResponse resp = new GetContextTool(g).expand(n0.toString());
    JsonNode tree = mapper.valueToTree(resp);

    boolean hasDepthLimit =
        tree.path("placeholders").findValuesAsText("kind").stream()
            .anyMatch(k -> "depth_limit".equals(k));
    assertThat(hasDepthLimit).isTrue();

    boolean hasExpectedTarget =
        tree.path("placeholders").findValuesAsText("target_node_id").stream()
            .anyMatch(id -> n3.toString().equals(id));
    assertThat(hasExpectedTarget).isTrue();

    assertThat(tree.path("metadata").path("expansion_limit_reason").asText()).isEqualTo("depth");
  }
}

