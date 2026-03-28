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
  private final Map<NodeId, List<GraphEdge>> outgoingCallsIndex = new ConcurrentHashMap<>();
  private final Map<NodeId, List<GraphEdge>> incomingCallsIndex = new ConcurrentHashMap<>();
  private final Map<NodeId, List<GraphEdge>> outgoingEdgesIndex = new ConcurrentHashMap<>();

  private volatile boolean springSemanticsPresent = true;
  private volatile boolean springSemanticsComplete = true;
  private volatile String springSemanticsMissingSliceBoundary = "";

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
    outgoingEdgesIndex
        .computeIfAbsent(edge.from(), ignored -> Collections.synchronizedList(new ArrayList<>()))
        .add(edge);
    if (edge.type() == EdgeType.CALLS) {
      outgoingCallsIndex
          .computeIfAbsent(edge.from(), ignored -> Collections.synchronizedList(new ArrayList<>()))
          .add(edge);
      incomingCallsIndex
          .computeIfAbsent(edge.to(), ignored -> Collections.synchronizedList(new ArrayList<>()))
          .add(edge);
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
    List<GraphEdge> list = outgoingCallsIndex.get(from);
    if (list == null || list.isEmpty()) {
      return List.of();
    }
    synchronized (list) {
      return list.stream().sorted(Comparator.comparing(GraphEdge::to)).toList();
    }
  }

  public List<GraphEdge> incomingCalls(NodeId to) {
    List<GraphEdge> list = incomingCallsIndex.get(to);
    if (list == null || list.isEmpty()) {
      return List.of();
    }
    synchronized (list) {
      return list.stream().sorted(Comparator.comparing(GraphEdge::from)).toList();
    }
  }

  public List<GraphEdge> edgesFrom(NodeId from) {
    List<GraphEdge> list = outgoingEdgesIndex.get(from);
    if (list == null || list.isEmpty()) {
      return List.of();
    }
    synchronized (list) {
      return list.stream()
          .sorted(Comparator.comparing(GraphEdge::to).thenComparing(GraphEdge::type))
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

  public void springSemanticsState(boolean present, boolean complete, String missingSliceBoundary) {
    this.springSemanticsPresent = present;
    this.springSemanticsComplete = complete;
    this.springSemanticsMissingSliceBoundary = missingSliceBoundary == null ? "" : missingSliceBoundary;
  }

  public boolean springSemanticsComplete() {
    return springSemanticsComplete;
  }

  public String springSemanticsMissingSliceBoundary() {
    return springSemanticsMissingSliceBoundary;
  }

  public Map<String, Object> evidenceSnapshot() {
    Map<String, Object> m = new TreeMap<>();
    m.put("deterministic_ast", true);
    m.put("spring_semantics", springSemanticsPresent && springSemanticsComplete);
    m.put("heuristic_repair", false);
    m.put("gated_fallback", !springSemanticsComplete);
    return m;
  }
}
