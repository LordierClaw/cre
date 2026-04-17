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
    // If preceded by non-whitespace on the same line, insert newline before it, preserving indentation
    result = result.replaceAll("(?m)^(\\s*)(?!<omitted_(?:functions|properties)/>)(\\S.*?)[\\s&&[^\\n]]*(<omitted_(?:functions|properties)/>)", "$1$2\n$1$3");
    // If followed by non-whitespace on the same line, insert newline after it, preserving indentation
    result = result.replaceAll("(?m)^(\\s*)(<omitted_(?:functions|properties)/>)[\\s&&[^\\n]]*(\\S.*)$", "$1$2\n$1$3");

    // 2. Collapse 3+ consecutive newlines (including whitespace-only lines) into exactly 2 newlines (one empty line)
    result = result.replaceAll("(\\r?\\n[\\s&&[^\\n]]*){2,}\\r?\\n", "\n\n");

    // 3. Trim whitespace inside <file> blocks
    // Collapse multiple leading newlines after <file> tag into a single newline
    result = result.replaceAll("(<file[^>]*>)([\\s&&[^\\n]]*\\r?\\n)+", "$1\n");
    // Collapse multiple trailing newlines before </file> tag into a single newline
    result = result.replaceAll("(\\r?\\n[\\s&&[^\\n]]*)+(</file>)", "\n$2");


    // 4. Final cleanup: strip trailing whitespace but preserve leading whitespace of the first line
    return result.stripTrailing();
  }
}
