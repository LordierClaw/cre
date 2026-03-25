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
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContextPruningPolicyTest {

  @Test
  void topk_floor_and_center_protection_are_enforced() {
    GraphEngine g = new GraphEngine();
    NodeId center = new NodeId("t.P", "center()", "mem");
    NodeId high = new NodeId("t.P", "high()", "mem");
    NodeId low = new NodeId("t.P", "low()", "mem");
    g.addNode(new GraphNode(center, NodeKind.METHOD, "center", "center"));
    g.addNode(new GraphNode(high, NodeKind.METHOD, "high", "high"));
    g.addNode(new GraphNode(low, NodeKind.METHOD, "low", "low"));
    g.addEdge(new GraphEdge(center, high, EdgeType.CALLS));

    Map<NodeId, Integer> dist = new HashMap<>();
    dist.put(center, 20); // intentionally low, below floor if not protected
    dist.put(high, 1);
    dist.put(low, 10);

    RankingPruner.Result r = RankingPruner.prune(Set.of(center, high, low), center, dist, g, 2, 1000);

    assertThat(r.retainedSet()).contains(center);
    assertThat(r.retainedSet()).contains(high);
    assertThat(r.retainedSet()).doesNotContain(low);
    assertThat(r.retainedCount() + r.prunedCount()).isEqualTo(r.candidateCount());
    assertThat(r.prunedCount()).isGreaterThanOrEqualTo(1);
  }
}
