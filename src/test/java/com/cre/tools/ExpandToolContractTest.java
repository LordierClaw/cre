package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.graph.NodeId;
import com.cre.testsupport.GraphTestSupport;
import org.junit.jupiter.api.Test;

class ExpandToolContractTest {

  @Test
  void expand_has_xml_format() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(true);
    NodeId target =
        GraphTestSupport.requireMethod(ctx.graph(), "com.cre.fixtures.UserService", "getUser(String)");

    String resp = new GetContextTool(ctx).expand(target.toString());

    assertThat(resp).contains("<file origin=");
  }

  @Test
  void expand_unknown_node_is_fail_soft() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(true);
    String resp = new GetContextTool(ctx).expand("unknown::node::id");
    assertThat(resp).isEmpty();
  }

  @Test
  void expand_center_node_included() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext(true);
    NodeId target =
        GraphTestSupport.requireMethod(ctx.graph(), "com.cre.fixtures.UserService", "getUser(String)");

    String expanded = new GetContextTool(ctx).expand(target.toString());
    assertThat(expanded).contains("UserService.java");
  }
}
