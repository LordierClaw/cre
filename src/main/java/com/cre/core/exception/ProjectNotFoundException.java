package com.cre.core.exception;

import java.nio.file.Path;

/**
 * Thrown when a project root is missing or inaccessible.
 */
public class ProjectNotFoundException extends CreException {
  public ProjectNotFoundException(Path projectRoot) {
    super("Project not found or inaccessible: " + projectRoot);
  }
}
