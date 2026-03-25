package com.cre.core.plugins;

import com.cre.core.ast.JavaAstIndexer;
import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.NodeId;
import com.cre.core.graph.model.EdgeType;
import com.cre.core.graph.model.GraphEdge;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enriches the graph with exception-flow edges: method calls made from {@code catch} block bodies,
 * linked from the enclosing method. Resolution rules match {@link JavaAstIndexer} for {@code CALLS}.
 */
public final class ExceptionFlowPlugin implements GraphPlugin {

  @Override
  public String pluginId() {
    return "exception_flow";
  }

  @Override
  public void enrich(GraphEngine graph, Path javaSourceRoot, List<Path> javaFiles) {
    for (Path path : javaFiles) {
      try {
        CompilationUnit cu = StaticJavaParser.parse(path);
        String origin = NodeId.normalizeOrigin(path);
        for (var td : cu.getTypes()) {
          if (td instanceof ClassOrInterfaceDeclaration cid) {
            enrichType(graph, javaSourceRoot, cid, cu, origin);
          }
        }
      } catch (IOException ignored) {
        // Fail-soft: omit edges for unparseable files (same surface as skipping unresolved calls).
      }
    }
  }

  private void enrichType(
      GraphEngine graph,
      Path javaSourceRoot,
      ClassOrInterfaceDeclaration cid,
      CompilationUnit cu,
      String origin) {
    String fqName = resolveTypeFqName(cid, cu);
    for (MethodDeclaration md : cid.getMethods()) {
      if (md.getBody().isEmpty()) {
        continue;
      }
      NodeId fromId = new NodeId(fqName, JavaAstIndexer.methodSignature(md), origin);
      if (graph.node(fromId) == null) {
        continue;
      }
      Map<String, String> paramTypes = new HashMap<>();
      for (Parameter p : md.getParameters()) {
        paramTypes.put(p.getNameAsString(), p.getType().asString());
      }
      Set<String> seen = new HashSet<>();
      for (TryStmt tryStmt : md.findAll(TryStmt.class)) {
        for (CatchClause cc : tryStmt.getCatchClauses()) {
          for (MethodCallExpr call : cc.getBody().findAll(MethodCallExpr.class)) {
            resolveCallee(call, cid, cu, paramTypes, javaSourceRoot)
                .ifPresent(
                    callee -> {
                      String key = fromId + "|" + callee + "|" + EdgeType.CATCH_INVOKES;
                      if (seen.add(key)) {
                        graph.addEdge(new GraphEdge(fromId, callee, EdgeType.CATCH_INVOKES));
                      }
                    });
          }
        }
      }
    }
  }

  // --- Call resolution (aligned with JavaAstIndexer; keep in sync for deterministic NodeIds) ---

  private Optional<NodeId> resolveCallee(
      MethodCallExpr call,
      ClassOrInterfaceDeclaration clazz,
      CompilationUnit cu,
      Map<String, String> paramTypes,
      Path javaSourceRoot) {
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

    String calleeOrigin = originForFqn(calleeTypeFqn.get(), javaSourceRoot);
    return Optional.of(new NodeId(calleeTypeFqn.get(), signature, calleeOrigin));
  }

  private String originForFqn(String typeFqn, Path javaSourceRoot) {
    return NodeId.normalizeOrigin(javaSourceRoot.resolve(typeFqn.replace('.', '/') + ".java"));
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
    for (var fd : clazz.getFields()) {
      for (var v : fd.getVariables()) {
        if (v.getNameAsString().equals(fieldName)) {
          var t = v.getType();
          if (t.isClassOrInterfaceType()) {
            return resolveTypeReferenceToFqn(t.asClassOrInterfaceType(), cu);
          }
          return resolveSimpleNameToFqn(cu, t.asString());
        }
      }
    }
    return Optional.empty();
  }

  private Optional<String> resolveTypeReferenceToFqn(ClassOrInterfaceType type, CompilationUnit cu) {
    if (type == null) {
      return Optional.empty();
    }
    String simple = type.getNameAsString();
    return resolveSimpleNameToFqn(cu, simple);
  }

  private Optional<String> resolveSimpleNameToFqn(CompilationUnit cu, String simpleName) {
    for (var im : cu.getImports()) {
      if (im.isStatic() || im.isAsterisk()) {
        continue;
      }
      String full = im.getNameAsString();
      if (full.endsWith("." + simpleName)) {
        return Optional.of(full);
      }
    }
    return cu.getPackageDeclaration().map(pd -> pd.getNameAsString() + "." + simpleName);
  }

  private String resolveTypeFqName(ClassOrInterfaceDeclaration decl, CompilationUnit cu) {
    Optional<String> direct = decl.getFullyQualifiedName();
    if (direct.isPresent()) {
      return direct.get();
    }
    String simple = decl.getNameAsString();
    return resolveSimpleNameToFqn(cu, simple).orElse(simple);
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
