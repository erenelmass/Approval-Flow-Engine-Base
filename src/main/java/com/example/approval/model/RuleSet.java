package com.example.approval.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

public record RuleSet(
        String version,
        LocalDate validFrom,
        BigDecimal mediumThreshold,
        BigDecimal highThreshold,
        Map<String, String> costCenterOwners,
        String financeManager,
        String technologyDirector,
        String ceo) {
    public RuleSet {
        Objects.requireNonNull(version);
        Objects.requireNonNull(validFrom);
        Objects.requireNonNull(mediumThreshold);
        Objects.requireNonNull(highThreshold);
        Objects.requireNonNull(costCenterOwners);
        Objects.requireNonNull(financeManager);
        Objects.requireNonNull(ceo);
        if (mediumThreshold.signum() < 0 || highThreshold.compareTo(mediumThreshold) <= 0) {
            throw new IllegalArgumentException("thresholds must be ordered and non-negative");
        }
        costCenterOwners = Map.copyOf(costCenterOwners);
    }

    public static RuleSet standardV1(Map<String, String> owners) {
        return new RuleSet("v1", LocalDate.of(2026, 1, 1),
                new BigDecimal("10000"), new BigDecimal("100000"), owners,
                "Fatma", "Deniz", "Elif");
    }

    public static RuleSet standardV2(Map<String, String> owners) {
        return new RuleSet("v2", LocalDate.of(2026, 3, 15),
                new BigDecimal("15000"), new BigDecimal("150000"), owners,
                "Fatma", "Deniz", "Elif");
    }
}
