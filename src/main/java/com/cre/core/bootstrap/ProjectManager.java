package com.cre.core.bootstrap;

import com.cre.core.exception.CreException;
import com.cre.core.exception.ProjectNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Manages multiple {@link CreContext} instances with a TTL cache.
 * Supports multi-project 24/7 runtime.
 */
@Service
public final class ProjectManager {

  private static final Logger log = LoggerFactory.getLogger(ProjectManager.class);
  private static final Duration TTL = Duration.ofHours(2);

  private final Map<Path, CachedContext> cache = new ConcurrentHashMap<>();

  public ProjectManager() {
  }

  public CreContext getContext(Path projectRoot) throws CreException {
    return getContext(projectRoot, true);
  }

  public CreContext getContext(Path projectRoot, boolean pluginsEnabled) throws CreException {
    Objects.requireNonNull(projectRoot, "projectRoot must not be null");
    Path absRoot = projectRoot.toAbsolutePath().normalize();
    
    if (!Files.exists(absRoot) || !Files.isDirectory(absRoot)) {
      throw new ProjectNotFoundException(absRoot);
    }

    CachedContext cached = cache.get(absRoot);
    
    if (cached != null && !cached.isExpired()) {
      return cached.context();
    }

    synchronized (this) {
      // Re-check after lock
      cached = cache.get(absRoot);
      if (cached != null && !cached.isExpired()) {
        return cached.context();
      }

      log.info("Indexing project at: {}", absRoot);
      long start = System.currentTimeMillis();
      try {
        CreContext ctx = CreContext.fromDirectory(absRoot, pluginsEnabled);
        long end = System.currentTimeMillis();
        log.info("Indexing completed in {} ms for project: {}", (end - start), absRoot);
        
        cache.put(absRoot, new CachedContext(ctx, Instant.now()));
        return ctx;
      } catch (IOException e) {
        throw new CreException("Failed to index project: " + absRoot, e);
      }
    }
  }

  public void resetContext(Path projectRoot) {
    Objects.requireNonNull(projectRoot, "projectRoot must not be null");
    Path absRoot = projectRoot.toAbsolutePath().normalize();
    cache.remove(absRoot);
    log.info("Context reset for project: {}", absRoot);
  }

  private record CachedContext(CreContext context, Instant createdAt) {
    boolean isExpired() {
      return Instant.now().isAfter(createdAt.plus(TTL));
    }
  }
}
