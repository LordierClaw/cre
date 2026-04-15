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
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class JavaAstIndexer {

  private final GraphEngine graph;
  private final Path projectRoot;
  private final JavaParser parser;
  private Path currentSourceRoot;

  public JavaAstIndexer(GraphEngine graph, Path projectRoot) {
    this.graph = graph;
    this.projectRoot = projectRoot;
    this.parser = createParser(projectRoot);
  }

  private JavaParser createParser(Path projectRoot) {
    CombinedTypeSolver typeSolver = new CombinedTypeSolver();
    typeSolver.add(new ReflectionTypeSolver());
    
    // Add source roots if they exist
    Path srcMain = projectRoot.resolve("src/main/java");
    if (Files.exists(srcMain)) {
      typeSolver.add(new JavaParserTypeSolver(srcMain));
    }
    Path srcTest = projectRoot.resolve("src/test/java");
    if (Files.exists(srcTest)) {
      typeSolver.add(new JavaParserTypeSolver(srcTest));
    }
    
    // Fallback to project root if it looks like a flat project
    if (!Files.exists(srcMain) && !Files.exists(srcTest)) {
      typeSolver.add(new JavaParserTypeSolver(projectRoot));
    }

    ParserConfiguration config = new ParserConfiguration()
        .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
        .setSymbolResolver(new JavaSymbolSolver(typeSolver));
    
    return new JavaParser(config);
  }

  private String normalizeResolvedType(String typeName) {
    if (typeName == null) return "?";
    String normalized = typeName;
    if (normalized.contains("<")) {
      normalized = normalized.substring(0, normalized.indexOf("<")).trim();
    }
    // Normalize JDK types if they aren't fully qualified by SymbolSolver
    if (isJavaLang(normalized) && !normalized.contains(".")) {
      return "java.lang." + normalized;
    }
    return normalized;
  }

  private String toMethodSymbol(ResolvedMethodDeclaration rmd) {
    String declaringType = rmd.declaringType().getQualifiedName();
    String methodName = rmd.getName();
    String params = "";
    for (int i = 0; i < rmd.getNumberOfParams(); i++) {
        String pType = rmd.getParam(i).getType().describe();
        if (pType.contains("<")) {
            pType = pType.substring(0, pType.indexOf("<")).trim();
        }
        if (pType.contains(".")) {
            pType = pType.substring(pType.lastIndexOf(".") + 1);
        }
        params += (params.isEmpty() ? "" : ",") + pType;
    }
    return declaringType + "::" + methodName + "(" + params + ")";
  }

  public void index(Path path) throws IOException {
    String source = Files.readString(path);
    CompilationUnit cu = parser.parse(source).getResult()
        .orElseThrow(() -> new RuntimeException("Failed to parse " + path));
    
    // Resolve source root for this file based on package
    this.currentSourceRoot = resolveSourceRoot(path, cu);

    for (TypeDeclaration<?> td : cu.getTypes()) {
      if (td instanceof ClassOrInterfaceDeclaration cid) {
        indexType(cid, cu);
      } else if (td instanceof RecordDeclaration rd) {
        indexRecord(rd, cu);
      }
    }
  }

  private Path resolveSourceRoot(Path filePath, CompilationUnit cu) {
    return cu.getPackageDeclaration()
        .map(pd -> {
          String pkg = pd.getNameAsString();
          Path p = filePath.getParent();
          String[] parts = pkg.split("\\.");
          for (int i = parts.length - 1; i >= 0; i--) {
            if (p != null && p.getFileName().toString().equals(parts[i])) {
              p = p.getParent();
            }
          }
          return p;
        })
        .orElse(filePath.getParent());
  }

  private void indexRecord(RecordDeclaration rd, CompilationUnit cu) {
    String fqName = rd.getFullyQualifiedName().orElse(rd.getNameAsString());
    String typeId = fqName;
    graph.addNode(
        new GraphNode(
            typeId,
            NodeKind.TYPE,
            rd.getNameAsString(),
            rd.toString()));

    for (ClassOrInterfaceType impl : rd.getImplementedTypes()) {
        resolveTypeReferenceToFqn(impl, cu).ifPresent(target -> {
            graph.addEdge(new GraphEdge(typeId, target, EdgeType.DEPENDS_ON));
            addTypeDependencies(typeId, impl, cu);
            graph.registerImplementation(target, typeId);
        });
    }

    // Index components as fields
    rd.getParameters().forEach(p -> {
        String fieldId = fqName + "::field:" + p.getNameAsString();
        graph.addNode(
            new GraphNode(
                fieldId,
                NodeKind.FIELD,
                p.getNameAsString(),
                p.toString()));
        graph.addEdge(new GraphEdge(fieldId, typeId, EdgeType.BELONGS_TO));
        addTypeDependencies(fieldId, p.getType(), cu);
    });

    for (MethodDeclaration md : rd.getMethods()) {
      String methodId = fqName + "::" + methodSignature(md);
      graph.addNode(
          new GraphNode(
              methodId,
              NodeKind.METHOD,
              md.getNameAsString(),
              md.toString()));
      graph.addEdge(new GraphEdge(methodId, typeId, EdgeType.BELONGS_TO));
      
      addTypeDependencies(methodId, md.getType(), cu);
      for (Parameter p : md.getParameters()) {
          addTypeDependencies(methodId, p.getType(), cu);
      }

      indexMethodCalls(md, rd, cu, fqName);
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

    for (ClassOrInterfaceType ext : decl.getExtendedTypes()) {
        resolveTypeReferenceToFqn(ext, cu).ifPresent(target -> {
            graph.addEdge(new GraphEdge(typeId, target, EdgeType.DEPENDS_ON));
            addTypeDependencies(typeId, ext, cu);
        });
    }

    for (ClassOrInterfaceType impl : decl.getImplementedTypes()) {
        resolveTypeReferenceToFqn(impl, cu).ifPresent(target -> {
            graph.addEdge(new GraphEdge(typeId, target, EdgeType.DEPENDS_ON));
            addTypeDependencies(typeId, impl, cu);
            if (!decl.isInterface()) {
                graph.registerImplementation(target, typeId);
            }
        });
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
      
      // Add DEPENDS_ON edges for return type and parameters (including generic arguments)
      addTypeDependencies(methodId, md.getType(), cu);
      for (Parameter p : md.getParameters()) {
          addTypeDependencies(methodId, p.getType(), cu);
      }

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
        
        // Add DEPENDS_ON edge for field type
        addTypeDependencies(fieldId, v.getType(), cu);
      }
    }

    for (var member : decl.getMembers()) {
      if (member instanceof ClassOrInterfaceDeclaration nestedCid) {
        indexType(nestedCid, cu);
      } else if (member instanceof RecordDeclaration nestedRd) {
        indexRecord(nestedRd, cu);
      }
    }
  }

  private void indexMethodCalls(
      MethodDeclaration md,
      TypeDeclaration<?> clazz,
      CompilationUnit cu,
      String declaringFqName) {
    String fromId = declaringFqName + "::" + methodSignature(md);
    Map<String, String> paramTypes = new HashMap<>();
    for (Parameter p : md.getParameters()) {
      paramTypes.put(p.getNameAsString(), p.getType().asString());
    }
    
    // Add local variables to paramTypes for type inference
    md.findAll(com.github.javaparser.ast.expr.VariableDeclarationExpr.class).forEach(vde -> {
        vde.getVariables().forEach(v -> {
            paramTypes.put(v.getNameAsString(), v.getType().asString());
        });
    });

    md.findAll(MethodCallExpr.class)
        .forEach(
            call -> {
              try {
                  ResolvedMethodDeclaration rmd = call.resolve();
                  graph.addEdge(new GraphEdge(fromId, toMethodSymbol(rmd), EdgeType.CALLS));
              } catch (Exception e) {
                  resolveCallee(call, clazz, cu, paramTypes)
                      .ifPresent(
                          callee -> graph.addEdge(new GraphEdge(fromId, callee, EdgeType.CALLS)));
              }
            });

    md.findAll(FieldAccessExpr.class)
        .forEach(
            fa -> {
              try {
                  var resolved = fa.resolve();
                  if (resolved.isField()) {
                      String fieldFqn = resolved.asField().declaringType().getQualifiedName() + "::field:" + resolved.getName();
                      graph.addEdge(new GraphEdge(fromId, fieldFqn, EdgeType.USES_FIELD));
                      return;
                  }
              } catch (Exception e) {
                  // Fallback
              }
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
              try {
                  var resolved = ne.resolve();
                  if (resolved.isField()) {
                      String fieldFqn = resolved.asField().declaringType().getQualifiedName() + "::field:" + resolved.getName();
                      graph.addEdge(new GraphEdge(fromId, fieldFqn, EdgeType.USES_FIELD));
                      return;
                  }
              } catch (Exception e) {
                  // Fallback
              }
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

  private void addTypeDependencies(String fromId, com.github.javaparser.ast.type.Type type, CompilationUnit cu) {
    if (type == null) return;

    resolveTypeReferenceToFqn(type, cu).ifPresent(target -> {
        graph.addEdge(new GraphEdge(fromId, target, EdgeType.DEPENDS_ON));
    });

    if (type.isClassOrInterfaceType()) {
        ClassOrInterfaceType ct = type.asClassOrInterfaceType();
        ct.getTypeArguments().ifPresent(args -> {
            for (com.github.javaparser.ast.type.Type arg : args) {
                addTypeDependencies(fromId, arg, cu);
            }
        });
    }
  }

  private Optional<String> resolveCallee(
      MethodCallExpr call,
      TypeDeclaration<?> clazz,
      CompilationUnit cu,
      Map<String, String> paramTypes) {
    try {
        ResolvedMethodDeclaration rmd = call.resolve();
        return Optional.of(toMethodSymbol(rmd));
    } catch (Exception e) {
        // Fallback to manual resolution logic (existing)
    }

    Optional<String> calleeTypeFqn =
        call.getScope().map(s -> resolveScopeTypeFqn(s, clazz, cu, paramTypes)).orElse(Optional.empty());

    if (calleeTypeFqn.isEmpty()) {
      return Optional.empty();
    }

    String methodName = call.getNameAsString();
    List<String> argTypes = call.getArguments().stream()
                .map(a -> inferArgumentType(a, paramTypes, clazz, cu))
                .toList();
    
    String signature = methodName + "(" + String.join(",", argTypes) + ")";
    String fullId = calleeTypeFqn.get() + "::" + signature;
    
    if (graph.node(fullId) != null) {
        return Optional.of(fullId);
    }
    
    // Heuristic: If we don't find exact match, try matching by replacing generic type parameters with '?' or actual inferred types
    final int argCount = argTypes.size();
    List<String> candidates = graph.nodes().keySet().stream()
        .filter(id -> id.startsWith(calleeTypeFqn.get() + "::" + methodName + "("))
        .filter(id -> {
            String params = id.substring(id.indexOf('(') + 1, id.lastIndexOf(')'));
            if (params.isEmpty()) return argCount == 0;
            String[] parts = params.split(",");
            if (parts.length != argCount) return false;
            
            for (int i = 0; i < argCount; i++) {
                String expected = parts[i].trim();
                String actual = argTypes.get(i);
                if (expected.equals(actual)) continue;
                // If expected is a single character (like T, E, K, V), it's likely a generic type parameter
                if (expected.length() == 1 && Character.isUpperCase(expected.charAt(0))) continue;
                if (actual.equals("?")) continue;
                return false;
            }
            return true;
        })
        .toList();
    
    if (!candidates.isEmpty()) {
        return Optional.of(candidates.get(0));
    }

    return Optional.of(fullId);
  }

  private Optional<String> resolveScopeTypeFqn(
      Expression scope, TypeDeclaration<?> clazz, CompilationUnit cu, Map<String, String> paramTypes) {
    if (scope instanceof NameExpr ne) {
      return resolveNameExprType(ne.getNameAsString(), clazz, cu, paramTypes);
    }
    if (scope instanceof MethodCallExpr mce) {
      return mce.getScope().flatMap(s -> resolveScopeTypeFqn(s, clazz, cu, paramTypes));
    }
    if (scope instanceof FieldAccessExpr fa) {
      if (fa.getScope() instanceof NameExpr root
          && "this".equals(root.getNameAsString())) {
        return resolveFieldTypeName(clazz, fa.getNameAsString(), cu);
      }
      return resolveScopeTypeFqn(fa.getScope(), clazz, cu, paramTypes);
    }
    return Optional.empty();
  }

  private Optional<String> resolveNameExprType(
      String name, TypeDeclaration<?> clazz, CompilationUnit cu, Map<String, String> paramTypes) {
    if (paramTypes.containsKey(name)) {
        String typeName = paramTypes.get(name);
        return resolveSimpleNameToFqn(cu, typeName);
    }
    return resolveFieldTypeName(clazz, name, cu);
  }

  private Optional<String> resolveFieldTypeName(
      TypeDeclaration<?> clazz, String fieldName, CompilationUnit cu) {
    if (clazz instanceof ClassOrInterfaceDeclaration cid) {
        for (FieldDeclaration fd : cid.getFields()) {
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
    }
    return Optional.empty();
  }

  private Optional<String> resolveTypeReferenceToFqn(
      com.github.javaparser.ast.type.Type type, CompilationUnit cu) {
    if (type == null) {
      return Optional.empty();
    }
    
    // Try SymbolSolver first
    try {
        String resolved = type.resolve().describe();
        if (resolved.contains("<")) {
            resolved = resolved.substring(0, resolved.indexOf("<")).trim();
        }
        return Optional.of(resolved);
    } catch (Exception e) {
        // Fallback to manual resolution
    }

    if (type.isClassOrInterfaceType()) {
        return resolveTypeReferenceToFqn(type.asClassOrInterfaceType(), cu);
    }
    String simple = type.asString();
    if (simple.contains("<")) simple = simple.substring(0, simple.indexOf('<'));
    return resolveSimpleNameToFqn(cu, simple);
  }

  private Optional<String> resolveTypeReferenceToFqn(
      ClassOrInterfaceType type, CompilationUnit cu) {
    if (type == null) {
      return Optional.empty();
    }

    // Try SymbolSolver first
    try {
        String resolved = type.resolve().describe();
        if (resolved.contains("<")) {
            resolved = resolved.substring(0, resolved.indexOf("<")).trim();
        }
        return Optional.of(resolved);
    } catch (Exception e) {
        // Fallback to manual resolution
    }

    String simple = type.getNameAsString();
    return resolveSimpleNameToFqn(cu, simple);
  }

  private Optional<String> resolveTypeFqNameFromType(
      ClassOrInterfaceType type, CompilationUnit cu) {
    return resolveTypeReferenceToFqn(type, cu);
  }

  private Optional<String> resolveSimpleNameToFqn(CompilationUnit cu, String simpleName) {
    if (isJavaLang(simpleName)) {
        return Optional.of("java.lang." + simpleName);
    }
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

  private boolean isJavaLang(String simpleName) {
      // Basic list of common java.lang classes. For a real tool, this might be more comprehensive.
      return List.of("String", "Integer", "Long", "Double", "Boolean", "Object", "Exception", "RuntimeException", "Throwable", "Void")
              .contains(simpleName);
  }

  private String resolveTypeFqName(TypeDeclaration<?> decl, CompilationUnit cu) {
    Optional<String> direct = decl.getFullyQualifiedName();
    if (direct.isPresent()) {
      return direct.get();
    }
    String simple = decl.getNameAsString();
    return resolveSimpleNameToFqn(cu, simple).orElse(simple);
  }

  public static String methodSignature(MethodDeclaration md) {
    try {
        var rmd = md.resolve();
        String params = "";
        for (int i = 0; i < rmd.getNumberOfParams(); i++) {
            String pType = rmd.getParam(i).getType().describe();
            if (pType.contains("<")) {
                pType = pType.substring(0, pType.indexOf("<")).trim();
            }
            if (pType.contains(".")) {
                pType = pType.substring(pType.lastIndexOf(".") + 1);
            }
            params += (params.isEmpty() ? "" : ",") + pType;
        }
        return rmd.getName() + "(" + params + ")";
    } catch (Exception e) {
        String params =
            md.getParameters().stream()
                .map(p -> p.getType().asString())
                .collect(Collectors.joining(","));
        return md.getNameAsString() + "(" + params + ")";
    }
  }

  private String inferArgumentType(Expression expr, Map<String, String> paramTypes, TypeDeclaration<?> clazz, CompilationUnit cu) {
    if (expr instanceof StringLiteralExpr) {
      return "String";
    }
    if (expr instanceof NameExpr ne) {
      return paramTypes.getOrDefault(ne.getNameAsString(), "?");
    }
    if (expr instanceof MethodCallExpr mce) {
        // Try to resolve the return type of the method call
        return resolveTypeForExpression(mce, clazz, cu, paramTypes).orElse("?");
    }
    if (expr instanceof FieldAccessExpr fae) {
        return resolveTypeForExpression(fae, clazz, cu, paramTypes).orElse("?");
    }
    return "?";
  }

  private Optional<String> resolveTypeForExpression(Expression expr, TypeDeclaration<?> clazz, CompilationUnit cu, Map<String, String> paramTypes) {
      if (expr instanceof NameExpr ne) {
          String name = ne.getNameAsString();
          return Optional.ofNullable(resolveNameExprType(name, clazz, cu, paramTypes).orElse(null));
      }
      if (expr instanceof MethodCallExpr mce) {
          Optional<String> scopeType = mce.getScope()
                  .flatMap(s -> resolveScopeTypeFqn(s, clazz, cu, paramTypes));
          
          if (scopeType.isPresent()) {
              String methodName = mce.getNameAsString();
              String targetFqn = scopeType.get();
              if (targetFqn.contains("<")) {
                  targetFqn = targetFqn.substring(0, targetFqn.indexOf("<")).trim();
              }
              
              if (targetFqn.equals(resolveTypeFqName(clazz, cu))) {
                  for (MethodDeclaration md : clazz.getMethods()) {
                      if (md.getNameAsString().equals(methodName)) {
                          return Optional.of(md.getType().asString());
                      }
                  }
              } else {
                  // Try to find the file for targetFqn
                  Optional<Path> path = findFilePathForFqn(targetFqn);
                  if (path.isPresent()) {
                      try {
                          String source = Files.readString(path.get());
                          CompilationUnit otherCu = parser.parse(source).getResult().orElse(null);
                          if (otherCu != null) {
                              for (TypeDeclaration<?> td : otherCu.getTypes()) {
                                  if (td instanceof ClassOrInterfaceDeclaration otherCid) {
                                      if (targetFqn.endsWith("." + otherCid.getNameAsString())) {
                                          for (MethodDeclaration md : otherCid.getMethods()) {
                                              if (md.getNameAsString().equals(methodName)) {
                                                  return Optional.of(md.getType().asString());
                                              }
                                          }
                                      }
                                  }
                              }
                          }
                      } catch (IOException e) {
                          // Ignore
                      }
                  }
              }
          }
      }
      return Optional.empty();
  }

  Optional<Path> findFilePathForFqn(String fqn) {
    String baseFqn = fqn;
    if (fqn.contains("<")) {
      baseFqn = fqn.substring(0, fqn.indexOf("<")).trim();
    }
    String rel = baseFqn.replace('.', '/') + ".java";
    List<Path> candidates = List.of(
        projectRoot.resolve("src/main/java"),
        projectRoot.resolve("src/test/java"),
        currentSourceRoot != null ? currentSourceRoot : projectRoot
    );
    for (Path root : candidates) {
        Path p = root.resolve(rel);
        if (Files.exists(p)) return Optional.of(p);
    }
    return Optional.empty();
  }
}
