package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.testsupport.GraphTestSupport;
import org.junit.jupiter.api.Test;

class GetContextSchemaTest {

  @Test
  void get_context_returns_xml_format() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext();
    var node =
        GraphTestSupport.requireMethod(
            ctx.graph(), "com.cre.fixtures.UserController", "getUser(String)");

    String resp = new GetContextTool(ctx).execute(node.toString(), 0);

    assertThat(resp).contains("<file origin=");
    assertThat(resp).contains("<UserController.getUser>");
  }
}
