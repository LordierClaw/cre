# Phase 22 Research: Polymorphism & Inheritance

## Inheritance Support
`JavaSymbolSolver` automatically traverses the hierarchy when `resolve()` is called on a method. If a method is defined in a parent class but called on a child, `rmd.declaringType().getQualifiedName()` will return the PARENT class name.

Example:
```java
class Parent { void hello() {} }
class Child extends Parent {}
// call on child.hello()
// rmd.declaringType().getQualifiedName() -> "Parent"
```
This is excellent for accuracy as it links directly to where the code is defined.

## Interface and Abstract Calls
When calling a method on an interface, Symbol Solver returns the interface's method declaration.
To support `Expand-on-demand`, CRE should ideally also know about implementations. However, current v3 scope focuses on *accurate resolution* of the call to its definition.

## Graph Representation
I will verify that `JavaAstIndexer` correctly records edges from the caller to the parent class method ID.

## Implementation Verification
I will create a multi-level inheritance fixture and verify the edges in the graph.
