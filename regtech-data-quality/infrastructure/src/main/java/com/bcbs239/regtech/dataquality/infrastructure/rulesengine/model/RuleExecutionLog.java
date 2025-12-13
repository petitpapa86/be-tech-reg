package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * Rule Execution Log entity representing the audit trail of rule executions.
 * 
 * <p>Logs every rule execution with its context, result, and performance metrics.
 * Essential for compliance reporting and troubleshooting.</p>
 */
@Entity
@Table(name = "rule_execution_log", schema = "dataquality")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleExecutionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execution_id")
    private Long executionId;
    
    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;
    
    @Column(name = "execution_timestamp", nullable = false)
    @Builder.Default
    private Instant executionTimestamp = Instant.now();
    
    @Column(name = "entity_type", length = 50)
    private String entityType;
    
    @Column(name = "entity_id", length = 100)
    private String entityId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "execution_result", nullable = false, length = 20)
    private ExecutionResult executionResult;
    
    @Column(name = "violation_count")
    @Builder.Default
    private Integer violationCount = 0;
    
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
    
    @Column(name = "context_data", columnDefinition = "JSONB")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> contextData;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "executed_by", length = 100)
    private String executedBy;
    
    /**
     * Checks if the execution was successful.
     * 
     * @return true if execution succeeded
     */
    public boolean isSuccessful() {
        return executionResult == ExecutionResult.SUCCESS;
    }
    
    /**
     * Checks if the execution had violations.
     * 
     * @return true if violations were detected
     */
    public boolean hasViolations() {
        return violationCount != null && violationCount > 0;
    }
}
