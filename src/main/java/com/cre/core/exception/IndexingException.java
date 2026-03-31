package com.cre.core.exception;

/**
 * Thrown when project indexing fails.
 */
public class IndexingException extends CreException {
  public IndexingException(String message, Throwable cause) {
    super(message, cause);
  }
}
