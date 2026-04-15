package com.cre.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.cre.core.bootstrap.ProjectManager;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CreServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void testFindFilePathForFqnWithGenerics() {
        ProjectManager pm = new ProjectManager();
        ContextPostProcessor pp = context -> context;
        CreServiceImpl service = new CreServiceImpl(pm, pp);
        
        assertDoesNotThrow(() -> {
            Optional<Path> path = service.findFilePathForFqn(tempDir, "com.example.List<String>");
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

        ProjectManager pm = new ProjectManager();
        ContextPostProcessor pp = context -> context;
        CreServiceImpl service = new CreServiceImpl(pm, pp);
        
        Optional<Path> path = service.findFilePathForFqn(tempDir, "com.example.MyService<String>");
        assertThat(path).isPresent();
        assertThat(path.get()).isEqualTo(javaFile);
    }
}
