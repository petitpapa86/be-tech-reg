package com.bcbs239.regtech.dataquality.rulesengine.engine;

import com.bcbs239.regtech.dataquality.domain.rules.RuleViolation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of a rule execution.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleExecutionResult {
    
    private String ruleId;
    private boolean success;
    private String message;
    
    @Builder.Default
    private List<RuleViolation> violations = new ArrayList<>();
    
    @Builder.Default
    private Instant executionTime = Instant.now();
    
    private Long executionDurationMs;
    private String errorMessage;
    
    /**
     * Creates a successful result with no violations.
     * 
     * @param ruleId The rule ID
     * @return Success result
     */
    public static RuleExecutionResult success(String ruleId) {
        return RuleExecutionResult.builder()
            .ruleId(ruleId)
            .success(true)
            .message("Rule executed successfully")
            .build();
    }
    
    /**
     * Creates a success result with a custom message.
     * 
     * @param ruleId The rule ID
     * @param message Success message
     * @return Success result
     */
    public static RuleExecutionResult success(String ruleId, String message) {
        return RuleExecutionResult.builder()
            .ruleId(ruleId)
            .success(true)
            .message(message)
            .build();
    }
    
    /**
     * Creates a failure result with violations.
     * 
     * @param ruleId The rule ID
     * @param violations List of violations
     * @return Failure result
     */
    public static RuleExecutionResult failure(String ruleId, List<RuleViolation> violations) {
        return RuleExecutionResult.builder()
            .ruleId(ruleId)
            .success(false)
            .violations(violations)
            .message(String.format("Rule validation failed with %d violation(s)", violations.size()))
            .build();
    }
    
    /**
     * Creates an error result.
     * 
     * @param ruleId The rule ID
     * @param errorMessage Error message
     * @return Error result
     */
    public static RuleExecutionResult error(String ruleId, String errorMessage) {
        return RuleExecutionResult.builder()
            .ruleId(ruleId)
            .success(false)
            .errorMessage(errorMessage)
            .message("Rule execution encountered an error")
            .build();
    }
    
    /**
     * Checks if the execution has violations.
     * 
     * @return true if violations exist
     */
    public boolean hasViolations() {
        return violations != null && !violations.isEmpty();
    }
    
    /**
     * Checks if the execution encountered an error.
     * 
     * @return true if there was an error
     */
    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }
    
    /**
     * Gets the number of violations.
     * 
     * @return Violation count
     */
    public int getViolationCount() {
        return violations != null ? violations.size() : 0;
    }
}
