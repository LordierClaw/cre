package com.cre.core.ast;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;

public final class AstUtils {

  public static final JavaParser JAVA_PARSER = new JavaParser(
      new ParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_21)
  );

  private AstUtils() {}
}
