# Phase 23 Research: Modern Java Features

## Java Records
Records are a special kind of class in Java.
JavaParser handles them as `RecordDeclaration`.
Currently, `JavaAstIndexer` uses `cu.getTypes()` and checks for `ClassOrInterfaceDeclaration`.
I need to update it to also handle `RecordDeclaration`.

Example:
```java
public record UserRecord(String id, String name) {}
```
This should be indexed as a node, and its components (`id`, `name`) can be USES_FIELD or similar.

## Sealed Classes
Sealed classes and interfaces restrict which other classes or interfaces may extend or implement them.
They use the `permits` keyword.
JavaParser handles this via `getPermittedTypes()`.

Example:
```java
public sealed interface Shape permits Circle, Square {}
```
This should be indexed, and we can add edges between the sealed type and its permitted types.

## Implementation Verification
I will update `JavaAstIndexer` to support `RecordDeclaration` and verify with a test fixture.
I'll also check if `ClassOrInterfaceDeclaration.getPermittedTypes()` needs special handling for indexing.
