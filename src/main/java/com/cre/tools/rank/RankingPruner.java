package com.cre.tools.rank;

import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.NodeId;
import com.cre.core.graph.model.EdgeType;
import com.cre.core.graph.model.GraphEdge;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic ranking and pruning for context slices.
 *
 * <p>Defaults: top_k=32 and score_floor=500 (milliscore units).
 *
 * <p>Scoring model (all integer weights, saturating add at MAX_MILLISCORE=2_000_000):
 *
 * <ul>
 *   <li>depth_decay: max(0, 5000 - depth * 600)
 *   <li>edge_type: CALLS +120, ENTRY_POINT +220, SERVICE_LAYER +180, DEPENDS_ON +100
 *   <li>edge_type (exception-flow): CATCH_INVOKES — <b>no</b> incident milliscore (visibility in graph
 *       JSON only; same as unlisted structural edges for {@code aggregateBonuses})
 *   <li>uses_field: +80 per outgoing USES_FIELD (capped at 4 edges)
 *   <li>degree: +20 per outgoing/incoming CALLS edge (capped at 8 total)
 * </ul>
 */
public final class RankingPruner {

  public static final String RANKING_VERSION = "cre.rank.v1";
  public static final String PRUNE_POLICY = "top_k_floor";
  public static final int DEFAULT_TOP_K = 32;
  public static final int DEFAULT_SCORE_FLOOR = 500;
  public static final int MAX_MILLISCORE = 2_000_000;

  private static final List<String> SCORE_COMPONENTS =
      List.of("depth_decay", "edge_type", "uses_field", "degree");

  private RankingPruner() {}

  public record Result(
      List<NodeId> retainedOrdered,
      Set<NodeId> retainedSet,
      Set<NodeId> prunedSet,
      Map<NodeId, Integer> scores,
      int retainedCount,
      int prunedCount,
      int candidateCount,
      List<String> scoreComponentsUsed) {}

  public static Result prune(
      Collection<NodeId> candidates,
      NodeId center,
      Map<NodeId, Integer> dist,
      GraphEngine graph,
      int topK,
      int scoreFloor) {
    Set<NodeId> candidateSet = new LinkedHashSet<>(candidates);
    if (candidateSet.isEmpty()) {
      return new Result(
          List.of(), Set.of(), Set.of(), Map.of(), 0, 0, 0, List.copyOf(SCORE_COMPONENTS));
    }

    Aggregates aggregates = aggregateBonuses(candidateSet, graph);
    Map<NodeId, Integer> scores = new HashMap<>();
    for (NodeId id : candidateSet) {
      int score = 0;
      score = saturatingAdd(score, depthContribution(dist.getOrDefault(id, Integer.MAX_VALUE)));
      score = saturatingAdd(score, aggregates.incidentBonus().getOrDefault(id, 0));
      score = saturatingAdd(score, usesFieldContribution(aggregates.usesFieldOut().getOrDefault(id, 0)));
      score =
          saturatingAdd(
              score,
              degreeContribution(
                  aggregates.outCalls().getOrDefault(id, 0), aggregates.inCalls().getOrDefault(id, 0)));
      scores.put(id, score);
    }

    List<NodeId> ranked = new ArrayList<>(candidateSet);
    ranked.sort(
        (a, b) -> {
          int cmp = Integer.compare(scores.getOrDefault(b, 0), scores.getOrDefault(a, 0));
          if (cmp != 0) {
            return cmp;
          }
          return a.toString().compareTo(b.toString());
        });

    LinkedHashSet<NodeId> retained = new LinkedHashSet<>();
    if (center != null && candidateSet.contains(center)) {
      retained.add(center);
    }
    for (NodeId id : ranked) {
      if (retained.contains(id)) {
        continue;
      }
      if (scores.getOrDefault(id, 0) >= scoreFloor && retained.size() < topK) {
        retained.add(id);
      }
    }

    LinkedHashSet<NodeId> pruned = new LinkedHashSet<>(candidateSet);
    pruned.removeAll(retained);

    return new Result(
        List.copyOf(retained),
        Set.copyOf(retained),
        Set.copyOf(pruned),
        Map.copyOf(scores),
        retained.size(),
        pruned.size(),
        candidateSet.size(),
        List.copyOf(SCORE_COMPONENTS));
  }

