package com.cre.tools.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Placeholder(
    String kind,
    String reason,
    @JsonProperty("likely_next_tool") String likelyNextTool,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("target_node_id") String targetNodeId,
    @JsonProperty("slice_boundary") String sliceBoundary) {

  public static Placeholder expandCallee(String targetNodeId, String boundary) {
    return new Placeholder(
        "depth_limit",
        "Callee omitted due to depth limit",
        "expand",
        targetNodeId,
        boundary);
  }
}
