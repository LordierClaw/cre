package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.graph.NodeId;
import com.cre.testsupport.GraphTestSupport;
import java.util.List;
import org.junit.jupiter.api.Test;

class TraceFlowToolTest {

  @Test
  void trace_containsControllerThenServiceCall() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext();

    NodeId controller =
        GraphTestSupport.requireMethod(
            ctx.graph(), "com.cre.fixtures.UserController", "getUser(String)");
    NodeId service =
        GraphTestSupport.requireMethod(
            ctx.graph(), "com.cre.fixtures.UserService", "getUser(String)");

    List<String> trace = new TraceFlowTool(ctx.graph()).execute(controller.toString());

    int iController = trace.indexOf(controller.toString());
    int iService = trace.indexOf(service.toString());
    assertThat(iController).isGreaterThanOrEqualTo(0);
    assertThat(iService).isGreaterThanOrEqualTo(0);
    assertThat(iController).isLessThan(iService);
  }

  @Test
  void find_impl_returns_implementation_for_fixture_interface() throws Exception {
    CreContext ctx = CreContext.defaultFixtureContext();
    List<String> impls =
        new FindImplementationsTool(ctx.graph()).execute("com.cre.fixtures.UserService");
    assertThat(impls).isNotEmpty();
  }
}
