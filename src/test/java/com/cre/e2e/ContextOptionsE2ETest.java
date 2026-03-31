package com.cre.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.bootstrap.ProjectManager;
import com.cre.core.service.ContextOptions;
import com.cre.core.service.CreService;
import com.cre.testsupport.GraphTestSupport;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ContextOptionsE2ETest {

  private static final String TEST_PROJECT_PATH = "/home/hainn/blue/code/cre-test-project";

  @Autowired
  private CreService creService;

  @Test
  void test_relevance_mode_for_imports_and_properties() throws Exception {
    Path projectRoot = Path.of(TEST_PROJECT_PATH);
    String symbol = "com.bookstore.controller.BookSearchController::searchBooks(String,String,String)";

    // imports: relevance, properties: relevance (default)
    ContextOptions opts = new ContextOptions(
        ContextOptions.DefinitionLevel.RELEVANCE,
        ContextOptions.DefinitionLevel.RELEVANCE,
        ContextOptions.DefinitionLevel.OMITTED,
        Set.of()
    );

    String resp = creService.getContext(projectRoot, symbol, 0, opts);

    assertThat(resp).contains("<BookSearchController>");
    // BookSearchController uses BookSearchService
    assertThat(resp).contains("private final BookSearchService bookSearchService;");
    // Should contain the import for BookSearchService
    assertThat(resp).contains("import com.bookstore.service.BookSearchService;");
  }

  @Test
  void test_omitted_mode() throws Exception {
    Path projectRoot = Path.of(TEST_PROJECT_PATH);
    String symbol = "com.bookstore.controller.BookSearchController";

    ContextOptions opts = new ContextOptions(
        ContextOptions.DefinitionLevel.OMITTED,
        ContextOptions.DefinitionLevel.OMITTED,
        ContextOptions.DefinitionLevel.OMITTED,
        Set.of()
    );

    String resp = creService.getContext(projectRoot, symbol, 0, opts);

    assertThat(resp).contains("<BookSearchController>");
    assertThat(resp).contains("<omitted_imports/>");
    assertThat(resp).contains("<omitted_properties/>");
    assertThat(resp).contains("<omitted_functions/>");
    assertThat(resp).doesNotContain("private final BookSearchService bookSearchService;");
  }

  @Test
  void test_full_mode() throws Exception {
    Path projectRoot = Path.of(TEST_PROJECT_PATH);
    String symbol = "com.bookstore.controller.BookSearchController";

    ContextOptions opts = new ContextOptions(
        ContextOptions.DefinitionLevel.FULL,
        ContextOptions.DefinitionLevel.FULL,
        ContextOptions.DefinitionLevel.FULL,
        Set.of()
    );

    String resp = creService.getContext(projectRoot, symbol, 0, opts);

    assertThat(resp).contains("<BookSearchController>");
    assertThat(resp).doesNotContain("<omitted_imports/>");
    assertThat(resp).doesNotContain("<omitted_properties/>");
    assertThat(resp).doesNotContain("<omitted_functions/>");
    assertThat(resp).contains("searchBooks");
  }

  @Test
  void test_expanded_functions_override() throws Exception {
    Path projectRoot = Path.of(TEST_PROJECT_PATH);
    String symbol = "com.bookstore.controller.BookSearchController";

    // Global functions omitted, but specific one expanded
    ContextOptions opts = new ContextOptions(
        ContextOptions.DefinitionLevel.OMITTED,
        ContextOptions.DefinitionLevel.OMITTED,
        ContextOptions.DefinitionLevel.OMITTED,
        Set.of("com.bookstore.controller.BookSearchController.searchBooks")
    );

    String resp = creService.getContext(projectRoot, symbol, 0, opts);

    assertThat(resp).contains("<BookSearchController>");
    assertThat(resp).contains("searchBooks"); // Expanded
  }
}
