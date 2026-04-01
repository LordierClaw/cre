package com.cre.core.graph.model;

public enum EdgeType {
  CALLS,
  USES_FIELD,
  BELONGS_TO,
  ENTRY_POINT,
  SERVICE_LAYER,
  DEPENDS_ON,
  IMPLEMENTS,
  /** From enclosing method to callee invoked from a catch clause body (not normal try-body calls). */
  CATCH_INVOKES
}
