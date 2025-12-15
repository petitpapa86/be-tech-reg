package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.rulesengine.domain.*;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.domain.IBusinessRuleRepository;
import com.bcbs239.regtech.dataquality.rulesengine.domain.IRuleExecutionLogRepository;
import com.bcbs239.regtech.dataquality.rulesengine.domain.IRuleExemptionRepository;
import com.bcbs239.regtech.dataquality.rulesengine.domain.IRuleViolationRepository;
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
    private IBusinessRuleRepository ruleRepository;
    
    @Mock(lenient = true)
    private IRuleViolationRepository violationRepository;
    
    @Mock(lenient = true)
    private IRuleExecutionLogRepository executionLogRepository;
    
    @Mock(lenient = true)
    private IRuleExemptionRepository exemptionRepository;
    
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
    @DisplayName("Should create canonical rule context keys")
    void shouldCreateCanonicalRuleContextKeys() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        BusinessRuleDto rule = createTestRule("RULE_001", true);

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

        // Canonical context keys (single representation)
        assertTrue(ctx.containsKey("exposure_id"), "Context should contain exposure_id");
        assertEquals(exposure.exposureId(), ctx.get("exposure_id"));
        assertTrue(ctx.containsKey("product_type"), "Context should contain product_type");
        assertEquals(exposure.productType(), ctx.get("product_type"));

        // Ensure we don't duplicate keys (performance)
        assertTrue(!ctx.containsKey("exposureId"), "Context should not contain exposureId (no duplication)");
        assertTrue(!ctx.containsKey("productType"), "Context should not contain productType (no duplication)");
    }
    
    // ====================================================================
    // Tests for Rule Enable/Disable Functionality (Task 6)
    // ====================================================================
    
    @Test
    @DisplayName("Should load only enabled rules")
    void shouldLoadOnlyEnabledRules() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        
        BusinessRuleDto enabledRule1 = createTestRule("RULE_001", true);
        BusinessRuleDto enabledRule2 = createTestRule("RULE_002", true);
        
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
        verify(executionLogRepository, times(2)).save(any(RuleExecutionLogDto.class));
    }
    
    @Test
    @DisplayName("Should skip disabled rules during validation")
    void shouldSkipDisabledRules() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        
        // Only enabled rules should be returned by the repository
        BusinessRuleDto enabledRule = createTestRule("RULE_ENABLED", true);
        
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
        verify(executionLogRepository, times(1)).save(any(RuleExecutionLogDto.class));
    }
    
    @Test
    @DisplayName("Should execute all enabled rules for validation")
    void shouldExecuteAllEnabledRules() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        
        BusinessRuleDto rule1 = createTestRule("COMPLETENESS_AMOUNT", true);
        BusinessRuleDto rule2 = createTestRule("ACCURACY_POSITIVE_AMOUNT", true);
        BusinessRuleDto rule3 = createTestRule("VALIDITY_CURRENCY", true);
        
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
        verify(executionLogRepository, times(3)).save(any(RuleExecutionLogDto.class));
    }
    
    @Test
    @DisplayName("Should execute multiple rules for same field and aggregate violations")
    void shouldExecuteMultipleRulesForSameFieldAndAggregate() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        
        // Multiple rules validating the same field (amount)
        BusinessRuleDto rule1 = createTestRule("ACCURACY_POSITIVE_AMOUNT", true);
        BusinessRuleDto rule2 = createTestRule("ACCURACY_REASONABLE_AMOUNT", true);
        BusinessRuleDto rule3 = createTestRule("COMPLETENESS_AMOUNT_REQUIRED", true);
        
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
        verify(executionLogRepository, times(3)).save(any(RuleExecutionLogDto.class));
        
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
        verify(executionLogRepository, never()).save(any(RuleExecutionLogDto.class));
    }
    
    @Test
    @DisplayName("Should handle mix of enabled and disabled rules correctly")
    void shouldHandleMixOfEnabledAndDisabledRules() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        
        // Repository should only return enabled rules
        BusinessRuleDto enabledRule1 = createTestRule("RULE_ENABLED_1", true);
        BusinessRuleDto enabledRule2 = createTestRule("RULE_ENABLED_2", true);
        
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
        
        BusinessRuleDto rule1 = createTestRule("RULE_001", true);
        BusinessRuleDto rule2 = createTestRule("RULE_002", true);
        
        when(ruleRepository.findByEnabledTrue()).thenReturn(Arrays.asList(rule1, rule2));
        when(rulesEngine.executeRule(anyString(), any(RuleContext.class)))
            .thenReturn(RuleExecutionResult.success("RULE_001"))
            .thenReturn(RuleExecutionResult.success("RULE_002"));
        when(exemptionRepository.findActiveExemptions(anyString(), anyString(), anyString(), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        
        ArgumentCaptor<RuleExecutionLogDto> logCaptor = ArgumentCaptor.forClass(RuleExecutionLogDto.class);
        
        // Act
        service.validateConfigurableRules(exposure);
        
        // Assert
        verify(executionLogRepository, times(2)).save(logCaptor.capture());
        
        List<RuleExecutionLogDto> logs = logCaptor.getAllValues();
        assertEquals(2, logs.size());
        assertEquals("RULE_001", logs.get(0).ruleId());
        assertEquals("RULE_002", logs.get(1).ruleId());
        assertEquals(ExecutionResult.SUCCESS, logs.get(0).executionResult());
        assertEquals(ExecutionResult.SUCCESS, logs.get(1).executionResult());
    }
    
    @Test
    @DisplayName("Should handle violations from enabled rules correctly")
    void shouldHandleViolationsFromEnabledRulesCorrectly() {
        // Arrange
        ExposureRecord exposure = createTestExposure();
        
        BusinessRuleDto rule = createTestRule("ACCURACY_POSITIVE_AMOUNT", true);
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
        verify(executionLogRepository).save(any(RuleExecutionLogDto.class));
    }
    
    // ====================================================================
    // Helper Methods
    // ====================================================================
    
    private ExposureRecord createTestExposure() {
        return ExposureRecord.builder()
            .exposureId("EXP-001")
            .counterpartyId("CP-123")
            .amount(BigDecimal.valueOf(1_000_000))
            .currency("USD")
            .country("USA")
            .sector("Financial")
            .counterpartyType("Bank")
            .productType("Loan")
            .leiCode("5493001KJTIIGC8Y1R12")
            .internalRating("A")
            .riskCategory("Low")
            .riskWeight(BigDecimal.valueOf(0.5))
            .reportingDate(LocalDate.now())
            .build();
    }

    private BusinessRuleDto createTestRule(String ruleId, boolean enabled) {
        return new BusinessRuleDto(
            ruleId,
            "REG-001",
            null,
            "Test Rule " + ruleId,
            ruleId,
            "Test rule description",
            RuleType.ACCURACY,
            "TEST",
            Severity.MEDIUM,
            "#amount > 0",
            100,
            LocalDate.now().minusDays(30),
            null,
            enabled,
            Collections.emptyList(),
            Instant.now(),
            Instant.now(),
            "SYSTEM"
        );
    }
    
    private RuleViolation createTestViolation(String ruleId, String fieldName, String description) {
        Map<String, Object> details = new HashMap<>();
        details.put("field", fieldName);
        
        return RuleViolation.builder()
            .ruleId(ruleId)
            .executionId(1L)
            .entityType("EXPOSURE")
            .entityId("EXP-001")
            .violationType(ruleId)
            .violationDescription(description)
            .severity(Severity.MEDIUM)
            .detectedAt(Instant.now())
            .violationDetails(details)
            .build();
    }
}
