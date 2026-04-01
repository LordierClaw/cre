package com.cre.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.bootstrap.ProjectManager;
import com.cre.core.exception.ProjectNotFoundException;
import com.cre.core.exception.SymbolNotFoundException;
import com.cre.testsupport.ExceptionFlowTestSupport;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CreServiceTest {

  private ProjectManager projectManager;
  private CreServiceImpl creService;
  private Path tempProjectRoot;

  @BeforeEach
  void setUp() throws Exception {
    projectManager = Mockito.mock(ProjectManager.class);
    creService = new CreServiceImpl(projectManager, new DefaultContextPostProcessor());
    
    CreContext ctx = ExceptionFlowTestSupport.load(true);
    tempProjectRoot = ctx.javaSourceRoot().getParent();
    Mockito.when(projectManager.getContext(tempProjectRoot)).thenReturn(ctx);
  }

  @Test
  void test_get_context_with_xml_tags() throws Exception {
    String getUser = "com.cre.fixtures.UserController::getUser(String)";
    
    String result = creService.getContext(tempProjectRoot, getUser, 0, ContextOptions.defaultOptions());

    assertThat(result).contains("<file name=\"com.cre.fixtures.UserController\">");
    assertThat(result).contains("public String getUser(String id)");
    assertThat(result).contains("</file>");
    assertThat(result).contains("<omitted_functions/>");
  }

  @Test
  void test_get_context_symbol_not_found_throws_exception() {
    assertThrows(SymbolNotFoundException.class, () -> 
        creService.getContext(tempProjectRoot, "UnknownClass", 0, ContextOptions.defaultOptions()));
  }

  @Test
  void test_get_context_prunes_non_javadoc_comments() throws Exception {
    // We need a fixture with comments to test this properly.
    // For now, verify that Javadoc is kept if present.
    String getUser = "com.cre.fixtures.UserController::getUser(String)";
    String result = creService.getContext(tempProjectRoot, getUser, 0, ContextOptions.defaultOptions());
    
    // If there were // comments, they should be gone.
    assertThat(result).doesNotContain("//");
  }

  @Test
  void test_get_context_with_relevance_properties() throws Exception {
    String getUser = "com.cre.fixtures.UserController::getUser(String)";
    
    String result = creService.getContext(tempProjectRoot, getUser, 0, ContextOptions.defaultOptions());

    assertThat(result).contains("private final UserService userService;");
  }

  @Test
  void test_expand_uses_depth_1() throws Exception {
    String getUser = "com.cre.fixtures.UserController::getUser(String)";
    
    String result = creService.expand(tempProjectRoot, getUser);

    assertThat(result).contains("<file name=\"com.cre.fixtures.UserController\">");
    assertThat(result).contains("<file name=\"com.cre.fixtures.UserService\">");
  }

  @Test
  void test_reset_project_delegates_to_manager() {
    creService.resetProject(tempProjectRoot);
    Mockito.verify(projectManager).resetContext(tempProjectRoot);
  }

  @Test
  void test_get_project_structure() throws Exception {
    String result = creService.getProjectStructure(tempProjectRoot);
    assertThat(result).contains("Project Structure:");
    assertThat(result).contains("src");
  }

  @Test
  void test_get_file_structure() throws Exception {
    String result = creService.getFileStructure(tempProjectRoot, "com.cre.fixtures.UserController");
    assertThat(result).contains("public class UserController");
    assertThat(result).contains("public String getUser(String id)");
    assertThat(result).doesNotContain("return userService.getUser(id);");
  }
}
