package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.graph.NodeId;
import com.cre.testsupport.ExceptionFlowTestSupport;
import com.cre.testsupport.GraphTestSupport;
import org.junit.jupiter.api.Test;

class ExceptionFlowPluginIntegrationTest {

  @Test
  void get_context_includes_catch_invokes_replacement() throws Exception {
    var ctx = ExceptionFlowTestSupport.load(true);
    NodeId entry =
        GraphTestSupport.requireMethod(
            ctx.graph(), "com.cre.fixtures.ExceptionFlowController", "risky(String)");

    // Use a very high score floor to force pruning of the callee at distance 1
    String resp = new GetContextTool(ctx).execute(entry.toString(), 1, 10000);
    
    // In V2, we verify that the call inside catch is replaced by a marker
    assertThat(resp).contains("<ommitted_code");
  }
}
