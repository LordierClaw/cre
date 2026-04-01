package com.cre.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.bootstrap.ProjectManager;
import com.cre.core.service.CreService;
import com.cre.testsupport.GraphTestSupport;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RealProjectE2ETest {

  private static final String TEST_PROJECT_PATH = "/home/hainn/blue/code/cre-test-project";
  private static final String CRE_PROJECT_PATH = "/home/hainn/blue/code/cre";

  @Autowired
  private ProjectManager projectManager;

  @Autowired
  private CreService creService;

  @Test
  void test_real_project_ingestion_caching_and_isolation() throws Exception {
    Path testProjectRoot = Path.of(TEST_PROJECT_PATH);
    Path creProjectRoot = Path.of(CRE_PROJECT_PATH);

    // 1. Index test project
    CreContext testCtx = projectManager.getContext(testProjectRoot);
    assertThat(testCtx).isNotNull();
    assertThat(testCtx.projectRoot().toString()).contains("cre-test-project");

    // 2. Resolve a known symbol in test project
    String searchMethod = GraphTestSupport.requireMethod(
        testCtx.graph(), 
        "com.bookstore.controller.BookSearchController", 
        "searchBooks(String,String,String)"
    );
    assertThat(searchMethod).isNotNull();

    // 3. Reconstruct context via CreService
    String resp = creService.getContext(testProjectRoot, searchMethod, 1, com.cre.core.service.ContextOptions.defaultOptions());
    assertThat(resp).contains("<file name=\"com.bookstore.controller.BookSearchController\">");
    assertThat(resp).contains("bookSearchService.searchBooks");

    // 4. Verify Caching (same instance)
    CreContext testCtx2 = projectManager.getContext(testProjectRoot);
    assertThat(testCtx2).isSameAs(testCtx);

    // 5. Load another project (isolation)
    CreContext creCtx = projectManager.getContext(creProjectRoot);
    assertThat(creCtx).isNotSameAs(testCtx);
    assertThat(creCtx.graph().node(searchMethod)).isNull(); // No pollution

    // 6. Reset project
    creService.resetProject(testProjectRoot);
    CreContext testCtx3 = projectManager.getContext(testProjectRoot);
    assertThat(testCtx3).isNotSameAs(testCtx);
    assertThat(testCtx3.graph().node(searchMethod)).isNotNull();
  }
}
