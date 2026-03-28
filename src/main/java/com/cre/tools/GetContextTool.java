package com.cre.tools;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.NodeId;
import com.cre.core.graph.model.GraphEdge;
import com.cre.core.graph.model.EdgeType;
import com.cre.tools.rank.RankingPruner;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class GetContextTool {

  public static final int DEFAULT_EXPAND_DEPTH = 2;
  public static final int MAX_EXPAND_NODES = 64;

  private final CreContext context;
  private final GraphEngine graph;

  public GetContextTool(CreContext context) {
    this.context = context;
    this.graph = context.graph();
  }

  public String execute(String nodeIdRaw, int depth) {
    return execute(nodeIdRaw, depth, RankingPruner.DEFAULT_SCORE_FLOOR);
  }

  public String execute(String nodeIdRaw, int depth, int scoreFloor) {
    NodeId startNode = resolveNodeId(nodeIdRaw);
    GatherResult gathered = gatherNodes(startNode, depth);
    
    RankingPruner.Result pruned = RankingPruner.prune(
        gathered.nodes, startNode, gathered.distances, graph, RankingPruner.DEFAULT_TOP_K, scoreFloor);

    IntegratedViewBuilder builder = new IntegratedViewBuilder();
    return builder.build(
        pruned.retainedSet(), pruned.prunedSet(), graph, context.javaSourceRoot(), startNode);
  }

  public String expand(String nodeIdRaw) {
    NodeId anchor = resolveNodeId(nodeIdRaw);
    GatherResult gathered = gatherNodes(anchor, 1); // 1-hop expansion
    
    RankingPruner.Result pruned = RankingPruner.prune(
        gathered.nodes, anchor, gathered.distances, graph, RankingPruner.DEFAULT_TOP_K, RankingPruner.DEFAULT_SCORE_FLOOR);

    IntegratedViewBuilder builder = new IntegratedViewBuilder();
    return builder.build(
        pruned.retainedSet(), pruned.prunedSet(), graph, context.javaSourceRoot(), anchor);
  }

  private NodeId resolveNodeId(String raw) {
    if (raw.contains("::")) {
      return NodeId.parse(raw);
    }

    // Symbol resolution logic: match simpleName.methodName, fqn.methodName, or ClassName
    List<NodeId> matches = graph.nodes().keySet().stream()
        .filter(id -> matchesSymbol(id, raw))
        .toList();

    if (matches.isEmpty()) {
      throw new IllegalArgumentException("No node found for symbol: " + raw);
    }
    if (matches.size() > 1) {
      // Tie-breaker: prefer exact FQN match if available
      return matches.stream()
          .filter(id -> id.fullyQualifiedType().equals(raw) || 
                        (id.fullyQualifiedType() + "." + id.memberSignature().split("\\(")[0]).equals(raw))
          .findFirst()
          .orElse(matches.get(0));
    }
    return matches.get(0);
  }

  private boolean matchesSymbol(NodeId id, String symbol) {
    String fqn = id.fullyQualifiedType();
    String simpleName = fqn.substring(fqn.lastIndexOf('.') + 1);
    String signature = id.memberSignature();
    String methodName = signature.contains("(") ? signature.substring(0, signature.indexOf('(')) : signature;

    // Match Class.method
    if (symbol.equals(simpleName + "." + methodName)) return true;
    // Match FQN.method
    if (symbol.equals(fqn + "." + methodName)) return true;
    // Match Class
    if (symbol.equals(simpleName) && signature.equals("<type>")) return true;
    // Match FQN
    if (symbol.equals(fqn) && signature.equals("<type>")) return true;

    return false;
  }

  private record GatherResult(Set<NodeId> nodes, Map<NodeId, Integer> distances) {}

  private GatherResult gatherNodes(NodeId start, int maxDepth) {
    Set<NodeId> visited = new LinkedHashSet<>();
    Map<NodeId, Integer> distances = new HashMap<>();
    Queue<NodeId> queue = new ArrayDeque<>();

    if (graph.node(start) != null) {
      queue.add(start);
      visited.add(start);
      distances.put(start, 0);
    }

    while (!queue.isEmpty()) {
      NodeId current = queue.poll();
      int d = distances.get(current);

      if (d < maxDepth && visited.size() < MAX_EXPAND_NODES) {
        for (GraphEdge edge : graph.edgesFrom(current)) {
          if (edge.type() == EdgeType.CATCH_INVOKES) {
            continue; // Skip exception flow edges during BFS traversal
          }
          NodeId to = edge.to();
          if (!visited.contains(to)) {
            visited.add(to);
            distances.put(to, d + 1);
            queue.add(to);
            if (visited.size() >= MAX_EXPAND_NODES) {
              break;
            }
          }
        }
      }
    }

    return new GatherResult(visited, distances);
  }
}
