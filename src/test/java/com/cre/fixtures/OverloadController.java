package com.cre.fixtures;

public class OverloadController {
    private final OverloadService service = new OverloadService();

    public void test() {
        service.process("hello"); // Should map to process(String)
        service.process(123);     // Should map to process(int) or process(Integer)? Java prefers int if available.
        Object obj = "world";
        service.process(obj);     // Should map to process(Object)
    }

    public void testVar() {
        var s = "var hello";
        service.process(s);       // Should map to process(String)
    }
}
