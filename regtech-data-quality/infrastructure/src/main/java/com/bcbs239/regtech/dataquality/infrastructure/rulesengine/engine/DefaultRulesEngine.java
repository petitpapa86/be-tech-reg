package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.engine;

import com.bcbs239.regtech.dataquality.rulesengine.domain.*;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleExecutionResult;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;
import com.bcbs239.regtech.dataquality.rulesengine.evaluator.ExpressionEvaluator;
import com.bcbs239.regtech.dataquality.rulesengine.evaluator.ExpressionEvaluationException;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.BusinessRuleRepository;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleExecutionLogRepository;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleViolationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of the Rules Engine.
 * 
 * <p>Executes business rules using an expression evaluator and logs all executions.</p>
 */
// explicit slf4j logger to avoid Lombok issues during build
@Service
@RequiredArgsConstructor
public class DefaultRulesEngine implements RulesEngine {
    private static final Logger log = LoggerFactory.getLogger(DefaultRulesEngine.class);
    
    private final BusinessRuleRepository ruleRepository;
    private final RuleExecutionLogRepository executionLogRepository;
    private final RuleViolationRepository violationRepository;
    private final ExpressionEvaluator expressionEvaluator;
    
    @Override
    @Transactional
    public RuleExecutionResult executeRule(String ruleId, RuleContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Load the rule
            BusinessRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));
            
            // Check if rule is active
            if (!rule.isActive()) {
                log.debug("Rule {} is not active, skipping execution", ruleId);
                return logAndReturnSkipped(rule, context, startTime);
            }
            
            // Check for exemptions
            if (hasActiveExemption(rule, context)) {
                log.debug("Active exemption found for rule {}, skipping execution", ruleId);
                return logAndReturnSkipped(rule, context, startTime);
            }
            
            // Prepare context with rule parameters
            enrichContextWithParameters(rule, context);
            
            // Evaluate the rule
            boolean ruleResult = evaluateRule(rule, context);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (ruleResult) {
                // Rule passed
                logExecution(rule, context, ExecutionResult.SUCCESS, 0, executionTime, null);
                return RuleExecutionResult.success(ruleId);
            } else {
                // Rule failed - create violation
                RuleViolation violation = createViolation(rule, context);
                List<RuleViolation> violations = List.of(violation);
                
                // Save violation
                violationRepository.save(violation);
                
                // Log execution
                RuleExecutionLog execLog = logExecution(rule, context, ExecutionResult.FAILURE, 
                    1, executionTime, null);
                violation.setExecutionId(execLog.getExecutionId());
                
                return RuleExecutionResult.failure(ruleId, violations);
            }
            
        } catch (ExpressionEvaluationException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Expression evaluation failed for rule {}: {}", ruleId, e.getMessage());
            
            // Log error
            BusinessRule rule = ruleRepository.findById(ruleId).orElse(null);
            if (rule != null) {
                logExecution(rule, context, ExecutionResult.ERROR, 0, executionTime, e.getMessage());
            }
            
            return RuleExecutionResult.error(ruleId, "Expression evaluation failed: " + e.getMessage());
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Unexpected error executing rule {}: {}", ruleId, e.getMessage(), e);
            
            // Log error
            BusinessRule rule = ruleRepository.findById(ruleId).orElse(null);
            if (rule != null) {
                logExecution(rule, context, ExecutionResult.ERROR, 0, executionTime, e.getMessage());
            }
            
            return RuleExecutionResult.error(ruleId, "Execution error: " + e.getMessage());
        }
    }
    
    @Override
    public List<RuleExecutionResult> executeRules(List<String> ruleIds, RuleContext context) {
        List<RuleExecutionResult> results = new ArrayList<>();
        
        for (String ruleId : ruleIds) {
            try {
                RuleExecutionResult result = executeRule(ruleId, context);
                results.add(result);
            } catch (Exception e) {
                log.error("Failed to execute rule {}: {}", ruleId, e.getMessage());
                results.add(RuleExecutionResult.error(ruleId, e.getMessage()));
            }
        }
        
        return results;
    }
    
    @Override
    public List<RuleExecutionResult> executeRulesByType(String ruleType, RuleContext context) {
        try {
            RuleType type = RuleType.valueOf(ruleType.toUpperCase());
            List<BusinessRule> rules = ruleRepository.findActiveRulesByType(type, LocalDate.now());
            
            List<String> ruleIds = rules.stream()
                .map(BusinessRule::getRuleId)
                .collect(Collectors.toList());
            
            return executeRules(ruleIds, context);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid rule type: {}", ruleType);
            return List.of();
        }
    }
    
    @Override
    public List<RuleExecutionResult> executeRulesByCategory(String category, RuleContext context) {
        List<BusinessRule> rules = ruleRepository.findActiveRulesByCategory(category, LocalDate.now());
        
        List<String> ruleIds = rules.stream()
            .map(BusinessRule::getRuleId)
            .collect(Collectors.toList());
        
        return executeRules(ruleIds, context);
    }
    
    // ====================================================================
    // Private Helper Methods
    // ====================================================================
    
    /**
     * Evaluates a rule's business logic expression.
     */
    private boolean evaluateRule(BusinessRule rule, RuleContext context) {
        String expression = rule.getBusinessLogic();
        return expressionEvaluator.evaluateBoolean(expression, context);
    }
    
    /**
     * Enriches the context with rule parameters.
     */
    private void enrichContextWithParameters(BusinessRule rule, RuleContext context) {
        for (RuleParameter param : rule.getParameters()) {
            context.put(param.getParameterName(), param.getParameterValue());
        }
    }
    
    /**
     * Checks if there's an active exemption for the rule and entity.
     */
    private boolean hasActiveExemption(BusinessRule rule, RuleContext context) {
        String entityType = context.get("entity_type", String.class);
        String entityId = context.get("entity_id", String.class);
        
        if (entityType == null) {
            return false;
        }
        
        return rule.getExemptions().stream()
            .anyMatch(exemption -> exemption.appliesTo(entityType, entityId));
    }
    
    /**
     * Creates a violation from a failed rule execution.
     */
    private RuleViolation createViolation(BusinessRule rule, RuleContext context) {
        String entityType = context.get("entity_type", String.class);
        String entityId = context.get("entity_id", String.class);
        
        if (entityType == null) {
            entityType = "EXPOSURE";
        }
        if (entityId == null) {
            entityId = context.get("exposure_id", String.class);
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("context", context.getAllData());
        details.put("rule_logic", rule.getBusinessLogic());
        
        return RuleViolation.builder()
            .ruleId(rule.getRuleId())
            .entityType(entityType)
            .entityId(entityId)
            .violationType(rule.getRuleCode())
            .violationDescription(rule.getDescription())
            .severity(rule.getSeverity())
            .violationDetails(details)
            .build();
    }
    
    /**
     * Logs a rule execution.
     */
    private RuleExecutionLog logExecution(BusinessRule rule, RuleContext context, 
                                         ExecutionResult result, int violationCount, 
                                         long executionTime, String errorMessage) {
        String entityType = context.get("entity_type", String.class);
        String entityId = context.get("entity_id", String.class);
        
        RuleExecutionLog ruleExecLog = RuleExecutionLog.builder()
            .ruleId(rule.getRuleId())
            .entityType(entityType)
            .entityId(entityId)
            .executionResult(result)
            .violationCount(violationCount)
            .executionTimeMs(executionTime)
            .contextData(context.getAllData())
            .errorMessage(errorMessage)
            .build();
        
        return executionLogRepository.save(ruleExecLog);
    }
    
    /**
     * Logs and returns a skipped result.
     */
    private RuleExecutionResult logAndReturnSkipped(BusinessRule rule, RuleContext context, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        logExecution(rule, context, ExecutionResult.SKIPPED, 0, executionTime, "Rule skipped");
        return RuleExecutionResult.success(rule.getRuleId(), "Rule skipped (inactive or exempted)");
    }
}
