package com.bcbs239.regtech.dataquality.rulesengine.domain;

import java.time.Instant;
import java.util.Map;

/**
 * Domain DTO for RuleExecutionLog (not a JPA entity).
 */
public record RuleExecutionLogDto(
    String ruleId,
    Instant executionTimestamp,
    String entityType,
    String entityId,
    ExecutionResult executionResult,
    Integer violationCount,
    Long executionTimeMs,
    Map<String, Object> contextData,
    String errorMessage,
    String executedBy
) {
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String ruleId;
        private Instant executionTimestamp = Instant.now();
        private String entityType;
        private String entityId;
        private ExecutionResult executionResult;
        private Integer violationCount = 0;
        private Long executionTimeMs;
        private Map<String, Object> contextData;
        private String errorMessage;
        private String executedBy;
        
        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }
        
        public Builder executionTimestamp(Instant executionTimestamp) {
            this.executionTimestamp = executionTimestamp;
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
        
        public Builder executionResult(ExecutionResult executionResult) {
            this.executionResult = executionResult;
            return this;
        }
        
        public Builder violationCount(Integer violationCount) {
            this.violationCount = violationCount;
            return this;
        }
        
        public Builder executionTimeMs(Long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }
        
        public Builder contextData(Map<String, Object> contextData) {
            this.contextData = contextData;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder executedBy(String executedBy) {
            this.executedBy = executedBy;
            return this;
        }
        
        public RuleExecutionLogDto build() {
            return new RuleExecutionLogDto(
                ruleId,
                executionTimestamp,
                entityType,
                entityId,
                executionResult,
                violationCount,
                executionTimeMs,
                contextData,
                errorMessage,
                executedBy
            );
        }
    }
}
