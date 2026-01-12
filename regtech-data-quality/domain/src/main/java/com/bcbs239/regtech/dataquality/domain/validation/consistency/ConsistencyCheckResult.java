package com.bcbs239.regtech.dataquality.domain.validation.consistency;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a consistency check.
 * Each consistency check evaluates cross-field relationships and data uniformity.
 */
public record ConsistencyCheckResult(
    ConsistencyCheckType checkType,
    boolean passed,
    double score,
    String summary,
    List<ConsistencyViolation> violations
) {
    public ConsistencyCheckResult {
        if (violations == null) {
            violations = new ArrayList<>();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ConsistencyCheckType checkType;
        private boolean passed;
        private double score;
        private String summary;
        private List<ConsistencyViolation> violations = new ArrayList<>();

        public Builder checkType(ConsistencyCheckType checkType) {
            this.checkType = checkType;
            return this;
        }

        public Builder passed(boolean passed) {
            this.passed = passed;
            return this;
        }

        public Builder score(double score) {
            this.score = score;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder violations(List<ConsistencyViolation> violations) {
            this.violations = violations;
            return this;
        }

        public Builder addViolation(ConsistencyViolation violation) {
            this.violations.add(violation);
            return this;
        }

        public ConsistencyCheckResult build() {
            return new ConsistencyCheckResult(
                checkType,
                passed,
                score,
                summary,
                violations
            );
        }
    }
}
