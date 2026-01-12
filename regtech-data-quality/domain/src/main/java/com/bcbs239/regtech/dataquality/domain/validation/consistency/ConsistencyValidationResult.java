package com.bcbs239.regtech.dataquality.domain.validation.consistency;

import java.util.ArrayList;
import java.util.List;

/**
 * Overall result of consistency validation across all checks.
 */
public record ConsistencyValidationResult(
    boolean allPassed,
    double overallScore,
    List<ConsistencyCheckResult> checks
) {
    public ConsistencyValidationResult {
        if (checks == null) {
            checks = new ArrayList<>();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean allPassed = true;
        private double overallScore = 0.0;
        private final List<ConsistencyCheckResult> checks = new ArrayList<>();

        public Builder addCheck(ConsistencyCheckResult check) {
            this.checks.add(check);
            if (!check.passed()) {
                this.allPassed = false;
            }
            return this;
        }

        public Builder overallScore(double score) {
            this.overallScore = score;
            return this;
        }

        public ConsistencyValidationResult build() {
            // Calculate overall score from individual checks
            if (checks.isEmpty()) {
                overallScore = 100.0;
            } else {
                overallScore = checks.stream()
                    .mapToDouble(ConsistencyCheckResult::score)
                    .average()
                    .orElse(0.0);
            }
            return new ConsistencyValidationResult(allPassed, overallScore, checks);
        }
    }
}
