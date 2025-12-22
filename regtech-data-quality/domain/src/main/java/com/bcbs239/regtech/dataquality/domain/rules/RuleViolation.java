package com.bcbs239.regtech.dataquality.domain.rules;

import java.time.Instant;
import java.util.Map;

/**
 * Domain model representing a rule violation (not a JPA entity).
 * This is a pure domain object used by the rules engine.
 */
public record RuleViolation(
    String ruleId,
    Long executionId,
    String entityType,
    String entityId,
    String violationType,
    String violationDescription,
    Severity severity,
    Instant detectedAt,
    Map<String, Object> violationDetails,
    ResolutionStatus resolutionStatus
) {
    public RuleViolation {
        if (ruleId == null) throw new IllegalArgumentException("ruleId cannot be null");
        if (entityType == null) throw new IllegalArgumentException("entityType cannot be null");
        if (entityId == null) throw new IllegalArgumentException("entityId cannot be null");
        if (violationType == null) throw new IllegalArgumentException("violationType cannot be null");
        if (violationDescription == null) throw new IllegalArgumentException("violationDescription cannot be null");
        if (severity == null) throw new IllegalArgumentException("severity cannot be null");
        if (detectedAt == null) detectedAt = Instant.now();
        if (resolutionStatus == null) resolutionStatus = ResolutionStatus.OPEN;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String ruleId;
        private Long executionId;
        private String entityType;
        private String entityId;
        private String violationType;
        private String violationDescription;
        private Severity severity;
        private Instant detectedAt = Instant.now();
        private Map<String, Object> violationDetails;
        private ResolutionStatus resolutionStatus = ResolutionStatus.OPEN;
        
        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }
        
        public Builder executionId(Long executionId) {
            this.executionId = executionId;
            return this;
        }
        
        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }
        
        public Builder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }
        
        public Builder violationType(String violationType) {
            this.violationType = violationType;
            return this;
        }
        
        public Builder violationDescription(String violationDescription) {
            this.violationDescription = violationDescription;
            return this;
        }
        
        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder detectedAt(Instant detectedAt) {
            this.detectedAt = detectedAt;
            return this;
        }
        
        public Builder violationDetails(Map<String, Object> violationDetails) {
            this.violationDetails = violationDetails;
            return this;
        }
        
        public Builder resolutionStatus(ResolutionStatus resolutionStatus) {
            this.resolutionStatus = resolutionStatus;
            return this;
        }
        
        public RuleViolation build() {
            return new RuleViolation(
                ruleId,
                executionId,
                entityType,
                entityId,
                violationType,
                violationDescription,
                severity,
                detectedAt,
                violationDetails,
                resolutionStatus
            );
        }
    }
}
