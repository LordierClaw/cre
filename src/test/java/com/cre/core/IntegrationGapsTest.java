package com.cre.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.ProjectManager;
import com.cre.core.service.ContextOptions;
import com.cre.core.service.CreService;
import com.cre.core.service.CreServiceImpl;
import com.cre.core.service.DefaultContextPostProcessor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class IntegrationGapsTest {

    @TempDir
    Path tempDir;

    private CreService creService;

    @BeforeEach
    void setUp() {
        ProjectManager projectManager = new ProjectManager();
        creService = new CreServiceImpl(projectManager, new DefaultContextPostProcessor());
    }

    @Test
    void testRecordOptimization() throws Exception {
        Path recordPath = tempDir.resolve("src/main/java/com/test/UserRecord.java");
        String content = """
            package com.test;
            /**
             * Javadoc for record.
             */
            public record UserRecord(String name, int age) {
                /**
                 * Custom method.
                 */
                public String greet() {
                    return "Hello " + name;
                }
                
                public void other() {}
            }
            """;
        Files.createDirectories(recordPath.getParent());
        Files.writeString(recordPath, content);

        // Index and request context
        ContextOptions options = new ContextOptions(
            ContextOptions.DefinitionLevel.OMITTED,
            ContextOptions.DefinitionLevel.RELEVANCE, // Keep relevant properties/components
            ContextOptions.DefinitionLevel.RELEVANCE, // Keep relevant methods
            Set.of()
        );

        // Request context for greet()
        String context = creService.getContext(tempDir, "com.test.UserRecord::greet()", 0, options);

        assertThat(context).contains("public record UserRecord");
        assertThat(context).contains("Javadoc for record.");
        assertThat(context).contains("public String greet()");
        assertThat(context).contains("return \"Hello \" + name;");
        assertThat(context).doesNotContain("public void other()");
    }

    @Test
    void testFqnSignatureMatching() throws Exception {
        Path classPath = tempDir.resolve("src/main/java/com/test/FqnService.java");
        String content = """
            package com.test;
            import java.util.List;
            public class FqnService {
                public void execute(java.util.List<String> list) {
                    System.out.println(list.size());
                }
            }
            """;
        Files.createDirectories(classPath.getParent());
        Files.writeString(classPath, content);

        ContextOptions options = new ContextOptions(
            ContextOptions.DefinitionLevel.OMITTED,
            ContextOptions.DefinitionLevel.OMITTED,
            ContextOptions.DefinitionLevel.RELEVANCE,
            Set.of()
        );

        // Request context using simple-name signature "execute(List)" 
        // even though it was defined with FQN "java.util.List"
        String context = creService.getContext(tempDir, "com.test.FqnService::execute(List)", 0, options);

        assertThat(context).contains("public void execute(java.util.List<String> list)");
        assertThat(context).contains("System.out.println(list.size());");
    }
}
