package com.cre.fixtures;

import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

  @Override
  public String getUser(String id) {
    return id;
  }
}
