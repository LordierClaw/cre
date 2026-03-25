package com.cre.core.bootstrap;

import com.cre.core.ast.JavaAstIndexer;
import com.cre.core.graph.GraphEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class CreContext {

  private final GraphEngine graph;
  private final Path javaSourceRoot;

  private CreContext(GraphEngine graph, Path javaSourceRoot) {
    this.graph = graph;
    this.javaSourceRoot = javaSourceRoot;
  }

  public GraphEngine graph() {
    return graph;
  }

  public Path javaSourceRoot() {
    return javaSourceRoot;
  }

  public static CreContext fromJavaSourceRoot(Path javaSourceRoot, Path... javaFiles) throws IOException {
    GraphEngine g = new GraphEngine();
    JavaAstIndexer indexer = new JavaAstIndexer(g, javaSourceRoot);
    for (Path p : javaFiles) {
      indexer.index(p);
    }
    return new CreContext(g, javaSourceRoot);
  }

  public static CreContext defaultFixtureContext() throws IOException {
    Path root = Path.of(System.getProperty("user.dir", "."));
    Path javaRoot = root.resolve("src/test/java");
    Path[] files = {
      javaRoot.resolve("com/cre/fixtures/UserService.java"),
      javaRoot.resolve("com/cre/fixtures/UserServiceImpl.java"),
      javaRoot.resolve("com/cre/fixtures/UserController.java")
    };
    if (!Arrays.stream(files).allMatch(Files::exists)) {
      throw new IllegalStateException("Fixture sources missing under src/test/java; cwd=" + root);
    }
    return fromJavaSourceRoot(javaRoot, files);
  }
}
