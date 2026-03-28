package com.cre.tools.model;

import java.util.List;

/**
 * Represents a single source file in the integrated code view.
 * Grouped by origin to provide file-centric context.
 */
public record IntegratedFile(
    String origin,
    @com.fasterxml.jackson.annotation.JsonProperty("package_name") String packageName,
    List<String> imports,
    String code) {}
