package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.graph.model.EdgeType;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpringSemanticsPluginDeterminismTest {

  @Test
  void semantic_edges_are_stable_across_repeated_fixture_context_builds() throws Exception {
    var ctx1 = CreContext.defaultFixtureContext(true);
    var ctx2 = CreContext.defaultFixtureContext(true);

    var edges1 =
        ctx1.graph().sortedEdges().stream()
            .filter(
                e ->
                    e.type() == EdgeType.ENTRY_POINT
                        || e.type() == EdgeType.SERVICE_LAYER
                        || e.type() == EdgeType.DEPENDS_ON)
            .toList();
    var edges2 =
        ctx2.graph().sortedEdges().stream()
            .filter(
                e ->
                    e.type() == EdgeType.ENTRY_POINT
                        || e.type() == EdgeType.SERVICE_LAYER
                        || e.type() == EdgeType.DEPENDS_ON)
            .toList();

    assertThat(edges1).isNotEmpty();
    assertThat(edges2).isNotEmpty();
    assertThat(edges1).isEqualTo(edges2);

    // Sanity: controller depends on service interface in the fixture graph.
    boolean hasDepends =
        edges1.stream()
            .anyMatch(
                e ->
                    e.type() == EdgeType.DEPENDS_ON
                        && e.from().fullyQualifiedType().equals("com.cre.fixtures.UserController")
                        && e.to().fullyQualifiedType().equals("com.cre.fixtures.UserService"));
    assertThat(hasDepends).isTrue();
  }
}

