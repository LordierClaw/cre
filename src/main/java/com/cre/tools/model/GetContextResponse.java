package com.cre.tools.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * V2 Tool response model: Integrated file-centric code view.
 * Replaces verbose relational node/edge lists with compact code blocks and placeholders.
 */
public record GetContextResponse(
    @JsonProperty("slice_version") String sliceVersion,
    Map<String, Object> metadata,
    @JsonProperty("integrated_files") List<IntegratedFile> integratedFiles,
    @JsonProperty("node_id_map") Map<String, String> nodeIdMap) {}
