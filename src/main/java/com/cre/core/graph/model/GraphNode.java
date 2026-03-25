package com.cre.core.graph.model;

import com.cre.core.graph.NodeId;

public record GraphNode(NodeId id, NodeKind kind, String simpleName, String snippet) {

  public GraphNode(NodeId id, NodeKind kind, String simpleName) {
    this(id, kind, simpleName, "");
  }
}
