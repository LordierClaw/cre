package com.cre.testsupport;

import com.cre.core.bootstrap.CreContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Loads default fixtures plus {@link com.cre.fixtures.ExceptionFlowController} for exception-flow tests. */
public final class ExceptionFlowTestSupport {

  private ExceptionFlowTestSupport() {}

  public static CreContext load(boolean pluginsEnabled) throws IOException {
    Path root = Path.of(System.getProperty("user.dir", "."));
    Path javaRoot = root.resolve("src/test/java");
    Path[] files = {
      javaRoot.resolve("com/cre/fixtures/UserService.java"),
      javaRoot.resolve("com/cre/fixtures/UserServiceImpl.java"),
      javaRoot.resolve("com/cre/fixtures/UserController.java"),
      javaRoot.resolve("com/cre/fixtures/ExceptionFlowController.java"),
    };
    if (!Arrays.stream(files).allMatch(Files::exists)) {
      throw new IllegalStateException("Fixture sources missing under src/test/java; cwd=" + root);
    }
    return CreContext.fromJavaSourceRoot(javaRoot, pluginsEnabled, files);
  }
}
