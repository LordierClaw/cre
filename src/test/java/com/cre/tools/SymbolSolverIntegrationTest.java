package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.ast.JavaAstIndexer;
import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.model.EdgeType;
import com.cre.core.graph.model.GraphEdge;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SymbolSolverIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testWildcardImportResolution() throws Exception {
        Path projectRoot = Path.of(System.getProperty("user.dir", "."));
        GraphEngine graph = new GraphEngine();
        JavaAstIndexer indexer = new JavaAstIndexer(graph, projectRoot);
        
        // Target file: src/test/java/com/cre/fixtures/WildcardImportController.java
        Path path = projectRoot.resolve("src/test/java/com/cre/fixtures/WildcardImportController.java");
        indexer.index(path);
        
        String methodId = "com.cre.fixtures.WildcardImportController::list()";
        assertThat(graph.node(methodId)).isNotNull();
        
        // Verify dependency on java.util.List (resolved via wildcard import)
        // Note: JavaSymbolSolver resolves java.util.List as java.util.List
        assertThat(graph.edgesFrom(methodId))
            .filteredOn(e -> e.type() == EdgeType.DEPENDS_ON)
            .extracting(GraphEdge::to)
            .contains("java.util.List");
    }
}
