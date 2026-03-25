package com.cre.fixtures;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserControllerMissingServiceMapping {

  private final MissingService missingService;

  public UserControllerMissingServiceMapping(MissingService missingService) {
    this.missingService = missingService;
  }

  public String call(String id) {
    return missingService.work(id);
  }
}

