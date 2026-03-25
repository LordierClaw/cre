package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.graph.model.EdgeType;
import com.cre.testsupport.ExceptionFlowTestSupport;
import org.junit.jupiter.api.Test;

class ExceptionFlowPluginDeterminismTest {

  @Test
  void catch_invokes_edges_are_stable_across_repeated_fixture_context_builds() throws Exception {
    var ctx1 = ExceptionFlowTestSupport.load(true);
    var ctx2 = ExceptionFlowTestSupport.load(true);

    var edges1 =
        ctx1.graph().sortedEdges().stream().filter(e -> e.type() == EdgeType.CATCH_INVOKES).toList();
    var edges2 =
        ctx2.graph().sortedEdges().stream().filter(e -> e.type() == EdgeType.CATCH_INVOKES).toList();

    assertThat(edges1).isNotEmpty();
    assertThat(edges2).isNotEmpty();
    assertThat(edges1).isEqualTo(edges2);

    assertThat(edges1.stream().anyMatch(e -> e.type() == EdgeType.CATCH_INVOKES)).isTrue();
  }
}
