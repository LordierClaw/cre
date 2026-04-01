package com.cre.core.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Granular options for context reconstruction.
 */
public record ContextOptions(
    DefinitionLevel imports,
    DefinitionLevel properties,
    DefinitionLevel functions,
    Set<String> expandedFunctions
) {
  public enum DefinitionLevel {
    OMITTED, RELEVANCE, FULL;

    public static DefinitionLevel fromString(String val) {
      if (val == null) return null;
      try {
        return valueOf(val.toUpperCase());
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
  }

  public static ContextOptions defaultOptions() {
    return new ContextOptions(
        DefinitionLevel.OMITTED,
        DefinitionLevel.RELEVANCE,
        DefinitionLevel.OMITTED,
        Set.of()
    );
  }

  @SuppressWarnings("unchecked")
  public static ContextOptions fromMap(Map<String, Object> map) {
    if (map == null || map.isEmpty()) return defaultOptions();

    DefinitionLevel imp = DefinitionLevel.fromString((String) map.get("imports"));
    DefinitionLevel prop = DefinitionLevel.fromString((String) map.get("properties"));
    DefinitionLevel func = DefinitionLevel.fromString((String) map.get("functions"));

    Object exp = map.get("expanded_functions");
    Set<String> expanded = Set.of();
    if (exp instanceof List<?> list) {
      expanded = Set.copyOf(list.stream().map(String::valueOf).toList());
    }

    ContextOptions def = defaultOptions();
    return new ContextOptions(
        imp != null ? imp : def.imports(),
        prop != null ? prop : def.properties(),
        func != null ? func : def.functions(),
        expanded
    );
  }
}
