package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.engine;

import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.BusinessRuleEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleExecutionLogEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleParameterEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleViolationEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.BusinessRuleRepository;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleExecutionLogRepository;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleViolationRepository;
import com.bcbs239.regtech.dataquality.rulesengine.domain.*;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleExecutionResult;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;

import com.bcbs239.regtech.dataquality.rulesengine.evaluator.ExpressionEvaluationException;
import com.bcbs239.regtech.dataquality.rulesengine.evaluator.ExpressionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of the Rules Engine with in-memory caching.
 * 
 * <p>Executes business rules using an expression evaluator and logs all executions.</p>
 * <p>Implements caching with TTL to improve performance for repeated rule executions.</p>
 * 
 * <p><strong>Caching Strategy:</strong></p>
 * <ul>
 *   <li>Rules are cached in memory after first load from database</li>
 *   <li>Cache has configurable TTL (time-to-live) in seconds</li>
 *   <li>Cache is automatically refreshed when TTL expires</li>
 *   <li>Parameter updates are reflected after cache refresh</li>
 *   <li>Cache is shared across all exposures in a batch</li>
 * </ul>
 * 
 * <p><strong>Requirements:</strong> 3.3, 3.4, 5.1, 5.2, 5.3, 5.4, 5.5</p>
 */
// explicit slf4j logger to avoid Lombok issues during build
public class DefaultRulesEngine implements RulesEngine {
    private static final Logger log = LoggerFactory.getLogger(DefaultRulesEngine.class);
    
    private final BusinessRuleRepository ruleRepository;
    private final RuleExecutionLogRepository executionLogRepository;
    private final RuleViolationRepository violationRepository;
    private final ExpressionEvaluator expressionEvaluator;
    
    // Cache configuration
    private final boolean cacheEnabled;
    private final int cacheTtlSeconds;
    
    // In-memory cache for rules
    private final Map<String, CachedRule> ruleCache = new ConcurrentHashMap<>();
    private volatile Instant lastCacheRefresh = Instant.now();
    
    /**
     * Cached rule wrapper that includes the rule and its load timestamp.
     */
    private static class CachedRule {
        final BusinessRuleEntity rule;
        final Instant loadedAt;
        
        CachedRule(BusinessRuleEntity rule, Instant loadedAt) {
            this.rule = rule;
            this.loadedAt = loadedAt;
        }
    }
    
    /**
     * Constructor with cache configuration.
     * 
     * @param ruleRepository Repository for loading rules
     * @param executionLogRepository Repository for logging executions
     * @param violationRepository Repository for storing violations
     * @param expressionEvaluator Evaluator for SpEL expressions
     * @param cacheEnabled Whether caching is enabled
     * @param cacheTtlSeconds Cache TTL in seconds
     */
    public DefaultRulesEngine(
            BusinessRuleRepository ruleRepository,
            RuleExecutionLogRepository executionLogRepository,
            RuleViolationRepository violationRepository,
            ExpressionEvaluator expressionEvaluator,
            boolean cacheEnabled,
            int cacheTtlSeconds) {
        this.ruleRepository = ruleRepository;
        this.executionLogRepository = executionLogRepository;
        this.violationRepository = violationRepository;
        this.expressionEvaluator = expressionEvaluator;
        this.cacheEnabled = cacheEnabled;
        this.cacheTtlSeconds = cacheTtlSeconds;
        
        log.info("DefaultRulesEngine initialized with caching: enabled={}, ttl={}s", 
            cacheEnabled, cacheTtlSeconds);
    }
    
