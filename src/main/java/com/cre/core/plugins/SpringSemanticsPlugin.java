package com.cre.core.plugins;

import com.cre.core.ast.AstUtils;
import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.NodeId;
import com.cre.core.graph.model.EdgeType;
import com.cre.core.graph.model.GraphEdge;
import com.cre.core.graph.model.GraphNode;
import com.cre.core.graph.model.NodeKind;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SpringSemanticsPlugin implements GraphPlugin {

  private static final Set<String> CONTROLLER_ANNOS = Set.of("Controller", "RestController");
  private static final Set<String> SERVICE_ANNOS =
      Set.of("Service", "Repository", "Component");

  @Override
  public String pluginId() {
    return "spring-semantics";
  }

  @Override
  public void enrich(GraphEngine graph, Path javaSourceRoot, List<Path> javaFiles) {
    Map<String, NodeId> typeIds = new HashMap<>();
    Map<String, List<NodeId>> methodIdsByFqn = new HashMap<>();
    for (GraphNode n : graph.nodes().values()) {
      if (n.kind() == NodeKind.TYPE) {
        typeIds.put(n.id().fullyQualifiedType(), n.id());
      } else if (n.kind() == NodeKind.METHOD) {
        methodIdsByFqn.computeIfAbsent(n.id().fullyQualifiedType(), __ -> new java.util.ArrayList<>())
            .add(n.id());
      }
    }

    Set<String> controllerTypes = new HashSet<>();
    Set<String> serviceLayerTypes = new HashSet<>();
    Map<String, CompilationUnit> cusByTypeFqn = new HashMap<>();
    Map<String, ClassOrInterfaceDeclaration> declsByTypeFqn = new HashMap<>();

    for (Path p : javaFiles) {
      try {
        CompilationUnit cu = AstUtils.JAVA_PARSER.parse(p).getResult()
            .orElseThrow(() -> new RuntimeException("Failed to parse " + p));
        for (var td : cu.getTypes()) {
          if (td instanceof ClassOrInterfaceDeclaration cid) {
            String fqn = typeFqn(cu, cid);
            cusByTypeFqn.put(fqn, cu);
            declsByTypeFqn.put(fqn, cid);

            if (hasAnyAnnotation(cid.getAnnotations(), CONTROLLER_ANNOS)) {
              controllerTypes.add(fqn);
            }
            if (hasAnyAnnotation(cid.getAnnotations(), SERVICE_ANNOS) || hasBeanMethod(cid)) {
              serviceLayerTypes.add(fqn);
            }
          }
        }
      } catch (IOException e) {
        graph.springSemanticsState(true, false, "failed_to_parse:" + p);
        return;
      }
    }

    // If any service implementation is annotated, treat its interface(s) as service-layer too.
    for (String maybeIface : typeIds.keySet()) {
      List<NodeId> impls = graph.implementationsOf(maybeIface);
      if (impls.isEmpty()) {
        continue;
      }
      boolean anyServiceImpl =
          impls.stream().anyMatch(id -> serviceLayerTypes.contains(id.fullyQualifiedType()));
      if (anyServiceImpl) {
        serviceLayerTypes.add(maybeIface);
      }
    }

    // Emit ENTRY_POINT and SERVICE_LAYER edges.
    for (String controllerFqn : controllerTypes) {
      NodeId typeId = typeIds.get(controllerFqn);
      if (typeId == null) {
        continue;
      }
      for (NodeId m : methodIdsByFqn.getOrDefault(controllerFqn, List.of())) {
        graph.addEdge(new GraphEdge(typeId, m, EdgeType.ENTRY_POINT));
      }
    }

    for (String serviceFqn : serviceLayerTypes) {
      NodeId typeId = typeIds.get(serviceFqn);
      if (typeId == null) {
        continue;
      }
      for (NodeId m : methodIdsByFqn.getOrDefault(serviceFqn, List.of())) {
        graph.addEdge(new GraphEdge(typeId, m, EdgeType.SERVICE_LAYER));
      }
    }

    boolean complete = true;
    String missingBoundary = "";

    // Emit DEPENDS_ON edges via constructor injection (fail-closed).
    for (String controllerFqn : controllerTypes) {
      ClassOrInterfaceDeclaration decl = declsByTypeFqn.get(controllerFqn);
      CompilationUnit cu = cusByTypeFqn.get(controllerFqn);
      NodeId controllerTypeId = typeIds.get(controllerFqn);
      if (decl == null || cu == null || controllerTypeId == null) {
        continue;
      }

      for (ConstructorDeclaration ctor : decl.getConstructors()) {
        Map<String, String> paramTypeFqn = new HashMap<>();
        for (var p : ctor.getParameters()) {
          String fqn = resolveTypeToFqn(cu, p.getType());
          if (fqn != null) {
            paramTypeFqn.put(p.getNameAsString(), fqn);
          }
        }

        Set<String> wiredTargets = inferWiredParamNames(ctor);
        for (String paramName : wiredTargets) {
          String injectedFqn = paramTypeFqn.get(paramName);
          if (injectedFqn == null) {
            complete = false;
            missingBoundary = "wiring_target_type_not_resolved";
            continue;
          }
          NodeId injectedTypeId = typeIds.get(injectedFqn);
          if (injectedTypeId == null) {
            complete = false;
            missingBoundary = "wiring_target_type_node_missing";
            continue;
          }
          if (!serviceLayerTypes.contains(injectedFqn)) {
            complete = false;
            missingBoundary = "wiring_target_not_service_layer";
            continue;
          }
          graph.addEdge(new GraphEdge(controllerTypeId, injectedTypeId, EdgeType.DEPENDS_ON));
        }
      }
    }

    if (complete) {
      graph.springSemanticsState(true, true, "");
    } else {
      graph.springSemanticsState(true, false, missingBoundary.isBlank() ? "missing_spring_mapping" : missingBoundary);
    }
  }

  private static boolean hasAnyAnnotation(List<AnnotationExpr> annos, Set<String> names) {
    for (AnnotationExpr a : annos) {
      String n = a.getName().getIdentifier();
      if (names.contains(n)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasBeanMethod(ClassOrInterfaceDeclaration cid) {
    for (MethodDeclaration md : cid.getMethods()) {
      if (md.getAnnotations().stream().anyMatch(a -> "Bean".equals(a.getName().getIdentifier()))) {
        return true;
      }
    }
    return false;
  }

  private static String typeFqn(CompilationUnit cu, ClassOrInterfaceDeclaration cid) {
    String simple = cid.getNameAsString();
    return cu.getPackageDeclaration().map(pd -> pd.getNameAsString() + "." + simple).orElse(simple);
  }

  private static String resolveTypeToFqn(CompilationUnit cu, com.github.javaparser.ast.type.Type t) {
    if (!t.isClassOrInterfaceType()) {
      return null;
    }
    ClassOrInterfaceType ct = t.asClassOrInterfaceType();
    String simple = ct.getName().getIdentifier();
    Optional<String> fromImports =
        cu.getImports().stream()
            .filter(im -> !im.isStatic() && !im.isAsterisk())
            .map(im -> im.getNameAsString())
            .filter(full -> full.endsWith("." + simple))
            .findFirst();
    if (fromImports.isPresent()) {
      return fromImports.get();
    }
    return cu.getPackageDeclaration().map(pd -> pd.getNameAsString() + "." + simple).orElse(simple);
  }

  private static Set<String> inferWiredParamNames(ConstructorDeclaration ctor) {
    Set<String> wired = new HashSet<>();
    ctor.findAll(AssignExpr.class)
        .forEach(
            asg -> {
              String leftField = assignedThisField(asg.getTarget());
              String rightParam = nameExpr(asg.getValue());
              if (leftField != null && rightParam != null) {
                wired.add(rightParam);
              }
            });
    return wired;
  }

  private static String assignedThisField(Expression target) {
    if (target instanceof FieldAccessExpr fa) {
      if (fa.getScope() instanceof ThisExpr) {
        return fa.getNameAsString();
      }
    }
    return null;
  }

  private static String nameExpr(Expression value) {
    if (value instanceof NameExpr ne) {
      return ne.getNameAsString();
    }
    return null;
  }
}
