package com.cre.fixtures;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/generic")
public class GenericController {
    
    private final GenericService<String> service;
    private Map<String, List<Integer>> complexMap;
    
    public GenericController(GenericService<String> service) {
        this.service = service;
    }
    
    @PostMapping
    public void create(@RequestBody String name) {
        service.process(name);
    }
    
    @GetMapping
    public List<String> list() {
        return service.findAll();
    }
    
    @PostMapping("/bulk")
    public void bulkCreate(@RequestBody List<String> names) {
        service.processAll(names);
    }
}
