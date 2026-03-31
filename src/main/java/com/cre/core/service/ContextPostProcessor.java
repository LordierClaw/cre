package com.cre.core.service;

/**
 * Interface for post-processing reconstructed context.
 */
public interface ContextPostProcessor {
  /**
   * Process the reconstructed context string.
   *
   * @param context The reconstructed context.
   * @return The processed context.
   */
  String process(String context);
}
