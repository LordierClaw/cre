package com.cre.core.service;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.HashSet;
import java.util.Set;

/**
 * Visitor to collect used field names and type names from AST nodes.
 */
public class UsageVisitor extends VoidVisitorAdapter<Void> {
  private final Set<String> usedFields = new HashSet<>();
  private final Set<String> usedTypes = new HashSet<>();

  public void inspect(Node node) {
    node.accept(this, null);
  }

  public Set<String> getUsedFields() {
    return usedFields;
  }

  public Set<String> getUsedTypes() {
    return usedTypes;
  }

  @Override
  public void visit(FieldDeclaration n, Void arg) {
    n.getVariables().forEach(v -> {
        usedFields.add(v.getNameAsString());
        collectType(v.getType());
    });
    super.visit(n, arg);
  }

  @Override
  public void visit(MethodDeclaration n, Void arg) {
    collectType(n.getType());
    n.getParameters().forEach(p -> collectType(p.getType()));
    super.visit(n, arg);
  }

  @Override
  public void visit(Parameter n, Void arg) {
    collectType(n.getType());
    super.visit(n, arg);
  }

  private void collectType(com.github.javaparser.ast.type.Type t) {
    if (t.isClassOrInterfaceType()) {
        usedTypes.add(t.asClassOrInterfaceType().getNameWithScope());
        t.asClassOrInterfaceType().getTypeArguments().ifPresent(args -> {
            args.forEach(this::collectType);
        });
    }
  }

  @Override
  public void visit(NameExpr n, Void arg) {
    usedFields.add(n.getNameAsString());
    super.visit(n, arg);
  }

  @Override
  public void visit(FieldAccessExpr n, Void arg) {
    usedFields.add(n.getNameAsString());
    super.visit(n, arg);
  }

  @Override
  public void visit(ClassOrInterfaceType n, Void arg) {
    usedTypes.add(n.getNameWithScope());
    super.visit(n, arg);
  }

  @Override
  public void visit(ObjectCreationExpr n, Void arg) {
    usedTypes.add(n.getType().getNameWithScope());
    super.visit(n, arg);
  }

  @Override
  public void visit(MethodCallExpr n, Void arg) {
    // Collect types from scope if it's a static call or class ref
    n.getScope().ifPresent(s -> {
        if (s instanceof NameExpr ne) usedTypes.add(ne.getNameAsString());
    });
    super.visit(n, arg);
  }
}
