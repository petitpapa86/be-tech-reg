package com.bcbs239.regtech.dataquality.rulesengine.engine;

import java.util.List;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleViolation;

public class RuleExecutionResult {
    private String ruleId;
    private boolean success;
    private List<RuleViolation> violations;

    public static RuleExecutionResult success(String ruleId) {
        RuleExecutionResult r = new RuleExecutionResult();
        r.ruleId = ruleId;
        r.success = true;
        return r;
    }

    public static RuleExecutionResult failure(String ruleId, List<RuleViolation> violations) {
        RuleExecutionResult r = new RuleExecutionResult();
        r.ruleId = ruleId;
        r.success = false;
        r.violations = violations;
        return r;
    }

    public String getRuleId() { return ruleId; }
    public boolean isSuccess() { return success; }
    public boolean hasViolations() { return violations != null && !violations.isEmpty(); }
    public List<RuleViolation> getViolations() { return violations; }
}
