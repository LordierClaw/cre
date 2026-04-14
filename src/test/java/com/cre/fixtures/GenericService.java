package com.cre.fixtures;

import java.util.List;
import java.util.Map;

public interface GenericService<T> {
    void process(T item);
    List<T> findAll();
    <S extends T> S save(S entity);
    void processAll(List<? extends T> items);
}
