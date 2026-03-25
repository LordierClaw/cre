package com.cre.core.graph.model;

import com.cre.core.graph.NodeId;

public record GraphEdge(NodeId from, NodeId to, EdgeType type) {}
