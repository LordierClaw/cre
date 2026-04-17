package com.cre.core.service;

import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link ContextPostProcessor} that applies formatting cleanup.
 */
@Component
public class DefaultContextPostProcessor implements ContextPostProcessor {
  @Override
  public String process(String context) {
    if (context == null || context.isEmpty()) {
      return context;
    }

    String result = context;

    // 1. Ensure <omitted_.../> markers are on their own line
    // Inline before: text <omitted_.../>
    result = result.replaceAll("([^\\n])\\s*(<omitted_(functions|properties)/>)", "$1\n$2");
    // Inline after: <omitted_.../> text
    result = result.replaceAll("(<omitted_(functions|properties)/>)\\s*([^\\n])", "$1\n$3");

    // 2. Collapse 3+ consecutive newlines (including whitespace-only lines) into exactly 2 newlines (one empty line)
    // This targets blocks of 2+ empty lines
    result = result.replaceAll("(\\r?\\n\\s*){3,}", "\n\n");

    // 3. Trim whitespace inside <file> blocks
    // Trim after opening tag
    result = result.replaceAll("(<file[^>]*>)\\s+", "$1\n");
    // Trim before closing tag
    result = result.replaceAll("\\s+(</file>)", "\n$1");

    return result.trim();
  }
}
