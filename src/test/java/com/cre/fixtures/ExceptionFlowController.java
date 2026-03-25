package com.cre.fixtures;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExceptionFlowController {

  private final UserService userService;

  public ExceptionFlowController(UserService userService) {
    this.userService = userService;
  }

  /** Try body is call-free; catch invokes {@code userService.getUser} for deterministic CATCH_INVOKES. */
  public String risky(String id) {
    try {
      return id;
    } catch (RuntimeException e) {
      return userService.getUser("recovery");
    }
  }
}
