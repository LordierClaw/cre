package com.cre.core.plugins;

import com.cre.core.graph.GraphEngine;
import java.nio.file.Path;
import java.util.List;

public interface GraphPlugin {
  String pluginId();

  void enrich(GraphEngine graph, Path javaSourceRoot, List<Path> javaFiles);
}

