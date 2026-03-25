package com.cre.tools;

import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.NodeId;
import com.cre.core.graph.model.GraphEdge;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TraceFlowTool {

  private final GraphEngine graph;

  public TraceFlowTool(GraphEngine graph) {
    this.graph = graph;
  }

  public List<String> execute(String entryMethodNodeId) {
    NodeId start = NodeId.parse(entryMethodNodeId);
    List<String> order = new ArrayList<>();
    dfs(start, order, new LinkedHashSet<>());
    return order;
  }

  private void dfs(NodeId cur, List<String> order, Set<NodeId> seen) {
    if (!seen.add(cur)) {
      return;
    }
    order.add(cur.toString());
    graph.outgoingCalls(cur).stream()
        .sorted(Comparator.comparing(GraphEdge::to))
        .forEach(e -> dfs(e.to(), order, seen));
  }
}
