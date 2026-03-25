package com.cre.fixtures;

public class MissingServiceImpl implements MissingService {
  @Override
  public String work(String id) {
    return id;
  }
}

