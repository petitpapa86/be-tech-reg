package com.bcbs239.regtech.dataquality.rulesengine.engine;

import java.util.List;

/**
 * Rules Engine interface for executing business rules.
 * 
 * <p>The engine evaluates rules against a context and returns execution results.</p>
 */
public interface RulesEngine {
    
    /**
     * Executes a single rule by ID.
     * 
     * @param ruleId The rule ID to execute
     * @param context The execution context containing data
     * @return The execution result
     */
    RuleExecutionResult executeRule(String ruleId, RuleContext context);
    
    /**
     * Executes multiple rules in order.
     * 
     * @param ruleIds List of rule IDs to execute
     * @param context The execution context
     * @return List of execution results
     */
    List<RuleExecutionResult> executeRules(List<String> ruleIds, RuleContext context);
    
    /**
     * Executes all active rules of a specific type.
     * 
     * @param ruleType The type of rules to execute
     * @param context The execution context
     * @return List of execution results
     */
    List<RuleExecutionResult> executeRulesByType(String ruleType, RuleContext context);
    
    /**
     * Executes all active rules in a category.
     * 
     * @param category The rule category
     * @param context The execution context
     * @return List of execution results
     */
    List<RuleExecutionResult> executeRulesByCategory(String category, RuleContext context);
}
