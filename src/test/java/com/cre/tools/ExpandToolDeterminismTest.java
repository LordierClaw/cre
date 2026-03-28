package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.graph.NodeId;
import com.cre.testsupport.GraphTestSupport;
import org.junit.jupiter.api.Test;

class ExpandToolDeterminismTest {

  @Test
  void expand_is_deterministic() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(true);
    NodeId target =
        GraphTestSupport.requireMethod(ctx.graph(), "com.cre.fixtures.UserService", "getUser(String)");

    String a = new GetContextTool(ctx).expand(target.toString());
    String b = new GetContextTool(ctx).expand(target.toString());

    assertThat(a).isEqualTo(b);
  }

  @Test
  void expansion_returns_content() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(true);
    NodeId target =
        GraphTestSupport.requireMethod(ctx.graph(), "com.cre.fixtures.UserService", "getUser(String)");

    String expanded = new GetContextTool(ctx).expand(target.toString());
    assertThat(expanded).isNotEmpty();
  }
}
