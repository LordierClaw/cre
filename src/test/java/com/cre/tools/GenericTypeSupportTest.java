package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.graph.model.EdgeType;
import com.cre.core.graph.model.GraphEdge;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

public class GenericTypeSupportTest {

    @Test
    void testGenericTypeResolution() throws Exception {
        Path root = Path.of(System.getProperty("user.dir", "."));
        Path javaRoot = root.resolve("src/test/java");
        Path[] files = {
            javaRoot.resolve("com/cre/fixtures/GenericService.java"),
            javaRoot.resolve("com/cre/fixtures/GenericServiceImpl.java"),
            javaRoot.resolve("com/cre/fixtures/GenericController.java")
        };
        
        CreContext ctx = CreContext.fromProjectRoot(root, true, files);
        var graph = ctx.graph();
        
        // 1. Check if GenericController depends on GenericService (via constructor injection)
        // Note: Currently SpringSemanticsPlugin might fail to resolve GenericService<String> to com.cre.fixtures.GenericService
        assertThat(graph.edgesFrom("com.cre.fixtures.GenericController"))
            .extracting(GraphEdge::to)
            .contains("com.cre.fixtures.GenericService");
            
        // 2. Check if GenericController::create(String) calls GenericService::process(T)
        // The method ID in graph for GenericService::process is likely com.cre.fixtures.GenericService::process(T)
        // The call in GenericController is service.process(name) where name is String.
        // If it's not resolved correctly, it might be com.cre.fixtures.GenericService::process(String) which doesn't exist.
        
        String controllerCreateId = "com.cre.fixtures.GenericController::create(String)";
        assertThat(graph.node(controllerCreateId)).isNotNull();
        
        assertThat(graph.edgesFrom(controllerCreateId))
            .filteredOn(e -> e.type() == EdgeType.CALLS)
            .extracting(GraphEdge::to)
            .contains("com.cre.fixtures.GenericService::process(T)");
            
        // 3. Check Spring semantic edges
        // Controller -> Service (DEPENDS_ON)
        assertThat(graph.edgesFrom("com.cre.fixtures.GenericController"))
            .filteredOn(e -> e.type() == EdgeType.DEPENDS_ON)
            .extracting(GraphEdge::to)
            .contains("com.cre.fixtures.GenericService");
            
        // Service should be marked as SERVICE_LAYER
        assertThat(graph.edgesFrom("com.cre.fixtures.GenericService"))
            .filteredOn(e -> e.type() == EdgeType.SERVICE_LAYER)
            .isNotEmpty();
            
        // 4. Check dependencies on generic arguments
        // GenericController has List<String> in list() return type.
        // GenericController::list() should depend on java.util.List and java.lang.String
        String controllerListId = "com.cre.fixtures.GenericController::list()";
        assertThat(graph.edgesFrom(controllerListId))
            .filteredOn(e -> e.type() == EdgeType.DEPENDS_ON)
            .extracting(GraphEdge::to)
            .contains("java.util.List", "java.lang.String");
            
        // 4.1 Check complex generic field
        String complexMapFieldId = "com.cre.fixtures.GenericController::field:complexMap";
        assertThat(graph.edgesFrom(complexMapFieldId))
            .filteredOn(e -> e.type() == EdgeType.DEPENDS_ON)
            .extracting(GraphEdge::to)
            .contains("java.util.Map", "java.util.List", "java.lang.Integer");
            
        // 5. Check Spring Data Repository
        javaRoot.resolve("com/cre/fixtures/ItemRepository.java");
        CreContext ctxRepo = CreContext.fromProjectRoot(root, true, javaRoot.resolve("com/cre/fixtures/ItemRepository.java"));
        var graphRepo = ctxRepo.graph();
        
        // ItemRepository should be marked as SERVICE_LAYER (via Repository annotation)
        assertThat(graphRepo.edgesFrom("com.cre.fixtures.ItemRepository"))
            .filteredOn(e -> e.type() == EdgeType.SERVICE_LAYER)
            .isNotEmpty();
            
        // ItemRepository should depend on Item (generic argument of CrudRepository)
        // Note: Item is a nested/same-file class here, its FQN is com.cre.fixtures.Item
        assertThat(graphRepo.edgesFrom("com.cre.fixtures.ItemRepository"))
            .filteredOn(e -> e.type() == EdgeType.DEPENDS_ON)
            .extracting(GraphEdge::to)
            .contains("com.cre.fixtures.Item");
            
        // 6. Wildcards and Type Bounds
        assertThat(graph.nodes().keySet())
            .contains("com.cre.fixtures.GenericService::save(S)")
            .contains("com.cre.fixtures.GenericService::processAll(List)");
    }
}
