package com.cre.fixtures;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends CrudRepository<Item, Long> {
    Item findByName(String name);
}

class Item {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
