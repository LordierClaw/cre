package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.ProjectManager;
import com.cre.core.service.CreServiceImpl;
import com.cre.testsupport.ExceptionFlowTestSupport;
import com.cre.testsupport.GraphTestSupport;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ExceptionFlowPluginIntegrationTest {

  @Test
  void get_context_includes_callees_at_depth_1() throws Exception {
    var ctx = ExceptionFlowTestSupport.load(true);
    String entry =
        GraphTestSupport.requireMethod(
            ctx.graph(), "com.cre.fixtures.ExceptionFlowController", "risky(String)");

    ProjectManager pm = Mockito.mock(ProjectManager.class);
    Path root = ctx.javaSourceRoot().getParent();
    Mockito.when(pm.getContext(root)).thenReturn(ctx);
    CreServiceImpl creService = new CreServiceImpl(pm, new com.cre.core.service.DefaultContextPostProcessor());

    String resp = creService.getContext(root, entry, 1, com.cre.core.service.ContextOptions.defaultOptions());
    
    // In Phase 12, depth 1 should include the classes called by risky()
    assertThat(resp).contains("<ExceptionFlowController>");
    assertThat(resp).contains("<UserService>");
  }
}
