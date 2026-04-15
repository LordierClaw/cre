package com.cre.core.ast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.cre.core.graph.GraphEngine;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaAstIndexerTest {

    @TempDir
    Path tempDir;

    @Test
    void testFindFilePathForFqnWithGenerics() {
        GraphEngine graph = new GraphEngine();
        JavaAstIndexer indexer = new JavaAstIndexer(graph, tempDir);
        
        // This should not throw InvalidPathException even if it contains < >
        assertDoesNotThrow(() -> {
            Optional<Path> path = indexer.findFilePathForFqn("com.example.List<String>");
            assertThat(path).isEmpty();
        });
    }

    @Test
    void testFindFilePathForFqnStrippingGenerics() throws Exception {
        Path srcMainJava = tempDir.resolve("src/main/java");
        srcMainJava.toFile().mkdirs();
        Path javaFile = srcMainJava.resolve("com/example/MyService.java");
        javaFile.getParent().toFile().mkdirs();
        javaFile.toFile().createNewFile();

        GraphEngine graph = new GraphEngine();
        JavaAstIndexer indexer = new JavaAstIndexer(graph, tempDir);
        
        Optional<Path> path = indexer.findFilePathForFqn("com.example.MyService<String>");
        assertThat(path).isPresent();
        assertThat(path.get()).isEqualTo(javaFile);
    }
}
