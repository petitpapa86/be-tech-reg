package com.bcbs239.regtech.dataquality.domain.validation.consistency;

/**
 * Represents a specific violation found during consistency checking.
 */
public record ConsistencyViolation(
    String violationType,
    String affectedEntity,
    String expectedValue,
    String actualValue,
    String description
) {
    
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String violationType;
        private String affectedEntity;
        private String expectedValue;
        private String actualValue;
        private String description;

        public Builder violationType(String violationType) {
            this.violationType = violationType;
            return this;
        }

        public Builder affectedEntity(String affectedEntity) {
            this.affectedEntity = affectedEntity;
            return this;
        }

        public Builder expectedValue(String expectedValue) {
            this.expectedValue = expectedValue;
            return this;
        }

        public Builder actualValue(String actualValue) {
            this.actualValue = actualValue;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public ConsistencyViolation build() {
            return new ConsistencyViolation(
                violationType,
                affectedEntity,
                expectedValue,
                actualValue,
                description
            );
        }
    }
}
