package com.example.approval.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Request(
        String id,
        String requester,
        BigDecimal amount,
        String costCenter,
        String category,
        LocalDate date) {
    public Request {
        if (id == null || requester == null || amount == null || date == null) {
            throw new IllegalArgumentException("id, requester, amount and date are required");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
    }
}
