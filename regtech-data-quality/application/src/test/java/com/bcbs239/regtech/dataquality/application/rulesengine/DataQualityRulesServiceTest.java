package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.rulesengine.domain.*;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.evaluator.SpelExpressionEvaluator;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.*;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleExecutionResult;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataQualityRulesService Unit Tests")
class DataQualityRulesServiceTest {

    @Mock(lenient = true)
    private RulesEngine rulesEngine;
    
    @Mock(lenient = true)
    private BusinessRuleRepository ruleRepository;
    
    @Mock(lenient = true)
    private RuleViolationRepository violationRepository;
    
    @Mock(lenient = true)
    private RuleExecutionLogRepository executionLogRepository;
    
    @Mock(lenient = true)
    private RuleExemptionRepository exemptionRepository;
    
    private DataQualityRulesService service;
    
    @BeforeEach
    void setUp() {
        service = new DataQualityRulesService(
            rulesEngine,
            ruleRepository,
            violationRepository,
            executionLogRepository,
            exemptionRepository
        );
    }

    @Test
    @DisplayName("Should expose camelCase variables for DB SpEL rules")
    void shouldExposeCamelCaseVariablesForDbSpelRules() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        BusinessRule rule = createTestRule("RULE_001", true);

        when(ruleRepository.findByEnabledTrue()).thenReturn(Collections.singletonList(rule));
        when(rulesEngine.executeRule(eq("RULE_001"), any(RuleContext.class)))
            .thenReturn(RuleExecutionResult.success("RULE_001"));
        when(exemptionRepository.findActiveExemptions(anyString(), anyString(), anyString(), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());

        ArgumentCaptor<RuleContext> contextCaptor = ArgumentCaptor.forClass(RuleContext.class);

        // Act
        service.validateConfigurableRules(exposure);

        // Assert
        verify(rulesEngine).executeRule(eq("RULE_001"), contextCaptor.capture());
        RuleContext ctx = contextCaptor.getValue();

        assertTrue(ctx.containsKey("exposureId"), "Context should contain exposureId for #exposureId rules");
        assertEquals(exposure.exposureId(), ctx.get("exposureId"));
        assertTrue(ctx.containsKey("productType"), "Context should contain productType for #productType rules");
        assertEquals(exposure.productType(), ctx.get("productType"));

        // Backwards-compatible aliases
        assertTrue(ctx.containsKey("exposure_id"), "Context should keep exposure_id alias");
        assertEquals(exposure.exposureId(), ctx.get("exposure_id"));

        // Prove DB-style SpEL rules actually evaluate correctly
        SpelExpressionEvaluator evaluator = new SpelExpressionEvaluator();
        assertTrue(
            evaluator.evaluateBoolean("#exposureId != null && !#exposureId.trim().isEmpty()", ctx),
            "#exposureId rule should pass when exposure_id is provided"
        );
        assertTrue(
            evaluator.evaluateBoolean("#productType != null && !#productType.trim().isEmpty()", ctx),
            "#productType rule should pass when product_type is provided"
        );
        assertTrue(
            evaluator.evaluateBoolean("#exposure_id != null && !#exposure_id.trim().isEmpty()", ctx),
            "snake_case #exposure_id rule should still pass"
        );
    }
    
    // ====================================================================
    // Tests for Rule Enable/Disable Functionality (Task 6)
    // ====================================================================
    
    @Test
    @DisplayName("Should load only enabled rules")
    void shouldLoadOnlyEnabledRules() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        
        BusinessRule enabledRule1 = createTestRule("RULE_001", true);
        BusinessRule enabledRule2 = createTestRule("RULE_002", true);
        
