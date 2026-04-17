package com.cre.core.ast;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import java.util.stream.Collectors;

public final class AstUtils {

  public static final JavaParser JAVA_PARSER = new JavaParser(
      new ParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_21)
  );

  private AstUtils() {}

  /**
   * Strips generics and FQN prefixes (e.g., "java.util.List<String>" -> "List").
   */
  public static String normalizeType(String typeName) {
    if (typeName == null) {
      return "?";
    }
    String normalized = typeName;
    if (normalized.contains("<")) {
      normalized = normalized.substring(0, normalized.indexOf("<")).trim();
    }
    if (normalized.contains(".")) {
      normalized = normalized.substring(normalized.lastIndexOf(".") + 1);
    }
    return normalized;
  }

  /**
   * Generates "name(param1,param2)" using normalized type names.
   */
  public static String getMethodSignature(MethodDeclaration md) {
    String params = md.getParameters().stream()
        .map(p -> normalizeType(p.getType().asString()))
        .collect(Collectors.joining(","));
    return md.getNameAsString() + "(" + params + ")";
  }

  /**
   * Generates "name(param1,param2)" using normalized type names.
   */
  public static String getConstructorSignature(ConstructorDeclaration cd) {
    String params = cd.getParameters().stream()
        .map(p -> normalizeType(p.getType().asString()))
        .collect(Collectors.joining(","));
    return cd.getNameAsString() + "(" + params + ")";
  }

  /**
   * Generates "name()" for compact constructors.
   */
  public static String getCompactConstructorSignature(CompactConstructorDeclaration ccd) {
    return ccd.getNameAsString() + "()";
  }

  /**
   * Generates "declaringFqn::name(param1,param2)" using normalized type names.
   */
  public static String getResolvedMethodSignature(ResolvedMethodDeclaration rmd) {
    String declaringType = rmd.declaringType().getQualifiedName();
    String methodName = rmd.getName();
    StringBuilder params = new StringBuilder();
    for (int i = 0; i < rmd.getNumberOfParams(); i++) {
      String pType = rmd.getParam(i).getType().describe();
      if (params.length() > 0) {
        params.append(",");
      }
      params.append(normalizeType(pType));
    }
    return declaringType + "::" + methodName + "(" + params.toString() + ")";
  }

  /**
   * Generates "declaringFqn::name(param1,param2)" using normalized type names.
   */
  public static String getResolvedConstructorSignature(ResolvedConstructorDeclaration rcd) {
    String declaringType = rcd.declaringType().getQualifiedName();
    String constructorName = rcd.getName();
    StringBuilder params = new StringBuilder();
    for (int i = 0; i < rcd.getNumberOfParams(); i++) {
      String pType = rcd.getParam(i).getType().describe();
      if (params.length() > 0) {
        params.append(",");
      }
      params.append(normalizeType(pType));
    }
    return declaringType + "::" + constructorName + "(" + params.toString() + ")";
  }
}
