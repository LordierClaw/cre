package com.cre.core.exception;

/**
 * Thrown when a class or method symbol cannot be resolved.
 */
public class SymbolNotFoundException extends CreException {
  public SymbolNotFoundException(String symbol) {
    super("Symbol not found: " + symbol);
  }
}
