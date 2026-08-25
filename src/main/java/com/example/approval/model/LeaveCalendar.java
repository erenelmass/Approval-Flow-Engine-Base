package com.example.approval.model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record LeaveCalendar(List<Leave> leaves) {
    public LeaveCalendar {
        Objects.requireNonNull(leaves);
        leaves = List.copyOf(leaves);
    }

    public static LeaveCalendar empty() {
        return new LeaveCalendar(List.of());
    }

    public static LeaveCalendar of(Leave... leaves) {
        return new LeaveCalendar(List.of(leaves));
    }

    public String resolve(String employee, LocalDate date) {
        String current = employee;
        Set<String> seen = new HashSet<>();
        while (isOnLeave(current, date)) {
            if (!seen.add(current)) {
                throw new IllegalStateException("delegation cycle detected at " + current);
            }
            current = leaveOf(current, date).delegate();
            if (current == null || current.isBlank()) {
                throw new IllegalStateException("leave has no delegate for " + employee);
            }
        }
        return current;
    }

    private boolean isOnLeave(String employee, LocalDate date) {
        return leaves.stream().anyMatch(leave -> leave.employee().equals(employee)
                && !date.isBefore(leave.from()) && !date.isAfter(leave.to()));
    }

    private Leave leaveOf(String employee, LocalDate date) {
        return leaves.stream()
                .filter(leave -> leave.employee().equals(employee)
                        && !date.isBefore(leave.from()) && !date.isAfter(leave.to()))
                .findFirst()
                .orElseThrow();
    }

    public record Leave(String employee, LocalDate from, LocalDate to, String delegate) {
        public Leave {
            Objects.requireNonNull(employee);
            Objects.requireNonNull(from);
            Objects.requireNonNull(to);
            if (to.isBefore(from)) {
                throw new IllegalArgumentException("leave end cannot precede start");
            }
        }
    }
}
