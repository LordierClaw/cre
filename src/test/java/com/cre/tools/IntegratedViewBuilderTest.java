package com.cre.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.graph.NodeId;
import com.cre.core.graph.model.GraphNode;
import com.cre.core.graph.model.NodeKind;
import com.cre.testsupport.GraphTestSupport;
import com.cre.testsupport.ExceptionFlowTestSupport;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IntegratedViewBuilderTest {

  @Test
  void test_build_integrated_view_with_pruning() throws Exception {
    CreContext ctx = ExceptionFlowTestSupport.load(true);
    Path sourceRoot = ctx.javaSourceRoot();
    
    NodeId retainedMethod = GraphTestSupport.requireMethod(
        ctx.graph(), "com.cre.fixtures.UserController", "getUser(String)");
    
    NodeId prunedField = ctx.graph().nodes().values().stream()
        .filter(n -> n.kind() == NodeKind.FIELD)
        .map(GraphNode::id)
        .filter(id -> id.fullyQualifiedType().equals("com.cre.fixtures.UserController") && id.memberSignature().equals("field:userService"))
        .findFirst()
        .orElseThrow();

    IntegratedViewBuilder builder = new IntegratedViewBuilder();
    String result = builder.build(
        Set.of(retainedMethod),
        Set.of(prunedField),
        ctx.graph(),
        sourceRoot,
        retainedMethod);

    assertThat(result).contains("<file origin=\"com/cre/fixtures/UserController.java\">");
    assertThat(result).contains("<UserController.getUser>");
    assertThat(result).contains("<UserController>");
    assertThat(result).contains("public String getUser(String id)");
    assertThat(result).contains("<ommitted_properties/>");
  }

  @Test
  void test_method_call_replacement() throws Exception {
    CreContext ctx = ExceptionFlowTestSupport.load(true);
    Path sourceRoot = ctx.javaSourceRoot();

    NodeId userControllerMethod = GraphTestSupport.requireMethod(
        ctx.graph(), "com.cre.fixtures.UserController", "getUser(String)");
    
    NodeId userServiceMethod = GraphTestSupport.requireMethod(
        ctx.graph(), "com.cre.fixtures.UserService", "getUser(String)");

    IntegratedViewBuilder builder = new IntegratedViewBuilder();
    String result = builder.build(
        Set.of(userControllerMethod),
        Set.of(userServiceMethod),
        ctx.graph(),
        sourceRoot,
        userControllerMethod);

    assertThat(result).contains("<ommitted_code id=\"ommitted_01\" description=\"\"/>");
    assertThat(result).contains("ommitted_01: com.cre.fixtures.UserService::getUser(String)::com/cre/fixtures/UserService.java");
  }

  @Test
  void test_functions_grouping() throws Exception {
    CreContext ctx = ExceptionFlowTestSupport.load(true);
    Path sourceRoot = ctx.javaSourceRoot();

    NodeId getUser = GraphTestSupport.requireMethod(
        ctx.graph(), "com.cre.fixtures.UserController", "getUser(String)");
    
    NodeId prunedNode = ctx.graph().nodes().values().stream()
        .filter(n -> n.id().fullyQualifiedType().equals("com.cre.fixtures.UserController"))
        .filter(n -> !n.id().equals(getUser))
        .map(GraphNode::id)
        .findFirst().orElseThrow();

    IntegratedViewBuilder builder = new IntegratedViewBuilder();
    String result = builder.build(
        Set.of(getUser),
        Set.of(prunedNode),
        ctx.graph(),
        sourceRoot,
        getUser);

    assertThat(result).contains("<ommitted_functions/>");
  }

  @Test
  void test_imports_grouping() throws Exception {
    CreContext ctx = ExceptionFlowTestSupport.load(true);
    Path sourceRoot = ctx.javaSourceRoot();

    NodeId restController = new NodeId("org.springframework.web.bind.annotation.RestController", "<type>", "mock");
    
    NodeId getUser = GraphTestSupport.requireMethod(
        ctx.graph(), "com.cre.fixtures.UserController", "getUser(String)");

    IntegratedViewBuilder builder = new IntegratedViewBuilder();
    String result = builder.build(
        Set.of(getUser),
        Set.of(restController),
        ctx.graph(),
        sourceRoot,
        getUser);

    assertThat(result).contains("<ommitted_import/>");
    assertThat(result).doesNotContain("import org.springframework.web.bind.annotation.RestController;");
  }
}
