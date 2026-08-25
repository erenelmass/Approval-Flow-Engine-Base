package com.example.approval.model;

public record Employee(
        String name,
        String title,
        String department,
        String manager) {
    public Employee {
        if (name == null || title == null || department == null) {
            throw new IllegalArgumentException("name, title and department are required");
        }
    }
}