    @Override
    @Transactional
    public RuleExecutionResult executeRule(String ruleId, RuleContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Load the rule (from cache if enabled and valid, otherwise from database)
            BusinessRuleEntity rule = loadRule(ruleId);
            
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
                // Rule failed - log execution first to get execution ID
                RuleExecutionLogEntity execLog = logExecution(rule, context, ExecutionResult.FAILURE,
                    1, executionTime, null);
                
                // Create violation with execution ID
                RuleViolationEntity violation = createViolation(rule, context);
                violation.setExecutionId(execLog.getExecutionId());
                
                // Save violation
                violationRepository.save(violation);
                
                // Convert to domain violation for result
                RuleViolation domainViolation = toDomainViolation(violation);
                List<RuleViolation> violations = List.of(domainViolation);
                
                return RuleExecutionResult.failure(ruleId, violations);
            }
            
        } catch (ExpressionEvaluationException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Expression evaluation failed for rule {}: {}", ruleId, e.getMessage());
            
            // Log error
            BusinessRuleEntity rule = ruleRepository.findById(ruleId).orElse(null);
            if (rule != null) {
                logExecution(rule, context, ExecutionResult.ERROR, 0, executionTime, e.getMessage());
            }
            
            return RuleExecutionResult.error(ruleId, "Expression evaluation failed: " + e.getMessage());
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Unexpected error executing rule {}: {}", ruleId, e.getMessage(), e);
            
            // Log error
            BusinessRuleEntity rule = ruleRepository.findById(ruleId).orElse(null);
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
            List<BusinessRuleEntity> rules = ruleRepository.findActiveRulesByType(type, LocalDate.now());
            
            List<String> ruleIds = rules.stream()
                .map(BusinessRuleEntity::getRuleId)
                .collect(Collectors.toList());
            
            return executeRules(ruleIds, context);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid rule type: {}", ruleType);
            return List.of();
        }
    }
    
    @Override
    public List<RuleExecutionResult> executeRulesByCategory(String category, RuleContext context) {
        List<BusinessRuleEntity> rules = ruleRepository.findActiveRulesByCategory(category, LocalDate.now());
        
        List<String> ruleIds = rules.stream()
            .map(BusinessRuleEntity::getRuleId)
            .collect(Collectors.toList());
        
        return executeRules(ruleIds, context);
    }
    
    // ====================================================================
    // Private Helper Methods
    // ====================================================================
    
    /**
     * Evaluates a rule's business logic expression.
     */
    private boolean evaluateRule(BusinessRuleEntity rule, RuleContext context) {
        String expression = rule.getBusinessLogic();
        return expressionEvaluator.evaluateBoolean(expression, context);
    }
    
    /**
     * Enriches the context with rule parameters.
     */
    private void enrichContextWithParameters(BusinessRuleEntity rule, RuleContext context) {
        for (RuleParameterEntity param : rule.getParameters()) {
            Object value = convertParameterValue(param);
            context.put(param.getParameterName(), value);
        }
    }
    
    /**
     * Converts a parameter value to its appropriate type based on dataType.
     */
    private Object convertParameterValue(RuleParameterEntity param) {
        String value = param.getParameterValue();
        if (value == null) {
            return null;
        }
        
        String dataType = param.getDataType();
        if (dataType == null) {
            return value; // Return as string if no type specified
        }
        
        try {
            return switch (dataType.toUpperCase()) {
                case "DECIMAL", "NUMERIC" -> new java.math.BigDecimal(value);
                case "INTEGER", "INT" -> Integer.valueOf(value);
                case "LONG" -> Long.valueOf(value);
                case "DOUBLE" -> Double.valueOf(value);
                case "BOOLEAN" -> Boolean.valueOf(value);
                default -> {
                    // For LIST type, return as comma-separated string
                    // SpEL can handle splitting if needed
                    if (param.getParameterType() == ParameterType.LIST) {
                        yield param.getListValue();
                    }
                    yield value;
                }
            };
        } catch (NumberFormatException e) {
            log.warn("Failed to convert parameter {} value '{}' to type {}, using string value", 
                param.getParameterName(), value, dataType);
            return value;
        }
    }
    
    /**
     * Checks if there's an active exemption for the rule and entity.
     */
    private boolean hasActiveExemption(BusinessRuleEntity rule, RuleContext context) {
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
    private RuleViolationEntity createViolation(BusinessRuleEntity rule, RuleContext context) {
        String entityType = context.get("entity_type", String.class);
        String entityId = context.get("entity_id", String.class);
        
        if (entityType == null) {
            entityType = "EXPOSURE";
        }
        if (entityId == null) {
            entityId = context.get("exposure_id", String.class);
        }
        
        // Use sanitized context to avoid JSONB serialization issues
        Map<String, Object> details = new HashMap<>();
        details.put("context", sanitizeContextForLogging(context));
        details.put("rule_logic", rule.getBusinessLogic());
        
        return RuleViolationEntity.builder()
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
     * Converts entity violation to domain violation.
     */
    private RuleViolation toDomainViolation(RuleViolationEntity entity) {
        return RuleViolation.builder()
            .ruleId(entity.getRuleId())
            .executionId(entity.getExecutionId())
            .entityType(entity.getEntityType())
            .entityId(entity.getEntityId())
            .violationType(entity.getViolationType())
            .violationDescription(entity.getViolationDescription())
            .severity(entity.getSeverity())
            .violationDetails(entity.getViolationDetails())
            .detectedAt(entity.getDetectedAt())
            .resolutionStatus(entity.getResolutionStatus())
            .build();
    }
    
    /**
     * Logs a rule execution.
     */
    private RuleExecutionLogEntity logExecution(BusinessRuleEntity rule, RuleContext context, 
                                         ExecutionResult result, int violationCount, 
                                         long executionTime, String errorMessage) {
        String entityType = context.get("entity_type", String.class);
        String entityId = context.get("entity_id", String.class);
        
        // Create a sanitized context with only essential data to avoid JSONB serialization issues
        Map<String, Object> sanitizedContext = sanitizeContextForLogging(context);
        
        RuleExecutionLogEntity ruleExecLog = RuleExecutionLogEntity.builder()
            .ruleId(rule.getRuleId())
            .entityType(entityType)
            .entityId(entityId)
            .executionResult(result)
            .violationCount(violationCount)
            .executionTimeMs(executionTime)
            .contextData(sanitizedContext)
            .errorMessage(errorMessage)
            .build();
        
        try {
            return executionLogRepository.save(ruleExecLog);
        } catch (Exception e) {
            log.error("Failed to save rule execution log for rule {}: {}", rule.getRuleId(), e.getMessage());
            // Try again with minimal context
            ruleExecLog.setContextData(Map.of(
                "entity_type", entityType != null ? entityType : "UNKNOWN",
                "entity_id", entityId != null ? entityId : "UNKNOWN"
            ));
            try {
                return executionLogRepository.save(ruleExecLog);
            } catch (Exception e2) {
                log.error("Failed to save rule execution log even with minimal context: {}", e2.getMessage());
                throw new RuntimeException("Unable to log rule execution", e2);
            }
        }
    }
    
    /**
     * Sanitizes context data for logging by keeping only essential fields.
     * This prevents JSONB serialization issues with large or complex objects.
     */
    private Map<String, Object> sanitizeContextForLogging(RuleContext context) {
        Map<String, Object> sanitized = new HashMap<>();
        
        // Include only essential fields that are safe to serialize
        String[] essentialFields = {
            "entity_type", "entity_id", "exposure_id", "counterparty_id",
            "amount", "currency", "country", "product_type", "lei_code",
            "reference_number", "is_corporate_exposure", "is_term_exposure"
        };
        
        for (String field : essentialFields) {
            Object value = context.get(field);
            if (value != null) {
                // Convert to string to ensure JSON serialization works
                sanitized.put(field, value.toString());
            }
        }
        
        return sanitized;
    }
    
    /**
     * Logs and returns a skipped result.
     */
    private RuleExecutionResult logAndReturnSkipped(BusinessRuleEntity rule, RuleContext context, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        logExecution(rule, context, ExecutionResult.SKIPPED, 0, executionTime, "Rule skipped");
        return RuleExecutionResult.success(rule.getRuleId(), "Rule skipped (inactive or exempted)");
    }
    
    // ====================================================================
    // Cache Management Methods
    // ====================================================================
    
    /**
     * Loads a rule from cache if available and valid, otherwise from database.
     * 
     * <p>This method implements the caching strategy:</p>
     * <ul>
     *   <li>If caching is disabled, always load from database</li>
     *   <li>If cache TTL has expired, refresh entire cache from database</li>
     *   <li>If rule is in cache and cache is valid, return cached rule</li>
     *   <li>If rule is not in cache, load from database and cache it</li>
     * </ul>
     * 
     * <p><strong>Requirements:</strong></p>
     * <ul>
     *   <li>5.1: Cache active rules in memory</li>
     *   <li>5.2: Reload rules when cache TTL expires</li>
     *   <li>5.3: Reuse cached rules across exposures</li>
     *   <li>3.3: Parameter updates reflected after cache refresh</li>
     * </ul>
     * 
     * @param ruleId The rule ID to load
     * @return The business rule
     * @throws IllegalArgumentException if rule not found
     */
    private BusinessRuleEntity loadRule(String ruleId) {
        if (!cacheEnabled) {
            // Caching disabled - always load from database
            log.trace("Cache disabled, loading rule {} from database", ruleId);
            return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));
        }
        
        // Check if cache needs refresh
        if (isCacheExpired()) {
            log.info("Cache TTL expired, refreshing rule cache");
            refreshCache();
        }
        
        // Try to get from cache
        CachedRule cachedRule = ruleCache.get(ruleId);
        if (cachedRule != null) {
            log.trace("Rule {} loaded from cache (age: {}s)", 
                ruleId, 
                java.time.Duration.between(cachedRule.loadedAt, Instant.now()).getSeconds());
            return cachedRule.rule;
        }
        
        // Not in cache - load from database and cache it
        log.debug("Rule {} not in cache, loading from database", ruleId);
        BusinessRuleEntity rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));
        
        // Add to cache
        ruleCache.put(ruleId, new CachedRule(rule, Instant.now()));
        log.debug("Rule {} added to cache", ruleId);
        
        return rule;
    }
    
    /**
     * Checks if the cache has expired based on TTL.
     * 
     * @return true if cache should be refreshed
     */
    private boolean isCacheExpired() {
        Instant now = Instant.now();
        long secondsSinceRefresh = java.time.Duration.between(lastCacheRefresh, now).getSeconds();
        boolean expired = secondsSinceRefresh >= cacheTtlSeconds;
        
        if (expired) {
            log.debug("Cache expired: age={}s, ttl={}s", secondsSinceRefresh, cacheTtlSeconds);
        }
        
        return expired;
    }
    
    /**
     * Refreshes the entire rule cache from the database.
     * 
     * <p>This method:</p>
     * <ul>
     *   <li>Clears the existing cache</li>
     *   <li>Loads all active rules from database</li>
     *   <li>Populates cache with fresh rules (including updated parameters)</li>
     *   <li>Updates the last refresh timestamp</li>
     * </ul>
     * 
     * <p><strong>Requirement 3.3:</strong> Parameter updates are reflected after cache refresh</p>
     * <p><strong>Requirement 5.2:</strong> Reload rules when cache TTL expires</p>
     */
    private void refreshCache() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Clear existing cache
            int oldSize = ruleCache.size();
            ruleCache.clear();
            
            // Load all active rules from database
            List<BusinessRuleEntity> activeRules = ruleRepository.findByEnabledTrue();
            
            // Populate cache with fresh rules
            Instant loadTime = Instant.now();
            for (BusinessRuleEntity rule : activeRules) {
                ruleCache.put(rule.getRuleId(), new CachedRule(rule, loadTime));
            }
            
            // Update last refresh timestamp
            lastCacheRefresh = loadTime;
            
            long refreshTime = System.currentTimeMillis() - startTime;
            log.info("Rule cache refreshed: oldSize={}, newSize={}, refreshTime={}ms", 
                oldSize, ruleCache.size(), refreshTime);
            
        } catch (Exception e) {
            log.error("Error refreshing rule cache: {}", e.getMessage(), e);
            // On error, clear cache to force database loads
            ruleCache.clear();
            lastCacheRefresh = Instant.now();
        }
    }
    
    /**
     * Manually clears the rule cache.
     * Useful for testing or administrative operations.
     */
    public void clearCache() {
        log.info("Manually clearing rule cache");
        ruleCache.clear();
        lastCacheRefresh = Instant.now();
    }
    
    /**
     * Gets cache statistics for monitoring.
     * 
     * @return Map containing cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheEnabled", cacheEnabled);
        stats.put("cacheTtlSeconds", cacheTtlSeconds);
        stats.put("cachedRuleCount", ruleCache.size());
        stats.put("cacheAgeSeconds", 
            java.time.Duration.between(lastCacheRefresh, Instant.now()).getSeconds());
        stats.put("cacheExpired", isCacheExpired());
        return stats;
    }
}
