package com.example.approval.model;

import java.util.List;
import java.util.Objects;

public record ApprovalStep(String approver, List<String> roles) {
    public ApprovalStep {
        Objects.requireNonNull(approver);
        Objects.requireNonNull(roles);
        roles = List.copyOf(roles);
    }
}
