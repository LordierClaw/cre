package com.cre.fixtures;

import java.util.List;

public class ModernJavaFixture {
    
    public record UserRecord(String id, String name) {}

    public sealed interface Shape permits Circle, Square {
        double area();
    }

    public final class Circle implements Shape {
        public double area() { return 0; }
    }

    public non-sealed class Square implements Shape {
        public double area() { return 0; }
    }

    public void useRecord(UserRecord user) {
        System.out.println(user.name());
    }
}
