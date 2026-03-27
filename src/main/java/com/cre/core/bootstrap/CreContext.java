package com.cre.core.bootstrap;

import com.cre.core.ast.JavaAstIndexer;
import com.cre.core.graph.GraphEngine;
import com.cre.core.plugins.PluginRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class CreContext {

  private static final Set<String> DEFAULT_EXCLUSIONS = Set.of(
      ".git", "target", "build", ".gradle", ".idea", ".settings", "bin", "out", "node_modules"
  );

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

  public static CreContext fromDirectory(Path projectRoot, boolean pluginsEnabled) throws IOException {
    Path sourceRoot = projectRoot.resolve("src/main/java");
    if (!Files.exists(sourceRoot)) {
      sourceRoot = projectRoot;
    }

    List<Path> javaFiles = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(projectRoot)) {
      walk.filter(p -> p.toString().endsWith(".java"))
          .filter(p -> !shouldExclude(projectRoot, p))
          .forEach(javaFiles::add);
    }

    return fromJavaSourceRoot(sourceRoot, pluginsEnabled, javaFiles.toArray(new Path[0]));
  }

  private static boolean shouldExclude(Path root, Path p) {
    Path relative = root.relativize(p);
    for (Path part : relative) {
      if (DEFAULT_EXCLUSIONS.contains(part.toString())) {
        return true;
      }
    }
    return false;
  }

  public static CreContext fromJavaSourceRoot(Path javaSourceRoot, Path... javaFiles) throws IOException {
    return fromJavaSourceRoot(javaSourceRoot, true, javaFiles);
  }

  public static CreContext fromJavaSourceRoot(Path javaSourceRoot, boolean pluginsEnabled, Path... javaFiles)
      throws IOException {
    GraphEngine g = new GraphEngine();
    JavaAstIndexer indexer = new JavaAstIndexer(g, javaSourceRoot);
    for (Path p : javaFiles) {
      indexer.index(p);
    }
    PluginRegistry.applyPlugins(g, javaSourceRoot, List.of(javaFiles), pluginsEnabled);
    return new CreContext(g, javaSourceRoot);
  }

  public static CreContext defaultFixtureContext() throws IOException {
    return defaultFixtureContext(true);
  }

  public static CreContext defaultFixtureContext(boolean pluginsEnabled) throws IOException {
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
    return fromJavaSourceRoot(javaRoot, pluginsEnabled, files);
  }
}