  private record Aggregates(
      Map<NodeId, Integer> incidentBonus,
      Map<NodeId, Integer> usesFieldOut,
      Map<NodeId, Integer> outCalls,
      Map<NodeId, Integer> inCalls) {}

  private static Aggregates aggregateBonuses(Set<NodeId> candidates, GraphEngine graph) {
    Map<NodeId, Integer> incidentBonus = new HashMap<>();
    Map<NodeId, Integer> usesFieldOut = new HashMap<>();
    Map<NodeId, Integer> outCalls = new HashMap<>();
    Map<NodeId, Integer> inCalls = new HashMap<>();

    // Single deterministic edge pass: O(E)
    for (GraphEdge edge : graph.sortedEdges()) {
      NodeId from = edge.from();
      NodeId to = edge.to();
      boolean fromIn = candidates.contains(from);
      boolean toIn = candidates.contains(to);
      if (!fromIn && !toIn) {
        continue;
      }

      if (edge.type() == EdgeType.CALLS) {
        if (fromIn) {
          outCalls.put(from, outCalls.getOrDefault(from, 0) + 1);
          incidentBonus.put(from, saturatingAdd(incidentBonus.getOrDefault(from, 0), 120));
        }
        if (toIn) {
          inCalls.put(to, inCalls.getOrDefault(to, 0) + 1);
          incidentBonus.put(to, saturatingAdd(incidentBonus.getOrDefault(to, 0), 120));
        }
      } else if (edge.type() == EdgeType.ENTRY_POINT) {
        if (fromIn) {
          incidentBonus.put(from, saturatingAdd(incidentBonus.getOrDefault(from, 0), 220));
        }
        if (toIn) {
          incidentBonus.put(to, saturatingAdd(incidentBonus.getOrDefault(to, 0), 220));
        }
      } else if (edge.type() == EdgeType.SERVICE_LAYER) {
        if (fromIn) {
          incidentBonus.put(from, saturatingAdd(incidentBonus.getOrDefault(from, 0), 180));
        }
        if (toIn) {
          incidentBonus.put(to, saturatingAdd(incidentBonus.getOrDefault(to, 0), 180));
        }
      } else if (edge.type() == EdgeType.DEPENDS_ON) {
        if (fromIn) {
          incidentBonus.put(from, saturatingAdd(incidentBonus.getOrDefault(from, 0), 100));
        }
        if (toIn) {
          incidentBonus.put(to, saturatingAdd(incidentBonus.getOrDefault(to, 0), 100));
        }
      } else if (edge.type() == EdgeType.USES_FIELD && fromIn) {
        usesFieldOut.put(from, usesFieldOut.getOrDefault(from, 0) + 1);
      } else if (edge.type() == EdgeType.CATCH_INVOKES) {
        // No incident bonus for CATCH_INVOKES — documented in class Javadoc (visibility-only for ranking).
      }
    }
    return new Aggregates(incidentBonus, usesFieldOut, outCalls, inCalls);
  }

  private static int depthContribution(int depth) {
    if (depth == Integer.MAX_VALUE) {
      return 0;
    }
    int v = 5000 - (depth * 600);
    return Math.max(0, v);
  }

  private static int usesFieldContribution(int usesFieldCount) {
    int capped = Math.min(usesFieldCount, 4);
    return capped * 80;
  }

  private static int degreeContribution(int outCalls, int inCalls) {
    int capped = Math.min(8, outCalls + inCalls);
    return capped * 20;
  }

  private static int saturatingAdd(int current, int delta) {
    if (delta <= 0) {
      return current;
    }
    if (current >= MAX_MILLISCORE - delta) {
      return MAX_MILLISCORE;
    }
    return current + delta;
  }
}
