package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.NodeId;
import com.cre.core.graph.model.EdgeType;
import com.cre.core.graph.model.GraphEdge;
import com.cre.core.graph.model.GraphNode;
import com.cre.core.graph.model.NodeKind;
import com.cre.testsupport.ExceptionFlowTestSupport;
import com.cre.testsupport.GraphTestSupport;
import org.junit.jupiter.api.Test;

class ExpandToolBoundsTest {

  @Test
  void expand_returns_content() throws Exception {
    CreContext ctx = ExceptionFlowTestSupport.load(true);
    NodeId node = GraphTestSupport.requireMethod(ctx.graph(), "com.cre.fixtures.UserController", "getUser(String)");
    
    String resp = new GetContextTool(ctx).expand(node.toString());

    assertThat(resp).contains("<file origin=");
  }
}