        when(ruleRepository.findByEnabledTrue()).thenReturn(Arrays.asList(enabledRule1, enabledRule2));
        when(rulesEngine.executeRule(anyString(), any(RuleContext.class)))
            .thenReturn(RuleExecutionResult.success("RULE_001"))
            .thenReturn(RuleExecutionResult.success("RULE_002"));
        when(exemptionRepository.findActiveExemptions(anyString(), anyString(), anyString(), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        
        // Act
        List<ValidationError> errors = service.validateConfigurableRules(exposure);
        
        // Assert
        verify(ruleRepository).findByEnabledTrue();
        verify(rulesEngine, times(2)).executeRule(anyString(), any(RuleContext.class));
        verify(executionLogRepository, times(2)).save(any(RuleExecutionLog.class));
    }
    
    @Test
    @DisplayName("Should skip disabled rules during validation")
    void shouldSkipDisabledRules() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        
        // Only enabled rules should be returned by the repository
        BusinessRule enabledRule = createTestRule("RULE_ENABLED", true);
        
        when(ruleRepository.findByEnabledTrue()).thenReturn(Collections.singletonList(enabledRule));
        when(rulesEngine.executeRule(eq("RULE_ENABLED"), any(RuleContext.class)))
            .thenReturn(RuleExecutionResult.success("RULE_ENABLED"));
        when(exemptionRepository.findActiveExemptions(anyString(), anyString(), anyString(), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        
        // Act
        List<ValidationError> errors = service.validateConfigurableRules(exposure);
        
        // Assert
        verify(ruleRepository).findByEnabledTrue();
        verify(rulesEngine, times(1)).executeRule(eq("RULE_ENABLED"), any(RuleContext.class));
        verify(rulesEngine, never()).executeRule(eq("RULE_DISABLED"), any(RuleContext.class));
        verify(executionLogRepository, times(1)).save(any(RuleExecutionLog.class));
    }
    
    @Test
    @DisplayName("Should execute all enabled rules for validation")
    void shouldExecuteAllEnabledRules() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        
        BusinessRule rule1 = createTestRule("COMPLETENESS_AMOUNT", true);
        BusinessRule rule2 = createTestRule("ACCURACY_POSITIVE_AMOUNT", true);
        BusinessRule rule3 = createTestRule("VALIDITY_CURRENCY", true);
        
        when(ruleRepository.findByEnabledTrue()).thenReturn(Arrays.asList(rule1, rule2, rule3));
        when(rulesEngine.executeRule(anyString(), any(RuleContext.class)))
            .thenReturn(RuleExecutionResult.success("COMPLETENESS_AMOUNT"))
            .thenReturn(RuleExecutionResult.success("ACCURACY_POSITIVE_AMOUNT"))
            .thenReturn(RuleExecutionResult.success("VALIDITY_CURRENCY"));
        when(exemptionRepository.findActiveExemptions(anyString(), anyString(), anyString(), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        
        // Act
        List<ValidationError> errors = service.validateConfigurableRules(exposure);
        
        // Assert
        verify(ruleRepository).findByEnabledTrue();
        verify(rulesEngine, times(3)).executeRule(anyString(), any(RuleContext.class));
        verify(executionLogRepository, times(3)).save(any(RuleExecutionLog.class));
    }
    
    @Test
    @DisplayName("Should execute multiple rules for same field and aggregate violations")
    void shouldExecuteMultipleRulesForSameFieldAndAggregate() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        
        // Multiple rules validating the same field (amount)
        BusinessRule rule1 = createTestRule("ACCURACY_POSITIVE_AMOUNT", true);
        BusinessRule rule2 = createTestRule("ACCURACY_REASONABLE_AMOUNT", true);
        BusinessRule rule3 = createTestRule("COMPLETENESS_AMOUNT_REQUIRED", true);
        
        // Create violations for each rule
        RuleViolation violation1 = createTestViolation("ACCURACY_POSITIVE_AMOUNT", "amount", "Amount must be positive");
        RuleViolation violation2 = createTestViolation("ACCURACY_REASONABLE_AMOUNT", "amount", "Amount exceeds reasonable threshold");
        
        RuleExecutionResult result1 = RuleExecutionResult.failure("ACCURACY_POSITIVE_AMOUNT", Collections.singletonList(violation1));
        RuleExecutionResult result2 = RuleExecutionResult.failure("ACCURACY_REASONABLE_AMOUNT", Collections.singletonList(violation2));
        RuleExecutionResult result3 = RuleExecutionResult.success("COMPLETENESS_AMOUNT_REQUIRED");
        
        when(ruleRepository.findByEnabledTrue()).thenReturn(Arrays.asList(rule1, rule2, rule3));
        when(rulesEngine.executeRule(eq("ACCURACY_POSITIVE_AMOUNT"), any(RuleContext.class))).thenReturn(result1);
        when(rulesEngine.executeRule(eq("ACCURACY_REASONABLE_AMOUNT"), any(RuleContext.class))).thenReturn(result2);
        when(rulesEngine.executeRule(eq("COMPLETENESS_AMOUNT_REQUIRED"), any(RuleContext.class))).thenReturn(result3);
        when(exemptionRepository.findActiveExemptions(anyString(), anyString(), anyString(), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        
        // Act
        List<ValidationError> errors = service.validateConfigurableRules(exposure);
        
        // Assert
        assertEquals(2, errors.size(), "Should have 2 violations from 2 failed rules");
        verify(rulesEngine, times(3)).executeRule(anyString(), any(RuleContext.class));
        verify(violationRepository, times(2)).save(any(RuleViolation.class));
        verify(executionLogRepository, times(3)).save(any(RuleExecutionLog.class));
        
        // Verify both violations are for the same field
        assertTrue(errors.stream().allMatch(e -> "amount".equals(e.fieldName())));
    }
    
    @Test
    @DisplayName("Should return empty list when no enabled rules exist")
    void shouldReturnEmptyListWhenNoEnabledRules() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        
        when(ruleRepository.findByEnabledTrue()).thenReturn(Collections.emptyList());
        
        // Act
        List<ValidationError> errors = service.validateConfigurableRules(exposure);
        
        // Assert
        assertTrue(errors.isEmpty());
        verify(ruleRepository).findByEnabledTrue();
        verify(rulesEngine, never()).executeRule(anyString(), any(RuleContext.class));
        verify(executionLogRepository, never()).save(any(RuleExecutionLog.class));
    }
    
    @Test
    @DisplayName("Should handle mix of enabled and disabled rules correctly")
    void shouldHandleMixOfEnabledAndDisabledRules() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        
        // Repository should only return enabled rules
        BusinessRule enabledRule1 = createTestRule("RULE_ENABLED_1", true);
        BusinessRule enabledRule2 = createTestRule("RULE_ENABLED_2", true);
        
        when(ruleRepository.findByEnabledTrue()).thenReturn(Arrays.asList(enabledRule1, enabledRule2));
        when(rulesEngine.executeRule(anyString(), any(RuleContext.class)))
            .thenReturn(RuleExecutionResult.success("RULE_ENABLED_1"))
            .thenReturn(RuleExecutionResult.success("RULE_ENABLED_2"));
        when(exemptionRepository.findActiveExemptions(anyString(), anyString(), anyString(), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        
        // Act
        List<ValidationError> errors = service.validateConfigurableRules(exposure);
        
        // Assert
        verify(ruleRepository).findByEnabledTrue();
        verify(rulesEngine, times(2)).executeRule(anyString(), any(RuleContext.class));
        verify(rulesEngine).executeRule(eq("RULE_ENABLED_1"), any(RuleContext.class));
        verify(rulesEngine).executeRule(eq("RULE_ENABLED_2"), any(RuleContext.class));
    }
    
    @Test
    @DisplayName("Should persist execution logs for all enabled rules")
    void shouldPersistExecutionLogsForAllEnabledRules() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        
        BusinessRule rule1 = createTestRule("RULE_001", true);
        BusinessRule rule2 = createTestRule("RULE_002", true);
        
        when(ruleRepository.findByEnabledTrue()).thenReturn(Arrays.asList(rule1, rule2));
        when(rulesEngine.executeRule(anyString(), any(RuleContext.class)))
            .thenReturn(RuleExecutionResult.success("RULE_001"))
            .thenReturn(RuleExecutionResult.success("RULE_002"));
        when(exemptionRepository.findActiveExemptions(anyString(), anyString(), anyString(), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        
        ArgumentCaptor<RuleExecutionLog> logCaptor = ArgumentCaptor.forClass(RuleExecutionLog.class);
        
        // Act
        service.validateConfigurableRules(exposure);
        
        // Assert
        verify(executionLogRepository, times(2)).save(logCaptor.capture());
        
        List<RuleExecutionLog> logs = logCaptor.getAllValues();
        assertEquals(2, logs.size());
        assertEquals("RULE_001", logs.get(0).getRuleId());
        assertEquals("RULE_002", logs.get(1).getRuleId());
        assertEquals(ExecutionResult.SUCCESS, logs.get(0).getExecutionResult());
        assertEquals(ExecutionResult.SUCCESS, logs.get(1).getExecutionResult());
    }
    
    @Test
    @DisplayName("Should handle violations from enabled rules correctly")
    void shouldHandleViolationsFromEnabledRulesCorrectly() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        
        BusinessRule rule = createTestRule("ACCURACY_POSITIVE_AMOUNT", true);
        RuleViolation violation = createTestViolation("ACCURACY_POSITIVE_AMOUNT", "amount", "Amount must be positive");
        
        RuleExecutionResult result = RuleExecutionResult.failure("ACCURACY_POSITIVE_AMOUNT", Collections.singletonList(violation));
        
        when(ruleRepository.findByEnabledTrue()).thenReturn(Collections.singletonList(rule));
        when(rulesEngine.executeRule(eq("ACCURACY_POSITIVE_AMOUNT"), any(RuleContext.class))).thenReturn(result);
        when(exemptionRepository.findActiveExemptions(anyString(), anyString(), anyString(), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        
        // Act
        List<ValidationError> errors = service.validateConfigurableRules(exposure);
        
        // Assert
        assertEquals(1, errors.size());
        assertEquals("ACCURACY_POSITIVE_AMOUNT", errors.get(0).code());
        assertEquals("amount", errors.get(0).fieldName());
        verify(violationRepository).save(violation);
        verify(executionLogRepository).save(any(RuleExecutionLog.class));
    }
    
    // ====================================================================
    // Helper Methods
    // ====================================================================
    
    private ExposureRecord createTestExposure() {
        return ExposureRecord.builder()
            .exposureId("EXP-001")
            .counterpartyId("CP-123")
            .amount(BigDecimal.valueOf(1000000))
            .currency("USD")
            .country("USA")
            .sector("Financial")
            .counterpartyType("Bank")
            .productType("Loan")
            .leiCode("LEI123456789")
            .internalRating("A")
            .riskCategory("Low")
            .riskWeight(BigDecimal.valueOf(0.5))
            .reportingDate(LocalDate.now())
            .valuationDate(LocalDate.now())
            .maturityDate(LocalDate.now().plusYears(1))
            .referenceNumber("REF-001")
            .build();
    }
    
    private BusinessRule createTestRule(String ruleId, boolean enabled) {
        return BusinessRule.builder()
            .ruleId(ruleId)
            .regulationId("REG-001")
            .ruleName("Test Rule " + ruleId)
            .ruleCode(ruleId)
            .description("Test rule description")
            .ruleType(RuleType.ACCURACY)
            .ruleCategory("TEST")
            .severity(Severity.MEDIUM)
            .businessLogic("#amount > 0")
            .executionOrder(100)
            .effectiveDate(LocalDate.now().minusDays(30))
            .expirationDate(null)
            .enabled(enabled)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy("SYSTEM")
            .build();
    }
    
    private RuleViolation createTestViolation(String ruleId, String fieldName, String description) {
        Map<String, Object> details = new HashMap<>();
        details.put("field", fieldName);
        
        return RuleViolation.builder()
            .violationId(null) // Will be auto-generated by database
            .ruleId(ruleId)
            .executionId(1L)
            .entityType("EXPOSURE")
            .entityId("EXP-001")
            .violationType(ruleId)
            .violationDescription(description)
            .violationDetails(details)
            .severity(Severity.MEDIUM)
            .detectedAt(Instant.now())
            .build();
    }
}
