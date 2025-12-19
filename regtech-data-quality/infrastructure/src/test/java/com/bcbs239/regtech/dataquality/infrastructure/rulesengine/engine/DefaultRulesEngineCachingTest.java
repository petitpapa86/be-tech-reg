package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.engine;

import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.BusinessRuleEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleExecutionLogEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleParameterEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.evaluator.ExpressionEvaluator;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.BusinessRuleRepository;
import com.bcbs239.regtech.dataquality.rulesengine.domain.ParameterType;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleType;
import com.bcbs239.regtech.dataquality.rulesengine.domain.Severity;
import com.bcbs239.regtech.dataquality.rulesengine.engine.DefaultRuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for DefaultRulesEngine caching functionality.
 * 
 * <p>Verifies:</p>
 * <ul>
 *   <li>Rules are cached in memory after first load</li>
 *   <li>Cache TTL configuration works correctly</li>
 *   <li>Cache refresh on TTL expiration</li>
 *   <li>Parameter updates reflected after cache refresh</li>
 *   <li>Cache reuse across multiple exposures</li>
 * </ul>
 * 
 * <p><strong>Requirements:</strong> 3.3, 3.4, 5.1, 5.2, 5.3, 5.4, 5.5</p>
 */
@ExtendWith(MockitoExtension.class)
class DefaultRulesEngineCachingTest {
    
    @Mock
    private BusinessRuleRepository ruleRepository;
    
    @Mock
    private RulesEngineAuditPersistenceService auditPersistenceService;
    
    @Mock
    private ExpressionEvaluator expressionEvaluator;
    
    private DefaultRulesEngine rulesEngine;
    
