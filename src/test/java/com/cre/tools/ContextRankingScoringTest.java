package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.NodeId;
import com.cre.core.graph.model.EdgeType;
import com.cre.core.graph.model.GraphEdge;
import com.cre.core.graph.model.GraphNode;
import com.cre.core.graph.model.NodeKind;
import com.cre.tools.rank.RankingPruner;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContextRankingScoringTest {

  @Test
  void tie_breaks_by_lexicographic_node_id_when_scores_equal() {
    GraphEngine g = new GraphEngine();
    NodeId center = new NodeId("t.C", "m()", "mem");
    NodeId a = new NodeId("t.C", "a()", "mem");
    NodeId b = new NodeId("t.C", "b()", "mem");
    g.addNode(new GraphNode(center, NodeKind.METHOD, "m", "m"));
    g.addNode(new GraphNode(a, NodeKind.METHOD, "a", "a"));
    g.addNode(new GraphNode(b, NodeKind.METHOD, "b", "b"));
    g.addEdge(new GraphEdge(center, a, EdgeType.CALLS));
    g.addEdge(new GraphEdge(center, b, EdgeType.CALLS));

    Map<NodeId, Integer> dist = new HashMap<>();
    dist.put(center, 0);
    dist.put(a, 1);
    dist.put(b, 1);

    RankingPruner.Result r =
        RankingPruner.prune(Set.of(center, a, b), center, dist, g, 3, RankingPruner.DEFAULT_SCORE_FLOOR);

    List<NodeId> ordered = r.retainedOrdered();
    assertThat(ordered).containsExactly(center, a, b);
  }

  @Test
  void scoring_is_deterministic_and_saturates_without_overflow() {
    GraphEngine g = new GraphEngine();
    NodeId center = new NodeId("t.H", "center()", "mem");
    NodeId heavy = new NodeId("t.H", "heavy()", "mem");
    g.addNode(new GraphNode(center, NodeKind.METHOD, "center", "center"));
    g.addNode(new GraphNode(heavy, NodeKind.METHOD, "heavy", "heavy"));
    g.addEdge(new GraphEdge(center, heavy, EdgeType.CALLS));
    for (int i = 0; i < 20000; i++) {
      g.addEdge(new GraphEdge(heavy, center, EdgeType.ENTRY_POINT));
    }

    Map<NodeId, Integer> dist = Map.of(center, 0, heavy, 1);
    RankingPruner.Result a = RankingPruner.prune(Set.of(center, heavy), center, dist, g, 2, 0);
    RankingPruner.Result b = RankingPruner.prune(Set.of(center, heavy), center, dist, g, 2, 0);

    int heavyA = a.scores().getOrDefault(heavy, 0);
    int heavyB = b.scores().getOrDefault(heavy, 0);
    assertThat(heavyA).isEqualTo(heavyB);
    assertThat(heavyA).isEqualTo(RankingPruner.MAX_MILLISCORE);
  }
}
