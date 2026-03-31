# Phase 15: Robustness & Exceptions — Context & Decisions

## Objective
Standardize error handling, improve null safety, and refine comment processing for better token efficiency and reliability.

## Decisions

### 1. Exception Handling
- **Custom Hierarchy**: We will introduce a base `CreException` (checked or unchecked?) and specific subclasses:
  - `ProjectNotFoundException`: When a requested project root is missing or inaccessible.
  - `SymbolNotFoundException`: When a class or method symbol cannot be resolved.
  - `IndexingException`: For failures during project ingestion.
- **MCP Integration**: All core methods in `CreService` will throw these custom exceptions, which the `CreController` and `McpConfig` will map to descriptive error messages for the agent.

### 2. Comment Processing
- **Policy**: Keep **Javadoc-style comments** (`/** ... */`) only.
- **Pruning**: All other inline comments (`//`) and block comments (`/* ... */`) will be discarded during context reconstruction.
- **Rationale**: Javadocs often contain valuable high-level semantic information for agents, while implementation-level comments (or commented-out code) are noisy and consume unnecessary tokens.
- **Technical implementation**: Use `JavaParser`'s comment removal capabilities before lexical printing or use a post-processor to filter them.

### 3. Null Safety & Robustness
- **Policy**: 
  - Use `java.util.Optional` for all methods that might return a null value (e.g., finding a node, resolving a file path).
  - Use `java.util.Objects.requireNonNull` for defensive programming at method entry points for parameters that must not be null.
- **File System**: Re-verify file existence before reading, even if the file was present during indexing (handle `NoSuchFileException`).

### 4. Commented-out Code Handling
- By sticking to **Javadoc Only**, we naturally ignore most commented-out code blocks (which are usually block comments `/* */` or inline `//`). 
- If a function is "commented out" but kept in a Javadoc block (unlikely but possible), it will be treated as text.

## Technical Implementation Details
- **LexicalPreservingPrinter**: We need to ensure that comment removal doesn't break the lexical preservation if we want to keep specific formatting. If it does, we might need a two-pass approach: parse/strip comments -> print.

## Next Steps
- Define the `CreException` hierarchy.
- Research the best way to strip comments while keeping Javadocs in JavaParser.
- Refactor `CreServiceImpl` and `ProjectManager` for standardized exceptions and null safety.
