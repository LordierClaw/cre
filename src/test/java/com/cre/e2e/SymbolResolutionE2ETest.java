package com.cre.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cre.core.exception.SymbolNotFoundException;
import com.cre.core.service.ContextOptions;
import com.cre.core.service.CreService;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SymbolResolutionE2ETest {

  private static final String TEST_PROJECT_PATH = "/home/hainn/blue/code/cre-test-project";

  @Autowired
  private CreService creService;

  @Test
  void test_resolve_by_fqn() throws Exception {
    Path projectRoot = Path.of(TEST_PROJECT_PATH);
    String symbol = "com.bookstore.service.BookSearchService";

    String resp = creService.getContext(projectRoot, symbol, 0, ContextOptions.defaultOptions());
    assertThat(resp).contains("<BookSearchService>");
  }

  @Test
  void test_resolve_by_simple_name() throws Exception {
    Path projectRoot = Path.of(TEST_PROJECT_PATH);
    String symbol = "BookSearchService";

    String resp = creService.getContext(projectRoot, symbol, 0, ContextOptions.defaultOptions());
    assertThat(resp).contains("<BookSearchService>");
  }

  @Test
  void test_resolve_method_by_symbol() throws Exception {
    Path projectRoot = Path.of(TEST_PROJECT_PATH);
    String symbol = "BookSearchController.searchBooks";

    String resp = creService.getContext(projectRoot, symbol, 0, ContextOptions.defaultOptions());
    assertThat(resp).contains("searchBooks");
  }

  @Test
  void test_unknown_symbol_throws() {
    Path projectRoot = Path.of(TEST_PROJECT_PATH);
    String symbol = "UnknownNonExistentClass";

    assertThrows(SymbolNotFoundException.class, () -> 
        creService.getContext(projectRoot, symbol, 0, ContextOptions.defaultOptions()));
  }
}
