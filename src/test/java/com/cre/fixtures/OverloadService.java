package com.cre.fixtures;

public class OverloadService {
    public void process(String s) {}
    public void process(Object o) {}
    public void process(Integer i) {}
    public void process(int i) {}
}
