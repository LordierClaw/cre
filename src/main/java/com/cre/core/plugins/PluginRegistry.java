package com.cre.core.plugins;

import com.cre.core.graph.GraphEngine;
import java.nio.file.Path;
import java.util.List;

public final class PluginRegistry {

  private static final List<GraphPlugin> PLUGINS = List.of(new SpringSemanticsPlugin());

  private PluginRegistry() {}

  public static void applyPlugins(
      GraphEngine graph, Path javaSourceRoot, List<Path> javaFiles, boolean pluginsEnabled) {
    if (!pluginsEnabled) {
      graph.springSemanticsState(false, false, "plugins_disabled");
      return;
    }
    for (GraphPlugin p : PLUGINS) {
      p.enrich(graph, javaSourceRoot, javaFiles);
    }
  }
}

