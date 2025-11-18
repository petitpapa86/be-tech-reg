package com.bcbs239.regtech.dataquality.rulesengine.engine;

import java.util.List;

public interface RulesEngine {
    RuleExecutionResult executeRule(String ruleId, RuleContext context);
    List<RuleExecutionResult> executeRules(List<String> ruleIds, RuleContext context);
    List<RuleExecutionResult> executeRulesByType(String ruleType, RuleContext context);
    List<RuleExecutionResult> executeRulesByCategory(String category, RuleContext context);
}
