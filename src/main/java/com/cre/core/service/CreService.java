package com.cre.core.service;

import com.cre.core.exception.CreException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Central service for CRE core operations.
 */
public interface CreService {

  /**
   * Get a normalized context slice for a symbol (class or method).
   *
   * @param projectRoot The root directory of the project.
   * @param symbol      The class or method symbol (e.g., "UserController", "UserService::save").
   * @param depth       The depth of the context slice.
   * @param options     Granular options for context reconstruction.
   * @return A formatted context slice (XML-like).
   */
  String getContext(Path projectRoot, String symbol, int depth, ContextOptions options) throws CreException;

  /**
   * Expand a symbol into a bounded merged context slice.
   *
   * @param projectRoot The root directory of the project.
   * @param symbol      The class or method symbol to expand.
   * @return An expanded context slice.
   */
  String expand(Path projectRoot, String symbol) throws CreException;

  /**
   * Force re-indexing of a project root.
   *
   * @param projectRoot The root directory of the project.
   */
  void resetProject(Path projectRoot);

  /**
   * Get the project structure as a tree-like string.
   *
   * @param projectRoot The root directory of the project.
   * @return A formatted text tree of the project structure.
   */
  String getProjectStructure(Path projectRoot) throws CreException;

  /**
   * Get the structure of a specific file or class as a skeleton (no bodies).
   *
   * @param projectRoot The root directory of the project.
   * @param symbol      The class or file path symbol.
   * @return A code skeleton.
   */
  String getFileStructure(Path projectRoot, String symbol) throws CreException;
}
