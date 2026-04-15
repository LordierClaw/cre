package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.ast.JavaAstIndexer;
import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.model.EdgeType;
import com.cre.core.graph.model.GraphEdge;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class OverloadResolutionTest {

    @Test
    void testOverloadResolution() throws Exception {
        Path projectRoot = Path.of(System.getProperty("user.dir", "."));
        GraphEngine graph = new GraphEngine();
        JavaAstIndexer indexer = new JavaAstIndexer(graph, projectRoot);
        
        indexer.index(projectRoot.resolve("src/test/java/com/cre/fixtures/OverloadService.java"));
        indexer.index(projectRoot.resolve("src/test/java/com/cre/fixtures/OverloadController.java"));
        
        String fromId = "com.cre.fixtures.OverloadController::test()";
        assertThat(graph.node(fromId)).isNotNull();
        
        var calls = graph.edgesFrom(fromId).stream()
            .filter(e -> e.type() == EdgeType.CALLS)
            .map(GraphEdge::to)
            .toList();
            
        assertThat(calls).contains("com.cre.fixtures.OverloadService::process(String)");
        assertThat(calls).contains("com.cre.fixtures.OverloadService::process(int)");
        assertThat(calls).contains("com.cre.fixtures.OverloadService::process(Object)");
    }

    @Test
    void testVarTypeResolution() throws Exception {
        Path projectRoot = Path.of(System.getProperty("user.dir", "."));
        GraphEngine graph = new GraphEngine();
        JavaAstIndexer indexer = new JavaAstIndexer(graph, projectRoot);
        
        indexer.index(projectRoot.resolve("src/test/java/com/cre/fixtures/OverloadService.java"));
        indexer.index(projectRoot.resolve("src/test/java/com/cre/fixtures/OverloadController.java"));
        
        String fromId = "com.cre.fixtures.OverloadController::testVar()";
        
        var calls = graph.edgesFrom(fromId).stream()
            .filter(e -> e.type() == EdgeType.CALLS)
            .map(GraphEdge::to)
            .toList();
            
        assertThat(calls).contains("com.cre.fixtures.OverloadService::process(String)");
    }

    @Test
    void testComplexGenericsResolution() throws Exception {
        Path projectRoot = Path.of(System.getProperty("user.dir", "."));
        GraphEngine graph = new GraphEngine();
        JavaAstIndexer indexer = new JavaAstIndexer(graph, projectRoot);
        
        indexer.index(projectRoot.resolve("src/test/java/com/cre/fixtures/GenericService.java"));
        indexer.index(projectRoot.resolve("src/test/java/com/cre/fixtures/ComplexGenericController.java"));
        
        String fromId = "com.cre.fixtures.ComplexGenericController::processAll(List,GenericService)";
        assertThat(graph.node(fromId)).isNotNull();
        
        var calls = graph.edgesFrom(fromId).stream()
            .filter(e -> e.type() == EdgeType.CALLS)
            .map(GraphEdge::to)
            .toList();
            
        assertThat(calls).contains("com.cre.fixtures.GenericService::process(T)");
    }
}
