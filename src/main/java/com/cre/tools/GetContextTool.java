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
  public static final int DEFAULT_EXPAND_DEPTH = 2;
  public static final int MAX_EXPAND_NODES = 64;

  private final GraphEngine graph;

  public GetContextTool(GraphEngine graph) {
    this.graph = graph;
  }

  public GetContextResponse execute(String nodeIdRaw, int depth) {
    return buildSlice(nodeIdRaw, depth, Integer.MAX_VALUE);
  }

  private GetContextResponse buildSlice(String centerNodeIdRaw, int maxDepth, int maxIncludedNodes) {
    NodeId center = NodeId.parse(centerNodeIdRaw);
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
                  centerNodeIdRaw,
                  "unknown_node")));
    }

    Map<NodeId, Integer> dist = new HashMap<>();
    ArrayDeque<NodeId> q = new ArrayDeque<>();
    dist.put(center, 0);
    q.add(center);

    boolean depthLimitHit = false;
    boolean nodeBudgetHit = false;
    Set<String> placeholderKeys = new LinkedHashSet<>();
    List<Placeholder> placeholders = new ArrayList<>();

    while (!q.isEmpty()) {
      NodeId cur = q.poll();
      int d = dist.get(cur);

      if (d >= maxDepth) {
        depthLimitHit = true;
        for (GraphEdge e : graph.outgoingCalls(cur)) {
          NodeId target = e.to();
          if (!dist.containsKey(target)) {
            String key = "depth|" + target;
            if (placeholderKeys.add(key)) {
              placeholders.add(Placeholder.expandCallee(target.toString(), "calls_out"));
            }
          }
        }
        continue;
      }

      for (GraphEdge e : graph.outgoingCalls(cur)) {
        NodeId next = e.to();
        if (dist.containsKey(next)) {
          continue;
        }
        if (dist.size() >= maxIncludedNodes) {
          nodeBudgetHit = true;
          String key = "budget|" + next;
          if (placeholderKeys.add(key)) {
            placeholders.add(Placeholder.expandCallee(next.toString(), "node_budget"));
          }
          continue;
        }
        dist.put(next, d + 1);
        q.add(next);
      }
    }

    if (maxDepth <= 0) {
      depthLimitHit = true;
      for (GraphEdge e : graph.outgoingCalls(center)) {
        NodeId target = e.to();
        if (!dist.containsKey(target)) {
          String key = "depth|" + target;
          if (placeholderKeys.add(key)) {
            placeholders.add(Placeholder.expandCallee(target.toString(), "calls_out"));
          }
        }
      }
    }

    if (!graph.springSemanticsComplete()) {
      String boundary = graph.springSemanticsMissingSliceBoundary();
      if (boundary == null || boundary.isBlank()) {
        boundary = "missing_spring_mapping";
      }
      placeholders.add(
          new Placeholder(
              "missing_spring_mapping",
              "Spring semantics mapping incomplete",
              "expand",
              null,
              boundary));
    }

    List<NodeId> included = dist.keySet().stream().sorted().toList();

    List<Map<String, Object>> nodes = new ArrayList<>();
    for (NodeId id : included) {
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
      if (dist.containsKey(e.from()) && dist.containsKey(e.to())) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("from", e.from().toString());
        row.put("to", e.to().toString());
        row.put("type", e.type().name());
        edgesOut.add(row);
      }
    }

    List<Map<String, Object>> sliced = new ArrayList<>();
    for (NodeId id : included) {
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

    placeholders.sort(
        (a, b) -> {
          String ak = String.valueOf(a.targetNodeId());
          String bk = String.valueOf(b.targetNodeId());
          int c = ak.compareTo(bk);
          if (c != 0) return c;
          return String.valueOf(a.sliceBoundary()).compareTo(String.valueOf(b.sliceBoundary()));
        });

    return new GetContextResponse(SLICE_VERSION, metadata(), nodes, edgesOut, sliced, placeholders);
  }

  private Map<String, Object> metadata() {
    Map<String, Object> evidence = new LinkedHashMap<>(graph.evidenceSnapshot());
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("evidence", evidence);
    return meta;
  }
}
