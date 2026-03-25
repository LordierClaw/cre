package com.cre.tools.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record GetContextResponse(
    @JsonProperty("slice_version") String sliceVersion,
    Map<String, Object> metadata,
    List<Map<String, Object>> nodes,
    List<Map<String, Object>> edges,
    @JsonProperty("sliced_code") List<Map<String, Object>> slicedCode,
    List<Placeholder> placeholders) {}
