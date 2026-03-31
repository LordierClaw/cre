package com.cre.core.service;

import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link ContextPostProcessor} that returns context as-is.
 */
@Component
public class DefaultContextPostProcessor implements ContextPostProcessor {
  @Override
  public String process(String context) {
    return context;
  }
}
