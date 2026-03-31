package com.cre.testsupport;

import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.model.GraphNode;
import com.cre.core.graph.model.NodeKind;

public final class GraphTestSupport {

  private GraphTestSupport() {}

  public static String requireMethod(GraphEngine g, String fqn, String sig) {
    String target = fqn + "::" + sig;
    if (g.node(target) != null) return target;
    
    return g.nodes().keySet().stream()
        .filter(id -> id.equals(target))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Method not found: " + target));
  }
}
