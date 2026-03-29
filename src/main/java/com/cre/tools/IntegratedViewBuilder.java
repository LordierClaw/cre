package com.cre.tools;

import com.cre.core.ast.AstUtils;
import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.NodeId;
import com.cre.core.ast.JavaAstIndexer;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IntegratedViewBuilder {

  public String build(
      Set<NodeId> retained,
      Set<NodeId> pruned,
      GraphEngine graph,
      Path javaSourceRoot) {
    
    Set<String> allOrigins = new HashSet<>();
    retained.forEach(id -> allOrigins.add(id.sourceOrigin()));
    pruned.forEach(id -> allOrigins.add(id.sourceOrigin()));

    StringBuilder sb = new StringBuilder();
    Map<String, String> nodeIdMap = new HashMap<>();
    Map<NodeId, String> reverseMap = new HashMap<>();

    // We need a first pass to collect all short IDs into nodeIdMap
    // so we can print the map at the top.
    // However, to keep it simple and one-pass-ish, we can just process files
    // and then prepend the map.
    
    StringBuilder filesContent = new StringBuilder();

    for (String origin : allOrigins) {
      Path path = javaSourceRoot.resolve(origin);
      if (!Files.exists(path)) {
        continue;
      }

      try {
        String source = Files.readString(path);
        CompilationUnit cu = AstUtils.JAVA_PARSER.parse(source).getResult()
            .orElseThrow(() -> new RuntimeException("Failed to parse " + path));

        LexicalPreservingPrinter.setup(cu);

        boolean hasPrunedImports = transform(cu, origin, retained, pruned, nodeIdMap, reverseMap, javaSourceRoot);

        String code = LexicalPreservingPrinter.print(cu);
        
        // 1. Wrap each class in its own tag
        List<TypeDeclaration> types = cu.findAll(TypeDeclaration.class);
        types.sort((a, b) -> Integer.compare(LexicalPreservingPrinter.print(b).length(), LexicalPreservingPrinter.print(a).length()));
        for (TypeDeclaration<?> td : types) {
            String typeCode = LexicalPreservingPrinter.print(td);
            String tagName = td.getNameAsString();
            String wrapped = "<" + tagName + ">\n" + typeCode + "\n</" + tagName + ">";
            code = code.replace(typeCode, wrapped);
        }

        // 2. Handle pruned imports marker
        if (hasPrunedImports) {
          if (code.contains("import ")) {
            code = code.replaceFirst("(?s)(.*import [^;]+;)", "$1\n\n<ommitted_import/>");
          } else if (code.contains("package ")) {
            code = code.replaceFirst("(?s)(.*package [^;]+;)", "$1\n\n<ommitted_import/>");
          } else {
            code = "<ommitted_import/>\n\n" + code;
          }
        }

        // 3. Regex replacement for markers
        code = code.replaceAll("int\\s+CRE_OM_PROPS;", "<ommitted_properties/>");
        code = code.replaceAll("int\\s+CRE_OM_FUNCS;", "<ommitted_functions/>");
        code = code.replaceAll("CRE_OM_CODE_(ommitted_\\d+)", "<ommitted_code id=\"$1\" description=\"\"/>");
        
        code = code.replaceAll("int\\s+CRE_OMITTED_(ommitted_\\d+);", "<$1/>");
        code = code.replaceAll("CRE_OMITTED_(ommitted_\\d+)", "<$1/>");

        filesContent.append("<file origin=\"").append(origin).append("\">\n")
            .append(code).append("\n</file>\n\n");
      } catch (IOException e) {
        throw new RuntimeException("Error reading file: " + path, e);
      }
    }

    if (!nodeIdMap.isEmpty()) {
      sb.append("<node_id_map>\n");
      nodeIdMap.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n"));
      sb.append("</node_id_map>\n\n");
    }
    
    sb.append(filesContent);
    return sb.toString().trim();
  }

  private boolean transform(
      CompilationUnit cu,
      String origin,
      Set<NodeId> retained,
      Set<NodeId> pruned,
      Map<String, String> nodeIdMap,
      Map<NodeId, String> reverseMap,
      Path javaSourceRoot) {
    
    // Import pruning logic
    List<ImportDeclaration> imports = new ArrayList<>(cu.getImports());
    boolean hasPrunedImports = false;
    for (ImportDeclaration imp : imports) {
      String name = imp.getNameAsString();
      boolean isPruned = pruned.stream().anyMatch(p -> p.fullyQualifiedType().equals(name) || p.fullyQualifiedType().startsWith(name + "."));
      if (isPruned) {
        imp.remove();
        hasPrunedImports = true;
      }
    }

    for (TypeDeclaration<?> td : cu.getTypes()) {
      if (td instanceof ClassOrInterfaceDeclaration cid) {
        transformType(cid, cu, origin, retained, pruned, nodeIdMap, reverseMap, javaSourceRoot);
      }
    }
    return hasPrunedImports;
  }

  private void transformType(
      ClassOrInterfaceDeclaration decl,
      CompilationUnit cu,
      String origin,
      Set<NodeId> retained,
      Set<NodeId> pruned,
      Map<String, String> nodeIdMap,
      Map<NodeId, String> reverseMap,
      Path javaSourceRoot) {
    
    String fqName = resolveTypeFqName(decl, cu);
    
    // 1. Transform method calls in retained methods/constructors FIRST
    for (BodyDeclaration<?> member : decl.getMembers()) {
      if (member instanceof MethodDeclaration md) {
        NodeId id = new NodeId(fqName, JavaAstIndexer.methodSignature(md), origin);
        if (retained.contains(id)) {
          transformMethodCalls(md, decl, cu, origin, fqName, pruned, nodeIdMap, reverseMap, javaSourceRoot);
        }
      } else if (member instanceof ConstructorDeclaration cd) {
        NodeId id = new NodeId(fqName, constructorSignature(cd), origin);
        if (retained.contains(id)) {
          transformMethodCalls(cd, decl, cu, origin, fqName, pruned, nodeIdMap, reverseMap, javaSourceRoot);
        }
      } else if (member instanceof ClassOrInterfaceDeclaration nested) {
        NodeId id = new NodeId(resolveTypeFqName(nested, cu), "<type>", origin);
        if (retained.contains(id)) {
          transformType(nested, cu, origin, retained, pruned, nodeIdMap, reverseMap, javaSourceRoot);
        }
      }
    }

    // 2. Replace non-retained members with markers
    List<BodyDeclaration<?>> members = new ArrayList<>(decl.getMembers());
    boolean hasPrunedFields = false;
    FieldDeclaration firstPrunedField = null;
    boolean hasPrunedMethods = false;

    for (BodyDeclaration<?> member : members) {
      if (member instanceof MethodDeclaration md) {
        NodeId id = new NodeId(fqName, JavaAstIndexer.methodSignature(md), origin);
        if (!retained.contains(id)) {
          hasPrunedMethods = true;
          md.remove();
        }
      } else if (member instanceof ConstructorDeclaration cd) {
        NodeId id = new NodeId(fqName, constructorSignature(cd), origin);
        if (!retained.contains(id)) {
          hasPrunedMethods = true;
          cd.remove();
        }
      } else if (member instanceof FieldDeclaration fd) {
        boolean anyRetained = false;
        for (VariableDeclarator v : fd.getVariables()) {
          NodeId id = new NodeId(fqName, "field:" + v.getNameAsString(), origin);
          if (retained.contains(id)) {
            anyRetained = true;
          }
        }
        if (!anyRetained) {
          hasPrunedFields = true;
          if (firstPrunedField == null) {
            firstPrunedField = fd;
          } else {
            fd.remove();
          }
        }
      } else if (member instanceof ClassOrInterfaceDeclaration nested) {
        NodeId id = new NodeId(resolveTypeFqName(nested, cu), "<type>", origin);
        if (!retained.contains(id)) {
          String shortId = getShortId(id, nodeIdMap, reverseMap);
          replaceWithMarker(nested, shortId);
        }
      }
    }

    if (hasPrunedFields && firstPrunedField != null) {
      FieldDeclaration marker = new FieldDeclaration();
      marker.addVariable(new VariableDeclarator(
          new com.github.javaparser.ast.type.PrimitiveType(com.github.javaparser.ast.type.PrimitiveType.Primitive.INT),
          "CRE_OM_PROPS"));
      firstPrunedField.replace(marker);
    }

    if (hasPrunedMethods) {
      FieldDeclaration marker = new FieldDeclaration();
      marker.addVariable(new VariableDeclarator(
          new com.github.javaparser.ast.type.PrimitiveType(com.github.javaparser.ast.type.PrimitiveType.Primitive.INT),
          "CRE_OM_FUNCS"));
      decl.addMember(marker);
    }
  }

  private void replaceWithMarker(BodyDeclaration<?> member, String shortId) {
    FieldDeclaration marker = new FieldDeclaration();
    marker.addVariable(new VariableDeclarator(
        new com.github.javaparser.ast.type.PrimitiveType(com.github.javaparser.ast.type.PrimitiveType.Primitive.INT),
        "CRE_OMITTED_" + shortId));
    member.replace(marker);
  }

  private void transformMethodCalls(
      com.github.javaparser.ast.body.CallableDeclaration<?> callable,
      ClassOrInterfaceDeclaration clazz,
      CompilationUnit cu,
      String origin,
      String declaringFqName,
      Set<NodeId> pruned,
      Map<String, String> nodeIdMap,
      Map<NodeId, String> reverseMap,
      Path javaSourceRoot) {
    
    Map<String, String> paramTypes = new HashMap<>();
    for (Parameter p : callable.getParameters()) {
      paramTypes.put(p.getNameAsString(), p.getType().asString());
    }

    callable.findAll(MethodCallExpr.class).forEach(call -> {
      resolveCallee(call, clazz, cu, paramTypes, javaSourceRoot)
          .ifPresent(callee -> {
            if (pruned.contains(callee)) {
              String shortId = getShortId(callee, nodeIdMap, reverseMap);
              call.replace(new NameExpr("CRE_OM_CODE_" + shortId));
            }
          });
    });
  }

  public static String constructorSignature(ConstructorDeclaration cd) {
    String params =
        cd.getParameters().stream()
            .map(p -> p.getType().asString())
            .collect(Collectors.joining(","));
    return cd.getNameAsString() + "(" + params + ")";
  }

  private String getShortId(NodeId nodeId, Map<String, String> nodeIdMap, Map<NodeId, String> reverseMap) {
    String shortId = reverseMap.get(nodeId);
    if (shortId == null) {
      shortId = String.format("ommitted_%02d", reverseMap.size() + 1);
      reverseMap.put(nodeId, shortId);
      nodeIdMap.put(shortId, nodeId.toString());
    }
    return shortId;
  }

  // --- AST Resolution Helpers (largely copied from JavaAstIndexer) ---

  private String resolveTypeFqName(ClassOrInterfaceDeclaration decl, CompilationUnit cu) {
    return decl.getFullyQualifiedName().orElseGet(() -> {
      String simple = decl.getNameAsString();
      return resolveSimpleNameToFqn(cu, simple).orElse(simple);
    });
  }

  private Optional<String> resolveSimpleNameToFqn(CompilationUnit cu, String simpleName) {
    for (ImportDeclaration im : cu.getImports()) {
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

  private Optional<NodeId> resolveCallee(
      MethodCallExpr call,
      ClassOrInterfaceDeclaration clazz,
      CompilationUnit cu,
      Map<String, String> paramTypes,
      Path javaSourceRoot) {
    Optional<String> calleeTypeFqn =
        call.getScope().map(s -> resolveScopeTypeFqn(s, clazz, cu)).orElse(Optional.empty());

    if (calleeTypeFqn.isEmpty()) {
      // If no scope, it might be a method in the same class
      calleeTypeFqn = Optional.of(resolveTypeFqName(clazz, cu));
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
    return NodeId.normalizeOrigin(Path.of(typeFqn.replace('.', '/') + ".java"));
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
