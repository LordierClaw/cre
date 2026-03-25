package com.cre.testsupport;

import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.NodeId;
import com.cre.core.graph.model.GraphNode;
import com.cre.core.graph.model.NodeKind;

public final class GraphTestSupport {

  private GraphTestSupport() {}

  public static NodeId requireMethod(GraphEngine g, String fqn, String sig) {
    return g.nodes().values().stream()
        .filter(n -> n.kind() == NodeKind.METHOD)
        .map(GraphNode::id)
        .filter(id -> id.fullyQualifiedType().equals(fqn) && id.memberSignature().equals(sig))
        .findFirst()
        .orElseThrow();
  }
}
