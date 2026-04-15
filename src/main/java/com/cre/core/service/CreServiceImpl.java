package com.cre.core.service;

import com.cre.core.ast.AstUtils;
import com.cre.core.bootstrap.CreContext;
import com.cre.core.bootstrap.ProjectManager;
import com.cre.core.exception.CreException;
import com.cre.core.exception.SymbolNotFoundException;
import com.cre.core.graph.GraphEngine;
import com.cre.core.graph.model.EdgeType;
import com.cre.core.graph.model.GraphEdge;
import com.cre.core.graph.model.GraphNode;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CreServiceImpl implements CreService {

  private static final Logger log = LoggerFactory.getLogger(CreServiceImpl.class);
  private static final int MAX_GATHER_NODES = 100;

  private final ProjectManager projectManager;
  private final ContextPostProcessor postProcessor;

  public CreServiceImpl(ProjectManager projectManager, ContextPostProcessor postProcessor) {
    this.projectManager = Objects.requireNonNull(projectManager, "projectManager must not be null");
    this.postProcessor = Objects.requireNonNull(postProcessor, "postProcessor must not be null");
  }

  @Override
  public String getContext(Path projectRoot, String symbol, int depth, ContextOptions options) throws CreException {
    Objects.requireNonNull(projectRoot, "projectRoot must not be null");
    Objects.requireNonNull(symbol, "symbol must not be null");
    
    CreContext ctx = projectManager.getContext(projectRoot);
    GraphEngine graph = ctx.graph();
    
    String startNode = resolveNodeId(graph, symbol)
        .orElseThrow(() -> new SymbolNotFoundException(symbol));
    List<String> gathered = gatherNodesOrdered(graph, startNode, depth);
    
    try {
      String integratedView = buildIntegratedView(ctx, gathered, options != null ? options : ContextOptions.defaultOptions());
      return postProcessor.process(integratedView);
    } catch (IOException e) {
      throw new CreException("Failed to build integrated view", e);
    }
  }

  @Override
  public String expand(Path projectRoot, String symbol) throws CreException {
    return getContext(projectRoot, symbol, 1, ContextOptions.defaultOptions());
  }

  @Override
  public void resetProject(Path projectRoot) {
    Objects.requireNonNull(projectRoot, "projectRoot must not be null");
    projectManager.resetContext(projectRoot);
  }

  @Override
  public String getProjectStructure(Path projectRoot) throws CreException {
    Objects.requireNonNull(projectRoot, "projectRoot must not be null");
    Path absRoot = projectRoot.toAbsolutePath().normalize();
    projectManager.getContext(absRoot);
    StringBuilder sb = new StringBuilder();
    sb.append("Project Structure: ").append(absRoot).append("\n");
    try {
      walkDirectory(absRoot, "", sb);
    } catch (IOException e) {
      throw new CreException("Failed to walk project directory", e);
    }
    return sb.toString();
  }

  private void walkDirectory(Path dir, String prefix, StringBuilder sb) throws IOException {
    try (Stream<Path> stream = Files.list(dir)) {
      List<Path> files = stream
          .filter(p -> !p.getFileName().toString().startsWith("."))
          .filter(p -> !isExcluded(p))
          .sorted(Comparator.comparing(p -> !Files.isDirectory(p)))
          .toList();

      for (int i = 0; i < files.size(); i++) {
        Path p = files.get(i);
        boolean isLast = (i == files.size() - 1);
        sb.append(prefix).append(isLast ? "└── " : "├── ").append(p.getFileName()).append("\n");
        if (Files.isDirectory(p)) {
          walkDirectory(p, prefix + (isLast ? "    " : "│   "), sb);
        }
      }
    }
  }

  private boolean isExcluded(Path p) {
    String name = p.getFileName().toString();
    return Set.of("target", "build", "node_modules", ".git", ".idea").contains(name);
  }

  @Override
  public String getFileStructure(Path projectRoot, String symbol) throws CreException {
    Objects.requireNonNull(projectRoot, "projectRoot must not be null");
    Objects.requireNonNull(symbol, "symbol must not be null");
    
    CreContext ctx = projectManager.getContext(projectRoot);
    String nodeId = resolveNodeId(ctx.graph(), symbol)
        .orElseThrow(() -> new SymbolNotFoundException(symbol));
    
    String fqn = nodeId.contains("::") ? nodeId.split("::")[0] : nodeId;
    Path filePath = findFilePathForFqn(projectRoot, fqn)
        .orElseThrow(() -> new CreException("Source file not found for FQN: " + fqn));
    
    try {
      String source = Files.readString(filePath);
      CompilationUnit cu = AstUtils.JAVA_PARSER.parse(source).getResult()
          .orElseThrow(() -> new CreException("Failed to parse " + filePath));

      pruneComments(cu);
      cu.findAll(MethodDeclaration.class).forEach(MethodDeclaration::removeBody);
      cu.findAll(ConstructorDeclaration.class).forEach(cd -> cd.setBody(new com.github.javaparser.ast.stmt.BlockStmt()));

      return cu.toString();
    } catch (IOException e) {
      throw new CreException("Failed to read source file: " + filePath, e);
    }
  }

  Optional<Path> findFilePathForFqn(Path projectRoot, String fqn) {
    String baseFqn = fqn;
    if (fqn.contains("<")) {
      baseFqn = fqn.substring(0, fqn.indexOf("<")).trim();
    }
    String rel = baseFqn.replace('.', '/') + ".java";
    Path main = projectRoot.resolve("src/main/java").resolve(rel);
    if (Files.exists(main)) return Optional.of(main);
    Path test = projectRoot.resolve("src/test/java").resolve(rel);
    if (Files.exists(test)) return Optional.of(test);
    
    // Fallback to project root direct resolution (original logic)
    Path fallback = projectRoot.resolve(rel);
    return Files.exists(fallback) ? Optional.of(fallback) : Optional.empty();
  }

  private Optional<String> resolveNodeId(GraphEngine graph, String raw) {
    if (graph.node(raw) != null) return Optional.of(raw);
    List<String> matches = graph.nodes().keySet().stream()
        .filter(id -> matchesSymbol(id, raw))
        .toList();
    if (matches.isEmpty()) return Optional.empty();
    if (matches.size() > 1) {
      return Optional.of(matches.stream()
          .filter(id -> id.equals(raw) || id.startsWith(raw + "::"))
          .findFirst().orElse(matches.get(0)));
    }
    return Optional.of(matches.get(0));
  }

  private boolean matchesSymbol(String id, String symbol) {
    String fqn = id.contains("::") ? id.split("::")[0] : id;
    String simpleName = fqn.substring(fqn.lastIndexOf('.') + 1);
    if (symbol.equals(fqn) || symbol.equals(simpleName)) return true;
    if (id.contains("::")) {
      String signature = id.split("::")[1];
      String methodName = signature.contains("(") ? signature.substring(0, signature.indexOf('(')) : signature;
      return symbol.equals(simpleName + "." + methodName) || symbol.equals(fqn + "." + methodName) ||
             symbol.equals(simpleName + "::" + methodName) || symbol.equals(fqn + "::" + methodName);
    }
    return false;
  }

  private List<String> gatherNodesOrdered(GraphEngine graph, String start, int maxDepth) {
    List<String> result = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    Map<String, Integer> distances = new HashMap<>();
    Queue<String> queue = new ArrayDeque<>();

    if (graph.node(start) != null) {
      queue.add(start);
      visited.add(start);
      distances.put(start, 0);
    }

    while (!queue.isEmpty()) {
      String current = queue.poll();
      result.add(current);
      int d = distances.get(current);

      if (d < maxDepth && visited.size() < MAX_GATHER_NODES) {
        for (GraphEdge edge : graph.edgesFrom(current)) {
          // Strict filtering for context reconstruction traversal
          if (edge.type() != EdgeType.CALLS && edge.type() != EdgeType.USES_FIELD && 
              edge.type() != EdgeType.IMPLEMENTS && edge.type() != EdgeType.DEPENDS_ON) {
            continue;
          }
          
          String to = edge.to();
          if (!visited.contains(to)) {
            visited.add(to);
            distances.put(to, d + 1);
            queue.add(to);
          }
        }

        // Implementation traversal for Interfaces/Abstract classes
        String fqn = current.contains("::") ? current.split("::")[0] : current;
        String signature = current.contains("::") ? current.split("::")[1] : null;
        
        for (String implementor : graph.implementationsOf(fqn)) {
          String targetId = (signature != null) ? implementor + "::" + signature : implementor;
          if (!visited.contains(targetId)) {
            visited.add(targetId);
            distances.put(targetId, d + 1);
            queue.add(targetId);
          }
        }
      }
    }
    return result;
  }

  private String buildIntegratedView(CreContext ctx, List<String> gathered, ContextOptions options) throws IOException {
    Path projectRoot = ctx.projectRoot();
    Set<String> visitedTypes = new HashSet<>();
    StringBuilder sb = new StringBuilder();

    for (String nodeId : gathered) {
      String typeFqn = nodeId.contains("::") ? nodeId.split("::")[0] : nodeId;
      if (visitedTypes.contains(typeFqn)) continue;
      visitedTypes.add(typeFqn);

      findFilePathForFqn(projectRoot, typeFqn).ifPresent(path -> {
        try {
          String source = Files.readString(path);
          CompilationUnit cu = AstUtils.JAVA_PARSER.parse(source).getResult()
              .orElseThrow(() -> new RuntimeException("Failed to parse " + path));

          LexicalPreservingPrinter.setup(cu);
          pruneComments(cu);
          Set<String> markers = transformWithRelevance(cu, typeFqn, gathered, options);

          String code = LexicalPreservingPrinter.print(cu);
          code = code.replaceAll("int\\s+CRE_OM_PROPS\\s*=\\s*0;", "<omitted_properties/>");
          code = code.replaceAll("int\\s+CRE_OM_FUNCS\\s*=\\s*0;", "<omitted_functions/>");

          StringBuilder wrapped = new StringBuilder();
          wrapped.append("<file name=\"").append(typeFqn).append("\">\n");
          if (markers.contains("CRE_OM_IMPS")) wrapped.append("<omitted_imports/>\n");
          wrapped.append(code.trim());
          wrapped.append("\n</file>");

          sb.append(wrapped.toString()).append("\n\n");
        } catch (IOException e) {
          log.error("Failed to read or parse source file: {}", path, e);
        }
      });
    }
    return sb.toString().trim();
  }

  private void pruneComments(CompilationUnit cu) {
    List<Comment> comments = cu.getAllContainedComments();
    for (Comment comment : comments) {
      if (!(comment instanceof JavadocComment)) {
        comment.remove();
      }
    }
  }

  private Set<String> transformWithRelevance(CompilationUnit cu, String typeFqn, List<String> gathered, ContextOptions options) {
    Set<String> retainedNodes = new HashSet<>(gathered);
    Set<String> markers = new HashSet<>();
    UsageVisitor usage = new UsageVisitor();

    Set<BodyDeclaration<?>> toKeep = new HashSet<>();
    boolean hasAnyMethodGathered = false;
    for (TypeDeclaration<?> td : cu.getTypes()) {
      if (td instanceof ClassOrInterfaceDeclaration cid) {
        String cidFqn = cid.getFullyQualifiedName().orElse(cid.getNameAsString());
        // Exact match or endsWith for local/nested classes
        if (cidFqn.equals(typeFqn) || typeFqn.endsWith("." + cid.getNameAsString())) {
          for (BodyDeclaration<?> member : cid.getMembers()) {
            if (member instanceof MethodDeclaration md) {
              String mid = typeFqn + "::" + methodSignature(md);
              String fullSymbol = typeFqn + "." + md.getNameAsString();
              boolean matched = retainedNodes.contains(mid);
              
              // Fallback: match by name only if this class is relevant but the specific signature wasn't found
              // This helps if there were minor resolution issues during indexing
              if (!matched && retainedNodes.stream().anyMatch(rn -> rn.startsWith(typeFqn + "::" + md.getNameAsString() + "("))) {
                  matched = true;
              }

              if (matched || options.expandedFunctions().contains(fullSymbol) || options.functions() == ContextOptions.DefinitionLevel.FULL) {
                toKeep.add(member);
                usage.inspect(member);
                hasAnyMethodGathered = true;
              }
            } else if (member instanceof ConstructorDeclaration cd) {
              String cid_id = typeFqn + "::" + constructorSignature(cd);
              if (retainedNodes.contains(cid_id) || options.functions() == ContextOptions.DefinitionLevel.FULL) {
                toKeep.add(member);
                usage.inspect(member);
                hasAnyMethodGathered = true;
              }
            } else if (member instanceof FieldDeclaration fd) {
              for (VariableDeclarator v : fd.getVariables()) {
                String fieldId = typeFqn + "::field:" + v.getNameAsString();
                if (retainedNodes.contains(fieldId) || options.properties() == ContextOptions.DefinitionLevel.FULL) {
                  toKeep.add(member);
                  usage.inspect(member);
                }
              }
            }
          }
        }
      }
    }

    // Default show all properties for "Parameter Relevance Class" (Type node was gathered, but no methods)
    if (!hasAnyMethodGathered && retainedNodes.contains(typeFqn) && options.properties() == ContextOptions.DefinitionLevel.RELEVANCE) {
        for (TypeDeclaration<?> td : cu.getTypes()) {
            if (td instanceof ClassOrInterfaceDeclaration cid) {
                String cidFqn = cid.getFullyQualifiedName().orElse(cid.getNameAsString());
                if (cidFqn.equals(typeFqn) || typeFqn.endsWith("." + cid.getNameAsString())) {
                    for (BodyDeclaration<?> member : cid.getMembers()) {
                        if (member instanceof FieldDeclaration) {
                            toKeep.add(member);
                        }
                    }
                }
            }
        }
    }

    // Second pass for Relevance properties: Keep fields that were used by the kept methods/constructors
    if (options.properties() == ContextOptions.DefinitionLevel.RELEVANCE) {
        for (TypeDeclaration<?> td : cu.getTypes()) {
            if (td instanceof ClassOrInterfaceDeclaration cid) {
                for (BodyDeclaration<?> member : cid.getMembers()) {
                    if (member instanceof FieldDeclaration fd) {
                        for (VariableDeclarator v : fd.getVariables()) {
                            if (usage.getUsedFields().contains(v.getNameAsString())) {
                                toKeep.add(member);
                            }
                        }
                    }
                }
            }
        }
    }

    for (TypeDeclaration<?> td : cu.getTypes()) {
      if (td instanceof ClassOrInterfaceDeclaration cid) {
        List<BodyDeclaration<?>> members = new ArrayList<>(cid.getMembers());
        boolean hasPrunedFuncs = false;
        boolean hasPrunedProps = false;
        FieldDeclaration firstPrunedField = null;

        for (BodyDeclaration<?> m : members) {
          if (m instanceof MethodDeclaration || m instanceof ConstructorDeclaration) {
            if (!toKeep.contains(m)) {
              m.remove();
              hasPrunedFuncs = true;
            }
          } else if (m instanceof FieldDeclaration fd) {
            if (!toKeep.contains(m)) {
              if (firstPrunedField == null) firstPrunedField = fd;
              else fd.remove();
              hasPrunedProps = true;
            }
          }
        }

        if (hasPrunedProps && options.properties() != ContextOptions.DefinitionLevel.FULL) {
          if (firstPrunedField != null) firstPrunedField.replace(createMarker("CRE_OM_PROPS"));
          else cid.addMember(createMarker("CRE_OM_PROPS"));
        }
        
        // Inspect KEPT fields for types
        for (BodyDeclaration<?> m : cid.getMembers()) {
            if (m instanceof FieldDeclaration fd && !fd.getVariables().isEmpty()) {
                // If it wasn't replaced by a marker, it's kept
                if (!fd.getVariable(0).getNameAsString().equals("CRE_OM_PROPS")) {
                    usage.inspect(fd);
                }
            }
        }

        if (hasPrunedFuncs && options.functions() != ContextOptions.DefinitionLevel.FULL) {
          cid.addMember(createMarker("CRE_OM_FUNCS"));
        }
      }
    }

    if (options.imports() != ContextOptions.DefinitionLevel.FULL) {
      List<ImportDeclaration> imports = new ArrayList<>(cu.getImports());
      boolean hasPrunedImps = false;
      for (ImportDeclaration imp : imports) {
        boolean keep = false;
        if (options.imports() == ContextOptions.DefinitionLevel.RELEVANCE) {
          String name = imp.getNameAsString();
          String lastPart = name.substring(name.lastIndexOf('.') + 1);
          if (usage.getUsedTypes().contains(lastPart) || usage.getUsedTypes().contains(name)) keep = true;
        }
        if (!keep) {
          imp.remove();
          hasPrunedImps = true;
        }
      }
      if (hasPrunedImps && options.imports() == ContextOptions.DefinitionLevel.OMITTED) {
        markers.add("CRE_OM_IMPS");
      }
    }
    return markers;
  }

  private FieldDeclaration createMarker(String name) {
    FieldDeclaration marker = new FieldDeclaration();
    marker.addVariable(new VariableDeclarator(
        new com.github.javaparser.ast.type.PrimitiveType(com.github.javaparser.ast.type.PrimitiveType.Primitive.INT),
        name).setInitializer(new com.github.javaparser.ast.expr.IntegerLiteralExpr("0")));
    return marker;
  }

  private String methodSignature(MethodDeclaration md) {
    String params = md.getParameters().stream()
        .map(p -> p.getType().asString())
        .collect(Collectors.joining(","));
    return md.getNameAsString() + "(" + params + ")";
  }

  private String constructorSignature(ConstructorDeclaration cd) {
    String params = cd.getParameters().stream()
        .map(p -> p.getType().asString())
        .collect(Collectors.joining(","));
    return cd.getNameAsString() + "(" + params + ")";
  }
}
