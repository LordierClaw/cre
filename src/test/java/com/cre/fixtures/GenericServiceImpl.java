package com.cre.fixtures;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class GenericServiceImpl<T> implements GenericService<T> {
    
    @Override
    public void process(T item) {
        System.out.println("Processing " + item);
    }
    
    @Override
    public List<T> findAll() {
        return new ArrayList<>();
    }
    
    @Override
    public <S extends T> S save(S entity) {
        return entity;
    }
    
    @Override
    public void processAll(List<? extends T> items) {
        for (T item : items) {
            process(item);
        }
    }
}
