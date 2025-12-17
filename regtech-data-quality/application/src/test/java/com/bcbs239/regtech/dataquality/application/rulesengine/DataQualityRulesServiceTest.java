package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.application.validation.ValidationExecutionStats;
import com.bcbs239.regtech.dataquality.application.validation.ValidationResults;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.rulesengine.domain.BusinessRuleDto;
import com.bcbs239.regtech.dataquality.rulesengine.domain.ExecutionResult;
import com.bcbs239.regtech.dataquality.rulesengine.domain.IBusinessRuleRepository;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleExecutionLogDto;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleType;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleViolation;
import com.bcbs239.regtech.dataquality.rulesengine.domain.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataQualityRulesService")
class DataQualityRulesServiceRefactorTest {

    @Mock
    private IBusinessRuleRepository ruleRepository;

    @Mock
    private RuleViolationRepository violationRepository;

    @Mock
    private RuleExecutionLogRepository executionLogRepository;

    @Mock
    private RuleExecutionService ruleExecutionService;

    private DataQualityRulesService service;

    @BeforeEach
    void setUp() {
        service = new DataQualityRulesService(
            ruleRepository,
            violationRepository,
            executionLogRepository,
            ruleExecutionService
        );
    }

    @Test
    @DisplayName("validateNoPersist fetches enabled rules and delegates")
    void validateNoPersistFetchesEnabledRulesAndDelegates() {
        ExposureRecord exposure = ExposureRecord.builder().exposureId("EXP-1").productType("SWAP").build();
        List<BusinessRuleDto> rules = List.of(testRule("R-1"));
        ValidationResults expected = new ValidationResults(
            exposure.exposureId(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            new ValidationExecutionStats()
        );

        when(ruleRepository.findByEnabledTrue()).thenReturn(rules);
        when(ruleExecutionService.execute(exposure, rules)).thenReturn(expected);

        ValidationResults actual = service.validateNoPersist(exposure);

        assertSame(expected, actual);
        verify(ruleRepository).findByEnabledTrue();
        verify(ruleExecutionService).execute(exposure, rules);
    }

    @Test
    @DisplayName("validateConfigurableRules persists logs before violations")
    void validateConfigurableRulesPersistsLogsBeforeViolations() {
        ExposureRecord exposure = ExposureRecord.builder().exposureId("EXP-2").productType("SWAP").build();
        List<BusinessRuleDto> rules = List.of(testRule("R-2"));

        RuleExecutionLogDto log = RuleExecutionLogDto.builder()
            .ruleId("R-2")
            .executionTimestamp(Instant.now())
            .entityType("EXPOSURE")
            .entityId(exposure.exposureId())
            .executionResult(ExecutionResult.SUCCESS)
            .violationCount(1)
            .executionTimeMs(12L)
            .contextData(Map.of())
            .executedBy("test")
            .build();

        RuleViolation violation = RuleViolation.builder()
            .ruleId("R-2")
            .executionId(null)
            .entityType("EXPOSURE")
            .entityId(exposure.exposureId())
            .violationType("TEST")
            .violationDescription("test violation")
            .severity(Severity.HIGH)
            .build();

        List<ValidationError> errors = Collections.emptyList();
        ValidationResults results = new ValidationResults(
            exposure.exposureId(),
            errors,
            List.of(violation),
            List.of(log),
            new ValidationExecutionStats()
        );

        when(ruleRepository.findByEnabledTrue()).thenReturn(rules);
        when(ruleExecutionService.execute(exposure, rules)).thenReturn(results);

        List<ValidationError> returned = service.validateConfigurableRules(exposure);

        assertSame(errors, returned);

        InOrder inOrder = inOrder(executionLogRepository, violationRepository);
        inOrder.verify(executionLogRepository).save(log);
        inOrder.verify(executionLogRepository).flush();
        inOrder.verify(violationRepository).save(violation);
        inOrder.verify(violationRepository).flush();
    }

    private static BusinessRuleDto testRule(String ruleId) {
        return new BusinessRuleDto(
            ruleId,
            "REG-1",
            "TPL-1",
            "Rule " + ruleId,
            ruleId,
            "",
            RuleType.DATA_QUALITY,
            "",
            Severity.MEDIUM,
            "",
            1,
            LocalDate.now().minusDays(1),
            null,
            true,
            Collections.emptyList(),
            Instant.now(),
            Instant.now(),
            "test"
        );
    }
}

/*

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
    @Mock
    private RulesEngine rulesEngine;

    @Mock
    private IBusinessRuleRepository ruleRepository;

    @Mock
    private IRuleViolationRepository violationRepository;

    @Mock
    private IRuleExecutionLogRepository executionLogRepository;

    @Mock
    private IRuleExemptionRepository exemptionRepository;

    @Mock
    private RuleExecutionService ruleExecutionService;

    private DataQualityRulesService service;

    @BeforeEach
    @Mock
        service = new DataQualityRulesService(
            ruleRepository,
            violationRepository,
            executionLogRepository,
            ruleExecutionService
        );
            ruleRepository,

    @Test
    @DisplayName("validateNoPersist fetches enabled rules and delegates")
    void validateNoPersistFetchesEnabledRulesAndDelegates() {
        ExposureRecord exposure = ExposureRecord.builder().exposureId("EXP-1").productType("SWAP").build();
        List<BusinessRuleDto> rules = List.of(testRule("R-1"));
        ValidationResults expected = new ValidationResults(
            exposure.exposureId(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            new ValidationExecutionStats()
        );

        when(ruleRepository.findByEnabledTrue()).thenReturn(rules);
        when(ruleExecutionService.execute(exposure, rules)).thenReturn(expected);

        ValidationResults actual = service.validateNoPersist(exposure);

        assertSame(expected, actual);
        verify(ruleRepository).findByEnabledTrue();
        verify(ruleExecutionService).execute(exposure, rules);
    }

    @Test
    @DisplayName("validateConfigurableRules persists logs before violations")
    void validateConfigurableRulesPersistsLogsBeforeViolations() {
        ExposureRecord exposure = ExposureRecord.builder().exposureId("EXP-2").productType("SWAP").build();
        List<BusinessRuleDto> rules = List.of(testRule("R-2"));

        RuleExecutionLogDto log = RuleExecutionLogDto.builder()
            .ruleId("R-2")
            .executionTimestamp(Instant.now())
            .entityType("EXPOSURE")
            .entityId(exposure.exposureId())
            .executionResult(ExecutionResult.SUCCESS)
            .violationCount(1)
            .executionTimeMs(12L)
            .contextData(Map.of())
            .executedBy("test")
            .build();

        RuleViolation violation = RuleViolation.builder()
            .ruleId("R-2")
            .executionId(null)
            .entityType("EXPOSURE")
            .entityId(exposure.exposureId())
            .violationType("TEST")
            .violationDescription("test violation")
            .severity(Severity.HIGH)
            .build();

        List<ValidationError> errors = Collections.emptyList();
        ValidationResults results = new ValidationResults(
            exposure.exposureId(),
            errors,
            List.of(violation),
            List.of(log),
            new ValidationExecutionStats()
        );

        when(ruleRepository.findByEnabledTrue()).thenReturn(rules);
        when(ruleExecutionService.execute(exposure, rules)).thenReturn(results);

        List<ValidationError> returned = service.validateConfigurableRules(exposure);

        assertSame(errors, returned);

        InOrder inOrder = inOrder(executionLogRepository, violationRepository);
        inOrder.verify(executionLogRepository).save(log);
        inOrder.verify(executionLogRepository).flush();
        inOrder.verify(violationRepository).save(violation);
        inOrder.verify(violationRepository).flush();
    }

    private static BusinessRuleDto testRule(String ruleId) {
        return new BusinessRuleDto(
            ruleId,
            "REG-1",
            "TPL-1",
            "Rule " + ruleId,
            ruleId,
            "",
            RuleType.DATA_QUALITY,
            "",
            Severity.MEDIUM,
            "",
            1,
            LocalDate.now().minusDays(1),
            null,
            true,
            Collections.emptyList(),
            Instant.now(),
            Instant.now(),
            "test"
        );
    }
            violationRepository,
            executionLogRepository,
            ruleExecutionService
        );
    }

    @Test
    @DisplayName("validateNoPersist delegates to RuleExecutionService")
    void validateNoPersistDelegatesToRuleExecutionService() {
        ExposureRecord exposure = createTestExposure();
        List<BusinessRuleDto> rules = List.of(createTestRule("RULE_001"));
        ValidationResults expected = new ValidationResults(
            exposure.exposureId(),
            List.of(),
            List.of(),
            List.of(),
            new ValidationExecutionStats()
        );

        when(ruleRepository.findByEnabledTrue()).thenReturn(rules);
        when(ruleExecutionService.execute(eq(exposure), eq(rules))).thenReturn(expected);

        ValidationResults actual = service.validateNoPersist(exposure);

        assertEquals(expected, actual);
        verify(ruleRepository).findByEnabledTrue();
        verify(ruleExecutionService).execute(eq(exposure), eq(rules));
    }

    @Test
    @DisplayName("validateConfigurableRules persists execution logs then violations")
    void validateConfigurableRulesPersistsLogsThenViolations() {
        ExposureRecord exposure = createTestExposure();
        BusinessRuleDto rule = createTestRule("RULE_001");

        RuleExecutionLogDto log = RuleExecutionLogDto.builder()
            .ruleId("RULE_001")
            .entityType("EXPOSURE")
            .entityId(exposure.exposureId())
            .executionResult(ExecutionResult.SUCCESS)
            .executionTimeMs(10L)
            .violationCount(1)
            .contextData(Map.of())
            .build();

        RuleViolation violation = RuleViolation.builder()
            .ruleId("RULE_001")
            .entityType("EXPOSURE")
            .entityId(exposure.exposureId())
            .violationType("TEST")
            .violationDescription("test")
            .severity(Severity.MEDIUM)
            .detectedAt(Instant.now())
            .violationDetails(Map.of())
            .build();

        ValidationError error = new ValidationError(
            exposure.exposureId(),
            "RULE_001",
            "TEST",
            "test",
            "ACCURACY"
        );

        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(ruleExecutionService.execute(eq(exposure), eq(List.of(rule)))).thenReturn(
            new ValidationResults(
                exposure.exposureId(),
                List.of(error),
                List.of(violation),
                List.of(log),
                new ValidationExecutionStats()
            )
        );

        List<ValidationError> errors = service.validateConfigurableRules(exposure);

        assertEquals(1, errors.size());

        InOrder inOrder = inOrder(executionLogRepository, violationRepository);
        inOrder.verify(executionLogRepository).save(any(RuleExecutionLogDto.class));
        inOrder.verify(executionLogRepository).flush();
        inOrder.verify(violationRepository).save(any(RuleViolation.class));
        inOrder.verify(violationRepository).flush();
        

    private static ExposureRecord createTestExposure() {
        return ExposureRecord.builder()
            .exposureId("E1")
            .counterpartyId("C1")
            .amount(BigDecimal.TEN)
            .currency("USD")
            .country("US")
            .sector("TECH")
            .counterpartyType("BANK")
            .productType("LOAN")
            .leiCode("LEI")
            .internalRating("A")
            .riskCategory("LOW")
            .riskWeight(BigDecimal.ONE)
            .reportingDate(LocalDate.of(2024, 1, 1))
            .valuationDate(LocalDate.of(2024, 1, 1))
            .maturityDate(LocalDate.of(2030, 1, 1))
            .referenceNumber("R1")
            .build();
    }

    private static BusinessRuleDto createTestRule(String ruleCode) {
        return BusinessRuleDto.builder()
            .ruleCode(ruleCode)
            .enabled(true)
            .active(true)
            .ruleType(RuleType.DATA_QUALITY)
            .build();
    }
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
*/
