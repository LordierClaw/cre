package com.cre.core.graph.model;

public record GraphNode(String id, NodeKind kind, String simpleName, String snippet) {

  public GraphNode(String id, NodeKind kind, String simpleName) {
    this(id, kind, simpleName, "");
  }
}
