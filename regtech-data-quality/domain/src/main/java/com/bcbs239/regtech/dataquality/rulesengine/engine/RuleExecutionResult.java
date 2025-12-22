package com.bcbs239.regtech.dataquality.rulesengine.engine;

import com.bcbs239.regtech.dataquality.domain.rules.RuleViolation;

import java.util.List;

public class RuleExecutionResult {
    private String ruleId;
    private boolean success;
    private List<RuleViolation> violations;
    private String errorMessage;
    private String message;

    public static RuleExecutionResult success(String ruleId) {
        RuleExecutionResult r = new RuleExecutionResult();
        r.ruleId = ruleId;
        r.success = true;
        return r;
    }
    
    public static RuleExecutionResult success(String ruleId, String message) {
        RuleExecutionResult r = new RuleExecutionResult();
        r.ruleId = ruleId;
        r.success = true;
        r.message = message;
        return r;
    }

    public static RuleExecutionResult failure(String ruleId, List<RuleViolation> violations) {
        RuleExecutionResult r = new RuleExecutionResult();
        r.ruleId = ruleId;
        r.success = false;
        r.violations = violations;
        return r;
    }
    
    public static RuleExecutionResult error(String ruleId, String errorMessage) {
        RuleExecutionResult r = new RuleExecutionResult();
        r.ruleId = ruleId;
        r.success = false;
        r.errorMessage = errorMessage;
        return r;
    }

    public String getRuleId() { return ruleId; }
    public boolean isSuccess() { return success; }
    public boolean hasViolations() { return violations != null && !violations.isEmpty(); }
    public List<RuleViolation> getViolations() { return violations; }
    public String getErrorMessage() { return errorMessage; }
    public String getMessage() { return message; }
    public boolean isError() { return errorMessage != null; }
}
