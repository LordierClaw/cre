package com.cre.fixtures;

import java.util.List;

public class ComplexGenericController<T> {
    public void processAll(List<? extends T> items, GenericService<T> service) {
        for (T item : items) {
            service.process(item);
        }
    }
}
