package com.cre.core.ast;

import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.model.EdgeType;
import com.cre.core.graph.model.GraphEdge;
import com.cre.core.graph.model.GraphNode;
import com.cre.core.graph.model.NodeKind;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class JavaAstIndexer {

  private final GraphEngine graph;
  private final Path javaSourceRoot;

  public JavaAstIndexer(GraphEngine graph, Path javaSourceRoot) {
    this.graph = graph;
    this.javaSourceRoot = javaSourceRoot;
  }

  public void index(Path path) throws IOException {
    String source = Files.readString(path);
    CompilationUnit cu = AstUtils.JAVA_PARSER.parse(source).getResult()
        .orElseThrow(() -> new RuntimeException("Failed to parse " + path));
    for (TypeDeclaration<?> td : cu.getTypes()) {
      if (td instanceof ClassOrInterfaceDeclaration cid) {
        indexType(cid, cu);
      }
    }
  }

  private void indexType(ClassOrInterfaceDeclaration decl, CompilationUnit cu) {
    String fqName = resolveTypeFqName(decl, cu);
    String typeId = fqName;
    graph.addNode(
        new GraphNode(
            typeId,
            NodeKind.TYPE,
            decl.getNameAsString(),
            decl.toString()));

    if (!decl.isInterface()) {
      for (ClassOrInterfaceType impl : decl.getImplementedTypes()) {
        resolveTypeFqNameFromType(impl, cu)
            .ifPresent(
                ifaceFqn -> graph.registerImplementation(ifaceFqn, typeId));
      }
    }

    for (MethodDeclaration md : decl.getMethods()) {
      String methodId = fqName + "::" + methodSignature(md);
      graph.addNode(
          new GraphNode(
              methodId,
              NodeKind.METHOD,
              md.getNameAsString(),
              md.toString()));
      graph.addEdge(new GraphEdge(methodId, typeId, EdgeType.BELONGS_TO));
      indexMethodCalls(md, decl, cu, fqName);
    }

    for (FieldDeclaration fd : decl.getFields()) {
      for (VariableDeclarator v : fd.getVariables()) {
        String fieldId = fqName + "::field:" + v.getNameAsString();
        graph.addNode(
            new GraphNode(
                fieldId,
                NodeKind.FIELD,
                v.getNameAsString(),
                v.toString()));
        graph.addEdge(new GraphEdge(fieldId, typeId, EdgeType.BELONGS_TO));
      }
    }

    for (com.github.javaparser.ast.body.BodyDeclaration<?> member : decl.getMembers()) {
      if (member instanceof ClassOrInterfaceDeclaration nested) {
        indexType(nested, cu);
      }
    }
  }

  private void indexMethodCalls(
      MethodDeclaration md,
      ClassOrInterfaceDeclaration clazz,
      CompilationUnit cu,
      String declaringFqName) {
    String fromId = declaringFqName + "::" + methodSignature(md);
    Map<String, String> paramTypes = new HashMap<>();
    for (Parameter p : md.getParameters()) {
      paramTypes.put(p.getNameAsString(), p.getType().asString());
    }

    md.findAll(MethodCallExpr.class)
        .forEach(
            call -> {
              resolveCallee(call, clazz, cu, paramTypes)
                  .ifPresent(
                      callee -> graph.addEdge(new GraphEdge(fromId, callee, EdgeType.CALLS)));
            });

    md.findAll(FieldAccessExpr.class)
        .forEach(
            fa -> {
              if (fa.getScope() instanceof NameExpr scopeName
                  && "this".equals(scopeName.getNameAsString())) {
                String fieldName = fa.getNameAsString();
                String fieldId = declaringFqName + "::field:" + fieldName;
                if (graph.node(fieldId) != null) {
                  graph.addEdge(new GraphEdge(fromId, fieldId, EdgeType.USES_FIELD));
                }
              }
            });

    md.findAll(NameExpr.class)
        .forEach(
            ne -> {
              String name = ne.getNameAsString();
              if (paramTypes.containsKey(name) || "this".equals(name)) {
                return;
              }
              String fieldId = declaringFqName + "::field:" + name;
              if (graph.node(fieldId) != null) {
                graph.addEdge(new GraphEdge(fromId, fieldId, EdgeType.USES_FIELD));
              }
            });
  }

  private Optional<String> resolveCallee(
      MethodCallExpr call,
      ClassOrInterfaceDeclaration clazz,
      CompilationUnit cu,
      Map<String, String> paramTypes) {
    Optional<String> calleeTypeFqn =
        call.getScope().map(s -> resolveScopeTypeFqn(s, clazz, cu)).orElse(Optional.empty());

    if (calleeTypeFqn.isEmpty()) {
      return Optional.empty();
    }

    String methodName = call.getNameAsString();
    String signature =
        methodName
            + "("
            + call.getArguments().stream()
                .map(a -> inferArgumentType(a, paramTypes))
                .collect(Collectors.joining(","))
            + ")";

    return Optional.of(calleeTypeFqn.get() + "::" + signature);
  }

  private Optional<String> resolveScopeTypeFqn(
      Expression scope, ClassOrInterfaceDeclaration clazz, CompilationUnit cu) {
    if (scope instanceof NameExpr ne) {
      return resolveNameExprType(ne.getNameAsString(), clazz, cu);
    }
    if (scope instanceof FieldAccessExpr fa) {
      if (fa.getScope() instanceof NameExpr root
          && "this".equals(root.getNameAsString())) {
        return resolveFieldTypeName(clazz, fa.getNameAsString(), cu);
      }
      return resolveScopeTypeFqn(fa.getScope(), clazz, cu);
    }
    return Optional.empty();
  }

  private Optional<String> resolveNameExprType(
      String name, ClassOrInterfaceDeclaration clazz, CompilationUnit cu) {
    return resolveFieldTypeName(clazz, name, cu);
  }

  private Optional<String> resolveFieldTypeName(
      ClassOrInterfaceDeclaration clazz, String fieldName, CompilationUnit cu) {
    for (FieldDeclaration fd : clazz.getFields()) {
      for (VariableDeclarator v : fd.getVariables()) {
        if (v.getNameAsString().equals(fieldName)) {
          com.github.javaparser.ast.type.Type t = v.getType();
          if (t.isClassOrInterfaceType()) {
            return resolveTypeReferenceToFqn(t.asClassOrInterfaceType(), cu);
          }
          return resolveSimpleNameToFqn(cu, t.asString());
        }
      }
    }
    return Optional.empty();
  }

  private Optional<String> resolveTypeReferenceToFqn(
      ClassOrInterfaceType type, CompilationUnit cu) {
    if (type == null) {
      return Optional.empty();
    }
    String simple = type.getNameAsString();
    return resolveSimpleNameToFqn(cu, simple);
  }

  private Optional<String> resolveTypeFqNameFromType(
      ClassOrInterfaceType type, CompilationUnit cu) {
    return resolveTypeReferenceToFqn(type, cu);
  }

  private Optional<String> resolveSimpleNameToFqn(CompilationUnit cu, String simpleName) {
    for (com.github.javaparser.ast.ImportDeclaration im : cu.getImports()) {
      if (im.isStatic() || im.isAsterisk()) {
        continue;
      }
      String full = im.getNameAsString();
      if (full.endsWith("." + simpleName)) {
        return Optional.of(full);
      }
    }
    return cu.getPackageDeclaration()
        .map(pd -> pd.getNameAsString() + "." + simpleName);
  }

  private String resolveTypeFqName(ClassOrInterfaceDeclaration decl, CompilationUnit cu) {
    Optional<String> direct = decl.getFullyQualifiedName();
    if (direct.isPresent()) {
      return direct.get();
    }
    String simple = decl.getNameAsString();
    return resolveSimpleNameToFqn(cu, simple).orElse(simple);
  }

  public static String methodSignature(MethodDeclaration md) {
    String params =
        md.getParameters().stream()
            .map(p -> p.getType().asString())
            .collect(Collectors.joining(","));
    return md.getNameAsString() + "(" + params + ")";
  }

  private static String inferArgumentType(Expression expr, Map<String, String> paramTypes) {
    if (expr instanceof StringLiteralExpr) {
      return "String";
    }
    if (expr instanceof NameExpr ne) {
      return paramTypes.getOrDefault(ne.getNameAsString(), "?");
    }
    return "?";
  }
}
