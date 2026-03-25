package com.cre.fixtures;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  public String getUser(String id) {
    return userService.getUser(id);
  }
}
