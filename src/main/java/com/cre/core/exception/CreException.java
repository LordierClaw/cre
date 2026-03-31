package com.cre.core.exception;

/**
 * Base class for all CRE-specific exceptions.
 */
public class CreException extends Exception {
  public CreException(String message) {
    super(message);
  }

  public CreException(String message, Throwable cause) {
    super(message, cause);
  }
}
