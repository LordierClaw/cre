package com.cre.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.service.ContextOptions;
import com.cre.core.service.CreService;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ContextOptionsE2ETest {

    private static final String TEST_PROJECT_PATH = "/home/hainn/blue/code/cre-test-project";

    @Autowired
    private CreService creService;

    @Test
    void testOptionsFunctionsFull() throws Exception {
        Path projectRoot = Path.of(TEST_PROJECT_PATH);
        String symbol = "com.bookstore.controller.AdminBookController::createBook(BookRequest)";
        
        // functions: FULL should include all methods in the class
        ContextOptions options = ContextOptions.fromMap(Map.of(
            "functions", "FULL"
        ));
        
        String context = creService.getContext(projectRoot, symbol, 0, options);
        
        assertThat(context).contains("createBook");
        assertThat(context).contains("getBook");
        assertThat(context).contains("getAllBooks");
        assertThat(context).doesNotContain("<omitted_functions/>");
    }

    @Test
    void testOptionsPropertiesFull() throws Exception {
        Path projectRoot = Path.of(TEST_PROJECT_PATH);
        String symbol = "com.bookstore.service.BookService::createBook(BookRequest)";
        
        // properties: FULL should include all fields
        ContextOptions options = ContextOptions.fromMap(Map.of(
            "properties", "FULL"
        ));
        
        String context = creService.getContext(projectRoot, symbol, 0, options);
        
        assertThat(context).contains("private final BookRepository bookRepository");
        assertThat(context).doesNotContain("<omitted_properties/>");
    }

    @Test
    void testImplementationTraversalWithFixtures() throws Exception {
        // Use the current project's root to find fixtures in src/test/java
        Path projectRoot = Path.of(".").toAbsolutePath().normalize();
        String symbol = "com.cre.fixtures.UserService::getUser(String)";
        
        // Ensure project is indexed including test fixtures
        creService.resetProject(projectRoot);
        
        // Depth 1 from Interface method should include Implementation method
        String context = creService.getContext(projectRoot, symbol, 1, ContextOptions.defaultOptions());
        
        assertThat(context).contains("<file name=\"com.cre.fixtures.UserService\">");
        assertThat(context).contains("<file name=\"com.cre.fixtures.UserServiceImpl\">");
        assertThat(context).contains("public String getUser(String id)");
    }
}
