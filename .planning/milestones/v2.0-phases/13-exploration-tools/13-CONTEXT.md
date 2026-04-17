# Phase 13: Exploration Tools — Context & Decisions

## Objective
Implement a set of tools to enable agents to discover the structure of a project and its individual files before requesting full context.

## Decisions

### 1. Project Exploration (`get_project_structure`)
- **Format**: Returns a formatted text tree (e.g., like the `tree` command) showing the directory and package structure of the project.
- **Options**: Should support filtering by folder/package path.
- **Rationale**: Direct readability for agents and humans is prioritized over machine-parsable JSON for this high-level view.

### 2. File Structure Extraction (`get_file_structure`)
- **Format**: Returns a "skeleton" version of the file, showing classes, properties, and method signatures without the implementation bodies.
- **Example**:
  ```java
  @RestController
  @RequestMapping("/api/users")
  public class UserController {
      private UserService service;

      @PostMapping("")
      public ResponseEntity<UserDto> create(SaveUserDto request, Authentication authentication);
  }
  ```
- **Annotations**: Included by default to provide a high-fidelity skeleton.
- **Identifiers**: ALL `NodeId` logic is removed. The system will no longer use or expose the `fqn::signature::origin` format. Tools will interact using human-readable symbols (e.g., `UserController`, `UserService.save`).
- **Rationale**: Simplifies the API for both agents and humans, reducing token overhead and cognitive load.
- **Options**:
  - `include_params`: Whether to show method parameters (default: true).
  - `include_annotations`: Whether to show annotations (default: true).

### 3. Ingestion & Indexing (`index_project`)
- **Trigger**: Automatic and blocking. If any exploration tool is called on an unindexed project, the engine will index it first.
- **Rationale**: Simplifies the agent workflow by removing the need for a separate `index` step.

### 4. Technical Implementation
- **AST Parsing**: Leverage `JavaAstIndexer` and `JavaParser` to extract the skeleton without logic.
- **Graph Engine**: Use the existing graph engine to find nodes belonging to a file if needed, but the primary source for `get_file_structure` will be a fresh (or cached) AST parse.

## Open Questions / Gray Areas (Deferred to Planning)
- Performance of automatic indexing for very large projects (we'll assume standard Spring Boot project sizes for now).
- Handling of non-Java files in `get_project_structure` (likely just list them by name).

## Next Steps
- Research specific `JavaParser` visitor patterns for skeleton extraction.
- Plan the implementation of `CreService` methods for these tools.
