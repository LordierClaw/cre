package com.cre.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.bootstrap.ProjectManager;
import com.cre.core.graph.NodeId;
import com.cre.testsupport.GraphTestSupport;
import com.cre.tools.GetContextTool;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RealProjectE2ETest {

  private static final String TEST_PROJECT_PATH = "/home/hainn/blue/code/cre-test-project";
  private static final String CRE_PROJECT_PATH = "/home/hainn/blue/code/cre";

  @Test
  void test_real_project_ingestion_caching_and_isolation() throws IOException {
    Path testProjectRoot = Path.of(TEST_PROJECT_PATH);
    Path creProjectRoot = Path.of(CRE_PROJECT_PATH);

    // 1. Index test project
    CreContext testCtx = ProjectManager.getInstance().getContext(testProjectRoot);
    assertThat(testCtx).isNotNull();
    assertThat(testCtx.javaSourceRoot().toString()).contains("cre-test-project");

    // 2. Resolve a known symbol in test project
    NodeId searchMethod = GraphTestSupport.requireMethod(
        testCtx.graph(), 
        "com.bookstore.controller.BookSearchController", 
        "searchBooks(String,String,String)"
    );
    assertThat(searchMethod).isNotNull();

    // 3. Reconstruct context
    String resp = new GetContextTool(testCtx).execute(searchMethod.toString(), 1);
    assertThat(resp).contains("<file origin=");
    assertThat(resp).contains("<BookSearchController.searchBooks>");
    assertThat(resp).contains("<BookSearchController>");
    assertThat(resp).contains("bookSearchService.searchBooks");

    // 4. Verify Caching (same instance)
    CreContext testCtx2 = ProjectManager.getInstance().getContext(testProjectRoot);
    assertThat(testCtx2).isSameAs(testCtx);

    // 5. Load another project (isolation)
    CreContext creCtx = ProjectManager.getInstance().getContext(creProjectRoot);
    assertThat(creCtx).isNotSameAs(testCtx);
    assertThat(creCtx.graph().node(searchMethod)).isNull(); // No pollution

    // 6. Reset project
    ProjectManager.getInstance().resetContext(testProjectRoot);
    CreContext testCtx3 = ProjectManager.getInstance().getContext(testProjectRoot);
    assertThat(testCtx3).isNotSameAs(testCtx);
    assertThat(testCtx3.graph().node(searchMethod)).isNotNull();
  }
}
