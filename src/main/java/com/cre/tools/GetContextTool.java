package com.cre.tools;

import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.NodeId;
import com.cre.core.graph.model.GraphEdge;
import com.cre.core.graph.model.GraphNode;
import com.cre.core.graph.model.NodeKind;
import com.cre.core.graph.model.EdgeType;
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
    return buildSlice(nodeIdRaw, depth, Integer.MAX_VALUE).response();
  }

  public GetContextResponse expand(String nodeIdRaw) {
    BuiltSlice target = buildSlice(nodeIdRaw, DEFAULT_EXPAND_DEPTH, MAX_EXPAND_NODES);

    NodeId targetId = null;
    try {
      targetId = NodeId.parse(nodeIdRaw);
    } catch (RuntimeException ignored) {
      // fall through: unknown/invalid stays fail-soft via buildSlice's missing_node response
    }
    if (targetId == null || graph.node(targetId) == null) {
      return withExpansionMetadata(target.response(), "target_only_fallback", nodeIdRaw, limitReason(target));
    }

    NodeId anchor = deriveAnchor(targetId);
    BuiltSlice center = buildSlice(anchor.toString(), 0, MAX_EXPAND_NODES);

    GetContextResponse merged = mergeSlices(center.response(), target.response());
    String mode = anchor.equals(targetId) ? "target_only_fallback" : "merged";
    return withExpansionMetadata(merged, mode, anchor.toString(), limitReason(target));
  }

  private record BuiltSlice(GetContextResponse response, boolean depthLimitHit, boolean nodeBudgetHit) {}

  private BuiltSlice buildSlice(String centerNodeIdRaw, int maxDepth, int maxIncludedNodes) {
    NodeId center = NodeId.parse(centerNodeIdRaw);
    if (graph.node(center) == null) {
      return new BuiltSlice(
          new GetContextResponse(
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
                      "unknown_node"))),
          false,
          false);
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
        for (GraphEdge e : graph.outgoingCalls(cur)) {
          NodeId target = e.to();
          if (!dist.containsKey(target)) {
            String key = "depth|" + target;
            if (placeholderKeys.add(key)) {
              placeholders.add(Placeholder.expandCallee(target.toString(), "calls_out"));
              depthLimitHit = true;
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
          String key = "budget|" + next;
          if (placeholderKeys.add(key)) {
            placeholders.add(Placeholder.expandCallee(next.toString(), "node_budget"));
            nodeBudgetHit = true;
          }
          continue;
        }
        dist.put(next, d + 1);
        q.add(next);
      }
    }

    if (maxDepth <= 0) {
      for (GraphEdge e : graph.outgoingCalls(center)) {
        NodeId target = e.to();
        if (!dist.containsKey(target)) {
          String key = "depth|" + target;
          if (placeholderKeys.add(key)) {
            placeholders.add(Placeholder.expandCallee(target.toString(), "calls_out"));
            depthLimitHit = true;
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

    return new BuiltSlice(
        new GetContextResponse(SLICE_VERSION, metadata(), nodes, edgesOut, sliced, placeholders),
        depthLimitHit,
        nodeBudgetHit);
  }

  private Map<String, Object> metadata() {
    Map<String, Object> evidence = new LinkedHashMap<>(graph.evidenceSnapshot());
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("evidence", evidence);
    return meta;
  }

  private String limitReason(BuiltSlice slice) {
    if (slice.nodeBudgetHit()) return "node_budget";
    if (slice.depthLimitHit()) return "depth";
    return "none";
  }

  private NodeId deriveAnchor(NodeId targetId) {
    Map<NodeId, Integer> dist = new HashMap<>();
    ArrayDeque<NodeId> q = new ArrayDeque<>();
    dist.put(targetId, 0);
    q.add(targetId);

    while (!q.isEmpty()) {
      NodeId cur = q.poll();
      int d = dist.get(cur);
      for (GraphEdge e : graph.incomingCalls(cur)) {
        NodeId prev = e.from();
        if (!dist.containsKey(prev)) {
          dist.put(prev, d + 1);
          q.add(prev);
        }
      }
    }

    NodeId best = null;
    int bestDist = Integer.MAX_VALUE;
    for (GraphEdge e : graph.sortedEdges()) {
      if (e.type() != EdgeType.ENTRY_POINT) {
        continue;
      }
      NodeId candidate = e.to();
      Integer cd = dist.get(candidate);
      if (cd == null) {
        continue;
      }
      if (cd < bestDist) {
        bestDist = cd;
        best = candidate;
      } else if (cd == bestDist && best != null) {
        if (candidate.toString().compareTo(best.toString()) < 0) {
          best = candidate;
        }
      }
    }
    return best == null ? targetId : best;
  }

  private GetContextResponse mergeSlices(GetContextResponse a, GetContextResponse b) {
    Map<String, Object> meta = new LinkedHashMap<>(a.metadata());

    Map<String, Map<String, Object>> nodesById = new LinkedHashMap<>();
    for (Map<String, Object> n : a.nodes()) {
      nodesById.put(String.valueOf(n.get("node_id")), n);
    }
    for (Map<String, Object> n : b.nodes()) {
      nodesById.putIfAbsent(String.valueOf(n.get("node_id")), n);
    }

    Set<String> edgeKeys = new LinkedHashSet<>();
    List<Map<String, Object>> edges = new ArrayList<>();
    for (Map<String, Object> e : a.edges()) {
      if (edgeKeys.add(edgeKey(e))) edges.add(e);
    }
    for (Map<String, Object> e : b.edges()) {
      if (edgeKeys.add(edgeKey(e))) edges.add(e);
    }

    Map<String, Map<String, Object>> slicedById = new LinkedHashMap<>();
    for (Map<String, Object> s : a.slicedCode()) {
      slicedById.put(String.valueOf(s.get("node_id")), s);
    }
    for (Map<String, Object> s : b.slicedCode()) {
      slicedById.putIfAbsent(String.valueOf(s.get("node_id")), s);
    }

    Set<String> placeholderKeys = new LinkedHashSet<>();
    List<Placeholder> placeholders = new ArrayList<>();
    Set<String> presentNodeIds = nodesById.keySet();
    for (Placeholder p : a.placeholders()) {
      if (shouldKeepPlaceholder(p, presentNodeIds) && placeholderKeys.add(placeholderKey(p))) {
        placeholders.add(p);
      }
    }
    for (Placeholder p : b.placeholders()) {
      if (shouldKeepPlaceholder(p, presentNodeIds) && placeholderKeys.add(placeholderKey(p))) {
        placeholders.add(p);
      }
    }

    edges.sort(
        (x, y) -> {
          int c = String.valueOf(x.get("from")).compareTo(String.valueOf(y.get("from")));
          if (c != 0) return c;
          c = String.valueOf(x.get("to")).compareTo(String.valueOf(y.get("to")));
          if (c != 0) return c;
          return String.valueOf(x.get("type")).compareTo(String.valueOf(y.get("type")));
        });

    List<Map<String, Object>> nodes =
        nodesById.values().stream()
            .sorted((x, y) -> String.valueOf(x.get("node_id")).compareTo(String.valueOf(y.get("node_id"))))
            .toList();

    List<Map<String, Object>> sliced =
        slicedById.values().stream()
            .sorted((x, y) -> String.valueOf(x.get("node_id")).compareTo(String.valueOf(y.get("node_id"))))
            .toList();

    placeholders.sort(
        (p1, p2) -> {
          int c = String.valueOf(p1.targetNodeId()).compareTo(String.valueOf(p2.targetNodeId()));
          if (c != 0) return c;
          c = String.valueOf(p1.kind()).compareTo(String.valueOf(p2.kind()));
          if (c != 0) return c;
          return String.valueOf(p1.sliceBoundary()).compareTo(String.valueOf(p2.sliceBoundary()));
        });

    return new GetContextResponse(SLICE_VERSION, meta, nodes, edges, sliced, placeholders);
  }

  private static String edgeKey(Map<String, Object> e) {
    return String.valueOf(e.get("from")) + "|" + e.get("to") + "|" + e.get("type");
  }

  private static String placeholderKey(Placeholder p) {
    return p.kind() + "|" + p.targetNodeId() + "|" + p.sliceBoundary();
  }

  private static boolean shouldKeepPlaceholder(Placeholder p, Set<String> presentNodeIds) {
    if ("depth_limit".equals(p.kind()) && p.targetNodeId() != null) {
      return !presentNodeIds.contains(p.targetNodeId());
    }
    return true;
  }

  private GetContextResponse withExpansionMetadata(
      GetContextResponse resp, String expansionMode, String derivedAnchor, String limitReason) {
    Map<String, Object> meta = new LinkedHashMap<>(resp.metadata());
    meta.put("expansion_mode", expansionMode);
    meta.put("derived_anchor", derivedAnchor);
    meta.put("expansion_limit_reason", limitReason);
    return new GetContextResponse(
        resp.sliceVersion(), meta, resp.nodes(), resp.edges(), resp.slicedCode(), resp.placeholders());
  }
}
