package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.graph.NodeId;
import com.cre.testsupport.ExceptionFlowTestSupport;
import com.cre.testsupport.GraphTestSupport;
import org.junit.jupiter.api.Test;

class PluginsEnabledDisabledTest {

  @Test
  void plugins_enabled_returns_content() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(true);
    NodeId node =
        GraphTestSupport.requireMethod(
            ctx.graph(), "com.cre.fixtures.UserController", "getUser(String)");

    String resp = new GetContextTool(ctx).execute(node.toString(), 0);
    assertThat(resp).isNotEmpty();
  }

  @Test
  void plugins_disabled_returns_content() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(false);
    NodeId node =
        GraphTestSupport.requireMethod(
            ctx.graph(), "com.cre.fixtures.UserController", "getUser(String)");

    String resp = new GetContextTool(ctx).execute(node.toString(), 0);
    assertThat(resp).isNotEmpty();
  }

  @Test
  void plugins_disabled_has_no_catch_invokes_in_code() throws Exception {
    CreContext ctx = ExceptionFlowTestSupport.load(false);
    NodeId node =
        GraphTestSupport.requireMethod(
            ctx.graph(), "com.cre.fixtures.ExceptionFlowController", "risky(String)");

    String resp = new GetContextTool(ctx).execute(node.toString(), 0);
    assertThat(resp).doesNotContain("<ommitted_code");
  }
}