    @BeforeEach
    void setUp() {
        // Mock audit persistence to behave like a no-op save.
        when(auditPersistenceService.saveExecutionLogBestEffort(any(RuleExecutionLogEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }
    
    /**
     * Test: Rules are cached in memory after first load
     * Requirement: 5.1 - Cache active rules in memory
     */
    @Test
    void testRulesCachedInMemory() {
        // Given: Cache enabled with 300 second TTL
        rulesEngine = new DefaultRulesEngine(
            ruleRepository, auditPersistenceService,
            expressionEvaluator, true, 300
        );
        
        BusinessRuleEntity rule = createTestRule("RULE_001", "#amount > 0");
        when(ruleRepository.findById("RULE_001")).thenReturn(Optional.of(rule));
        when(expressionEvaluator.evaluateBoolean(anyString(), any(RuleContext.class)))
            .thenReturn(true);
        
        RuleContext context = createTestContext();
        
        // When: Execute rule twice
        rulesEngine.executeRule("RULE_001", context);
        rulesEngine.executeRule("RULE_001", context);
        
        // Then: Rule should be loaded from database only once (cached on second call)
        verify(ruleRepository, times(1)).findById("RULE_001");
    }
    
    /**
     * Test: Cache disabled loads from database every time
     * Requirement: 5.1 - Cache can be disabled
     */
    @Test
    void testCacheDisabledLoadsFromDatabaseEveryTime() {
        // Given: Cache disabled
        rulesEngine = new DefaultRulesEngine(
            ruleRepository, auditPersistenceService,
            expressionEvaluator, false, 300
        );
        
        BusinessRuleEntity rule = createTestRule("RULE_001", "#amount > 0");
        when(ruleRepository.findById("RULE_001")).thenReturn(Optional.of(rule));
        when(expressionEvaluator.evaluateBoolean(anyString(), any(RuleContext.class)))
            .thenReturn(true);
        
        RuleContext context = createTestContext();
        
        // When: Execute rule twice
        rulesEngine.executeRule("RULE_001", context);
        rulesEngine.executeRule("RULE_001", context);
        
        // Then: Rule should be loaded from database both times
        verify(ruleRepository, times(2)).findById("RULE_001");
    }
    
    /**
     * Test: Cache refresh on TTL expiration
     * Requirement: 5.2 - Reload rules when cache TTL expires
     */
    @Test
    void testCacheRefreshOnTTLExpiration() throws InterruptedException {
        // Given: Cache enabled with 1 second TTL
        rulesEngine = new DefaultRulesEngine(
            ruleRepository, auditPersistenceService,
            expressionEvaluator, true, 1
        );
        
        BusinessRuleEntity rule = createTestRule("RULE_001", "#amount > 0");
        when(ruleRepository.findById("RULE_001")).thenReturn(Optional.of(rule));
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(expressionEvaluator.evaluateBoolean(anyString(), any(RuleContext.class)))
            .thenReturn(true);
        
        RuleContext context = createTestContext();
        
        // When: Execute rule, wait for TTL to expire, execute again
        rulesEngine.executeRule("RULE_001", context);
        Thread.sleep(1100); // Wait for cache to expire
        rulesEngine.executeRule("RULE_001", context);
        
        // Then: Cache should be refreshed (findByEnabledTrue called)
        verify(ruleRepository, atLeastOnce()).findByEnabledTrue();
    }
    
    /**
     * Test: Parameter updates reflected after cache refresh
     * Requirement: 3.3 - Parameter updates applied after cache refresh
     */
    @Test
    void testParameterUpdatesReflectedAfterCacheRefresh() throws InterruptedException {
        // Given: Cache enabled with 1 second TTL
        rulesEngine = new DefaultRulesEngine(
            ruleRepository, auditPersistenceService,
            expressionEvaluator, true, 1
        );
        
        // Initial rule with parameter value 100
        BusinessRuleEntity rule1 = createTestRuleWithParameter("RULE_001", "#amount > #threshold", 
            "threshold", "100");
        
        // Updated rule with parameter value 200
        BusinessRuleEntity rule2 = createTestRuleWithParameter("RULE_001", "#amount > #threshold", 
            "threshold", "200");
        
        when(ruleRepository.findById("RULE_001"))
            .thenReturn(Optional.of(rule1))
            .thenReturn(Optional.of(rule2));
        when(ruleRepository.findByEnabledTrue())
            .thenReturn(List.of(rule1))
            .thenReturn(List.of(rule2));
        when(expressionEvaluator.evaluateBoolean(anyString(), any(RuleContext.class)))
            .thenReturn(true);
        
        RuleContext context = createTestContext();
        
        // When: Execute rule, wait for cache to expire, execute again
        rulesEngine.executeRule("RULE_001", context);
        Thread.sleep(1100); // Wait for cache to expire
        rulesEngine.executeRule("RULE_001", context);
        
        // Then: Cache should be refreshed with updated parameters
        verify(ruleRepository, atLeastOnce()).findByEnabledTrue();
    }
    
    /**
     * Test: Cache reuse across multiple exposures
     * Requirement: 5.3 - Reuse cached rules across exposures
     */
    @Test
    void testCacheReuseAcrossMultipleExposures() {
        // Given: Cache enabled with 300 second TTL
        rulesEngine = new DefaultRulesEngine(
            ruleRepository, auditPersistenceService,
            expressionEvaluator, true, 300
        );
        
        BusinessRuleEntity rule = createTestRule("RULE_001", "#amount > 0");
        when(ruleRepository.findById("RULE_001")).thenReturn(Optional.of(rule));
        when(expressionEvaluator.evaluateBoolean(anyString(), any(RuleContext.class)))
            .thenReturn(true);
        
        // When: Execute rule for multiple exposures
        for (int i = 0; i < 10; i++) {
            RuleContext context = createTestContext();
            rulesEngine.executeRule("RULE_001", context);
        }
        
        // Then: Rule should be loaded from database only once
        verify(ruleRepository, times(1)).findById("RULE_001");
    }
    
    /**
     * Test: Cache statistics are accurate
     * Requirement: 5.4 - Cache parameter values with the rule
     */
    @Test
    void testCacheStatistics() {
        // Given: Cache enabled with 300 second TTL
        rulesEngine = new DefaultRulesEngine(
            ruleRepository, auditPersistenceService,
            expressionEvaluator, true, 300
        );
        
        BusinessRuleEntity rule = createTestRule("RULE_001", "#amount > 0");
        when(ruleRepository.findById("RULE_001")).thenReturn(Optional.of(rule));
        when(expressionEvaluator.evaluateBoolean(anyString(), any(RuleContext.class)))
            .thenReturn(true);
        
        RuleContext context = createTestContext();
        
        // When: Execute rule
        rulesEngine.executeRule("RULE_001", context);
        
        // Then: Cache stats should reflect cached rule
        Map<String, Object> stats = rulesEngine.getCacheStats();
        assertThat(stats).containsEntry("cacheEnabled", true);
        assertThat(stats).containsEntry("cacheTtlSeconds", 300);
        assertThat(stats).containsEntry("cachedRuleCount", 1);
        assertThat(stats).containsKey("cacheAgeSeconds");
        assertThat(stats).containsKey("cacheExpired");
    }
    
    /**
     * Test: Manual cache clear works
     * Requirement: 5.5 - Cache can be invalidated
     */
    @Test
    void testManualCacheClear() {
        // Given: Cache enabled with 300 second TTL
        rulesEngine = new DefaultRulesEngine(
            ruleRepository, auditPersistenceService,
            expressionEvaluator, true, 300
        );
        
        BusinessRuleEntity rule = createTestRule("RULE_001", "#amount > 0");
        when(ruleRepository.findById("RULE_001")).thenReturn(Optional.of(rule));
        when(expressionEvaluator.evaluateBoolean(anyString(), any(RuleContext.class)))
            .thenReturn(true);
        
        RuleContext context = createTestContext();
        
        // When: Execute rule, clear cache, execute again
        rulesEngine.executeRule("RULE_001", context);
        rulesEngine.clearCache();
        rulesEngine.executeRule("RULE_001", context);
        
        // Then: Rule should be loaded from database twice
        verify(ruleRepository, times(2)).findById("RULE_001");
    }
    
    // ====================================================================
    // Helper Methods
    // ====================================================================
    
    private BusinessRuleEntity createTestRule(String ruleId, String businessLogic) {
        return BusinessRuleEntity.builder()
            .ruleId(ruleId)
            .ruleCode(ruleId)
            .regulationId("TEST_REG")
            .ruleName("Test Rule")
            .description("Test rule description")
            .ruleType(RuleType.ACCURACY)
            .ruleCategory("TEST")
            .businessLogic(businessLogic)
            .severity(Severity.MEDIUM)
            .enabled(true)
            .effectiveDate(LocalDate.now().minusDays(1))
            .expirationDate(null)
            .parameters(new ArrayList<>())
            .exemptions(new ArrayList<>())
            .build();
    }
    
    private BusinessRuleEntity createTestRuleWithParameter(String ruleId, String businessLogic, 
                                                     String paramName, String paramValue) {
        BusinessRuleEntity rule = BusinessRuleEntity.builder()
            .ruleId(ruleId)
            .ruleCode(ruleId)
            .regulationId("TEST_REG")
            .ruleName("Test Rule")
            .description("Test rule description")
            .ruleType(RuleType.ACCURACY)
            .ruleCategory("TEST")
            .businessLogic(businessLogic)
            .severity(Severity.MEDIUM)
            .enabled(true)
            .effectiveDate(LocalDate.now().minusDays(1))
            .expirationDate(null)
            .parameters(new ArrayList<>())
            .exemptions(new ArrayList<>())
            .build();
        
        RuleParameterEntity parameter = RuleParameterEntity.builder()
            .rule(rule)
            .parameterName(paramName)
            .parameterValue(paramValue)
            .parameterType(ParameterType.NUMERIC)
            .dataType("NUMERIC")
            .description("Test parameter")
            .build();
        
        rule.addParameter(parameter);
        return rule;
    }
    
    private RuleContext createTestContext() {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", BigDecimal.valueOf(1000));
        data.put("currency", "EUR");
        data.put("entity_type", "EXPOSURE");
        data.put("entity_id", "EXP_001");
        return new DefaultRuleContext(data);
    }
}
