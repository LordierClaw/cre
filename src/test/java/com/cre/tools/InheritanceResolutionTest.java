package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.ast.JavaAstIndexer;
import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.model.EdgeType;
import com.cre.core.graph.model.GraphEdge;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class InheritanceResolutionTest {

    @Test
    void testInheritanceResolution() throws Exception {
        Path projectRoot = Path.of(System.getProperty("user.dir", "."));
        GraphEngine graph = new GraphEngine();
        JavaAstIndexer indexer = new JavaAstIndexer(graph, projectRoot);
        
        indexer.index(projectRoot.resolve("src/test/java/com/cre/fixtures/BaseService.java"));
        indexer.index(projectRoot.resolve("src/test/java/com/cre/fixtures/AbstractBaseService.java"));
        indexer.index(projectRoot.resolve("src/test/java/com/cre/fixtures/RealService.java"));
        indexer.index(projectRoot.resolve("src/test/java/com/cre/fixtures/ServiceConsumer.java"));
        
        String fromId = "com.cre.fixtures.ServiceConsumer::consume()";
        
        var calls = graph.edgesFrom(fromId).stream()
            .filter(e -> e.type() == EdgeType.CALLS)
            .map(GraphEdge::to)
            .toList();
            
        assertThat(calls).contains("com.cre.fixtures.RealService::doRealWork()");
        assertThat(calls).contains("com.cre.fixtures.AbstractBaseService::doAbstractWork()");
        assertThat(calls).contains("com.cre.fixtures.RealService::doInterfaceWork()");
    }
}
