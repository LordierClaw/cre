package com.cre.core.graph;

import com.cre.core.graph.model.EdgeType;
import com.cre.core.graph.model.GraphEdge;
import com.cre.core.graph.model.GraphNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory graph with deterministic ordering for serialization.
 */
public final class GraphEngine {

  private final Map<NodeId, GraphNode> nodes = new ConcurrentHashMap<>();
  private final List<GraphEdge> edges = Collections.synchronizedList(new ArrayList<>());
  private final Map<String, Set<NodeId>> interfaceToImplementors = new ConcurrentHashMap<>();

  public void addNode(GraphNode node) {
    nodes.putIfAbsent(node.id(), node);
  }

  public void addEdge(GraphEdge edge) {
    Objects.requireNonNull(edge.from());
    Objects.requireNonNull(edge.to());
    Objects.requireNonNull(edge.type());
    synchronized (edges) {
      edges.add(edge);
    }
  }

  public GraphNode node(NodeId id) {
    return nodes.get(id);
  }

  public Map<NodeId, GraphNode> nodes() {
    return Map.copyOf(nodes);
  }

  public List<GraphNode> sortedNodes() {
    return nodes.keySet().stream().sorted().map(nodes::get).filter(Objects::nonNull).toList();
  }

  public List<GraphEdge> sortedEdges() {
    synchronized (edges) {
      return edges.stream()
          .sorted(
              Comparator.comparing(GraphEdge::from)
                  .thenComparing(GraphEdge::to)
                  .thenComparing(GraphEdge::type))
          .toList();
    }
  }

  public List<GraphEdge> outgoingCalls(NodeId from) {
    synchronized (edges) {
      return edges.stream()
          .filter(e -> e.type() == EdgeType.CALLS && e.from().equals(from))
          .sorted(Comparator.comparing(GraphEdge::to))
          .toList();
    }
  }

  public void registerImplementation(String interfaceFqn, NodeId implementingTypeId) {
    interfaceToImplementors
        .computeIfAbsent(interfaceFqn, k -> ConcurrentHashMap.newKeySet())
        .add(implementingTypeId);
  }

  public List<NodeId> implementationsOf(String interfaceFqn) {
    Set<NodeId> set = interfaceToImplementors.get(interfaceFqn);
    if (set == null || set.isEmpty()) {
      return List.of();
    }
    return set.stream().sorted().toList();
  }

  public Map<String, Object> evidenceSnapshot() {
    Map<String, Object> m = new TreeMap<>();
    m.put("deterministic_ast", true);
    m.put("spring_semantics", true);
    m.put("heuristic_repair", false);
    m.put("gated_fallback", false);
    return m;
  }
}
