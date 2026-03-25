package com.cre.tools;

import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.NodeId;
import com.cre.core.graph.model.GraphEdge;
import com.cre.core.graph.model.GraphNode;
import com.cre.core.graph.model.NodeKind;
import com.cre.tools.model.GetContextResponse;
import com.cre.tools.model.Placeholder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GetContextTool {

  public static final String SLICE_VERSION = "cre.slice.v1";

  private final GraphEngine graph;

  public GetContextTool(GraphEngine graph) {
    this.graph = graph;
  }

  public GetContextResponse execute(String nodeIdRaw, int depth) {
    NodeId center = NodeId.parse(nodeIdRaw);
    if (graph.node(center) == null) {
      return new GetContextResponse(
          SLICE_VERSION,
          metadata(),
          List.of(),
          List.of(),
          List.of(),
          List.of(
              new Placeholder(
                  "missing_node",
                  "Unknown node id",
                  "expand",
                  nodeIdRaw,
                  "unknown_node")));
    }

    Set<NodeId> included = new LinkedHashSet<>();
    Map<NodeId, Integer> dist = new HashMap<>();

    if (depth <= 0) {
      included.add(center);
    } else {
      dist.put(center, 0);
      ArrayDeque<NodeId> q = new ArrayDeque<>();
      q.add(center);
      while (!q.isEmpty()) {
        NodeId cur = q.poll();
        int d = dist.get(cur);
        if (d >= depth) {
          continue;
        }
        for (GraphEdge e : graph.outgoingCalls(cur)) {
          NodeId next = e.to();
          if (!dist.containsKey(next)) {
            dist.put(next, d + 1);
            q.add(next);
          }
        }
      }
      included.addAll(dist.keySet());
    }

    List<Placeholder> placeholders = new ArrayList<>();
    if (depth <= 0) {
      for (GraphEdge e : graph.outgoingCalls(center)) {
        placeholders.add(Placeholder.expandCallee(e.to().toString(), "calls_out"));
      }
    }

    List<Map<String, Object>> nodes = new ArrayList<>();
    for (NodeId id : included.stream().sorted().toList()) {
      GraphNode gn = graph.node(id);
      if (gn == null) {
        continue;
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("node_id", id.toString());
      row.put("kind", gn.kind().name());
      row.put("fqn", id.fullyQualifiedType());
      row.put("member", id.memberSignature());
      nodes.add(row);
    }

    List<Map<String, Object>> edgesOut = new ArrayList<>();
    for (GraphEdge e : graph.sortedEdges()) {
      if (included.contains(e.from()) && included.contains(e.to())) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("from", e.from().toString());
        row.put("to", e.to().toString());
        row.put("type", e.type().name());
        edgesOut.add(row);
      }
    }

    List<Map<String, Object>> sliced = new ArrayList<>();
    for (NodeId id : included.stream().sorted().toList()) {
      GraphNode gn = graph.node(id);
      if (gn == null || gn.kind() == NodeKind.FIELD) {
        continue;
      }
      Map<String, Object> seg = new LinkedHashMap<>();
      seg.put("node_id", id.toString());
      seg.put("language", "java");
      seg.put("text", gn.snippet());
      sliced.add(seg);
    }

    return new GetContextResponse(SLICE_VERSION, metadata(), nodes, edgesOut, sliced, placeholders);
  }

  private Map<String, Object> metadata() {
    Map<String, Object> evidence = new LinkedHashMap<>(graph.evidenceSnapshot());
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("evidence", evidence);
    return meta;
  }
}
