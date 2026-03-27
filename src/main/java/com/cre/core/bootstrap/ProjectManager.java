package com.cre.core.bootstrap;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
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
  private static ProjectManager INSTANCE;
  private static final Duration TTL = Duration.ofHours(2);

  private final Map<Path, CachedContext> cache = new ConcurrentHashMap<>();

  public ProjectManager() {
    INSTANCE = this;
  }

  /**
   * @deprecated Use dependency injection.
   */
  @Deprecated
  public static ProjectManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new ProjectManager();
    }
    return INSTANCE;
  }

  public CreContext getContext(Path projectRoot) throws IOException {
    return getContext(projectRoot, true);
  }

  public CreContext getContext(Path projectRoot, boolean pluginsEnabled) throws IOException {
    Path absRoot = projectRoot.toAbsolutePath().normalize();
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
      CreContext ctx = CreContext.fromDirectory(absRoot, pluginsEnabled);
      long end = System.currentTimeMillis();
      log.info("Indexing completed in {} ms for project: {}", (end - start), absRoot);
      
      cache.put(absRoot, new CachedContext(ctx, Instant.now()));
      return ctx;
    }
  }

  public void resetContext(Path projectRoot) {
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
