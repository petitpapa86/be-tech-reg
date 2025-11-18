package com.bcbs239.regtech.dataquality.rulesengine.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "rule_execution_log")
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
    
    public boolean isSuccessful() {
        return executionResult == ExecutionResult.SUCCESS;
    }
    
    public boolean hasViolations() {
        return violationCount != null && violationCount > 0;
    }
}
