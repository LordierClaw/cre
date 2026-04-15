package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.ast.JavaAstIndexer;
import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.model.EdgeType;
import com.cre.core.graph.model.GraphEdge;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class ModernJavaSupportTest {

    @Test
    void testRecordSupport() throws Exception {
        Path projectRoot = Path.of(System.getProperty("user.dir", "."));
        GraphEngine graph = new GraphEngine();
        JavaAstIndexer indexer = new JavaAstIndexer(graph, projectRoot);
        
        indexer.index(projectRoot.resolve("src/test/java/com/cre/fixtures/ModernJavaFixture.java"));
        
        String recordId = "com.cre.fixtures.ModernJavaFixture.UserRecord";
        assertThat(graph.node(recordId)).isNotNull();
        
        // Verify component fields
        assertThat(graph.node(recordId + "::field:id")).isNotNull();
        assertThat(graph.node(recordId + "::field:name")).isNotNull();
        
        // Verify accessor method call
        String consumerId = "com.cre.fixtures.ModernJavaFixture::useRecord(UserRecord)";
        assertThat(graph.node(consumerId)).isNotNull();
        
        var calls = graph.edgesFrom(consumerId).stream()
            .filter(e -> e.type() == EdgeType.CALLS)
            .map(GraphEdge::to)
            .toList();
            
        // Record accessor methods are also records of calls
        assertThat(calls).contains(recordId + "::name()");
    }

    @Test
    void testSealedClassSupport() throws Exception {
        Path projectRoot = Path.of(System.getProperty("user.dir", "."));
        GraphEngine graph = new GraphEngine();
        JavaAstIndexer indexer = new JavaAstIndexer(graph, projectRoot);
        
        indexer.index(projectRoot.resolve("src/test/java/com/cre/fixtures/ModernJavaFixture.java"));
        
        String shapeId = "com.cre.fixtures.ModernJavaFixture.Shape";
        String circleId = "com.cre.fixtures.ModernJavaFixture.Circle";
        String squareId = "com.cre.fixtures.ModernJavaFixture.Square";
        
        assertThat(graph.node(shapeId)).isNotNull();
        assertThat(graph.node(circleId)).isNotNull();
        assertThat(graph.node(squareId)).isNotNull();
        
        // Verify implementation edges
        // Note: GraphEngine records implementations
        // We'll check if they are registered correctly
    }
}
