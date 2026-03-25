package com.cre.tools;

import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.NodeId;
import java.util.List;

public final class FindImplementationsTool {

  private final GraphEngine graph;

  public FindImplementationsTool(GraphEngine graph) {
    this.graph = graph;
  }

  public List<String> execute(String interfaceFqn) {
    return graph.implementationsOf(interfaceFqn).stream().map(NodeId::toString).toList();
  }
}
