package com.example.approval.model;

import java.util.Map;

public record Organization(Map<String, Employee> employees) {
    public Organization {
        if (employees == null || employees.isEmpty()) {
            throw new IllegalArgumentException("employees are required");
        }
        employees = Map.copyOf(employees);
    }

    public Employee employee(String name) {
        Employee employee = employees.get(name);
        if (employee == null) {
            throw new IllegalArgumentException("unknown employee: " + name);
        }
        return employee;
    }
}
