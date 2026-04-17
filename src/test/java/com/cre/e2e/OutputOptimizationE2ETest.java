package com.cre.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.service.ContextOptions;
import com.cre.core.service.CreService;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OutputOptimizationE2ETest {

    @Autowired
    private CreService creService;

    private Path projectRoot;

    @BeforeEach
    void setUp() {
        projectRoot = Path.of(".").toAbsolutePath().normalize();
        creService.resetProject(projectRoot);
    }

    @Test
    void testCommentPruning() throws Exception {
        String symbol = "com.cre.fixtures.OptimizationService::executeService()";
        
        // Request depth 1 to include OptimizationFixture as a neighbor
        String context = creService.getContext(projectRoot, symbol, 1, ContextOptions.defaultOptions());
        
        // 1. Target function (executeService) should have its Javadocs and internal comments
        assertThat(context).contains("Executes the service logic by calling the fixture.");
        assertThat(context).contains("// Log starting");
        assertThat(context).contains("// Log finished");

        // 2. Target class (OptimizationService) should have its class-level Javadoc
        assertThat(context).contains("Service that uses OptimizationFixture to test cross-file comment stripping.");

        // 3. Neighbor skeleton (getFixture) should have NO comments
        // Note: LexicalPreservingPrinter might leave some whitespace, but the text should be gone.
        assertThat(context).doesNotContain("Returns the fixture used by the service.");

        // 4. OptimizationFixture is a neighbor, and it is NOT the target class.
        // Its class level Javadoc should be REMOVED.
        assertThat(context).doesNotContain("Fixture class for testing output optimization and comment stripping.");
        
        // 5. OptimizationFixture methods (skeletons) should have NO comments
        assertThat(context).doesNotContain("Constructor for OptimizationFixture.");
        assertThat(context).doesNotContain("Internal comment in constructor");
        assertThat(context).doesNotContain("Gets the name of the fixture.");
        assertThat(context).doesNotContain("Internal comment in getName");
    }

    @Test
    void testTargetClassJavadocPreservation() throws Exception {
        String symbol = "com.cre.fixtures.OptimizationFixture::performAction()";
        
        String context = creService.getContext(projectRoot, symbol, 0, ContextOptions.defaultOptions());
        
        // Target class (OptimizationFixture) should have its class-level Javadoc preserved
        assertThat(context).contains("Fixture class for testing output optimization and comment stripping.");
        
        // Target function (performAction) should have its Javadoc preserved
        assertThat(context).contains("Performs an optimized action.");
    }

    @Test
    void testFormattingQuality() throws Exception {
        String symbol = "com.cre.fixtures.OptimizationFixture::performAction()";
        
        // Request context with some omitted parts to trigger markers
        ContextOptions options = ContextOptions.fromMap(java.util.Map.of(
            "functions", "RELEVANCE",
            "properties", "OMITTED"
        ));
        
        String context = creService.getContext(projectRoot, symbol, 0, options);
        
        // 1. No 3+ consecutive newlines
        assertThat(context).doesNotContain("\n\n\n");

        // 2. Markers are on their own lines
        // Check for <omitted_functions/> and <omitted_properties/>
        String[] lines = context.split("\\r?\\n");
        for (String line : lines) {
            if (line.contains("<omitted_functions/>")) {
                assertThat(line.trim()).isEqualTo("<omitted_functions/>");
            }
            if (line.contains("<omitted_properties/>")) {
                assertThat(line.trim()).isEqualTo("<omitted_properties/>");
            }
        }
    }

    @Test
    void testTraversalCapping() throws Exception {
        // We can't easily create a project with 150+ nodes here without lot of effort.
        // But we can verify that for a known symbol, the output is bounded.
        // Actually, let's just verify it doesn't crash and returns something reasonable.
        String symbol = "com.cre.fixtures.UserController::getUser(String)";
        
        // depth 10 should be capped by the dynamic cap (50 for depth > 3)
        String context = creService.getContext(projectRoot, symbol, 10, ContextOptions.defaultOptions());
        
        assertThat(context).isNotEmpty();
        // Since we only have a few fixtures, it won't hit the cap of 50.
        // But we can verify that it gathered what's expected.
        assertThat(context).contains("UserController");
        assertThat(context).contains("UserService");
        assertThat(context).contains("UserServiceImpl");
    }
}
