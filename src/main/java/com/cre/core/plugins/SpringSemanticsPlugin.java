package com.cre.core.plugins;

import com.cre.core.graph.GraphEngine;
import java.nio.file.Path;
import java.util.List;

public final class SpringSemanticsPlugin implements GraphPlugin {

  @Override
  public String pluginId() {
    return "spring-semantics";
  }

  @Override
  public void enrich(GraphEngine graph, Path javaSourceRoot, List<Path> javaFiles) {
    // Implemented in Phase 2 task 02-03.
  }
}

