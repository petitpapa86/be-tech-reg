package com.bcbs239.regtech.billing.infrastructure.monitoring;

import com.bcbs239.regtech.billing.infrastructure.entities.SagaAuditLogEntity;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaSagaAuditLogRepository;
import com.bcbs239.regtech.core.shared.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for BillingSagaAuditService.
 * Verifies audit logging functionality for compliance requirements using closure-based patterns.
 */
class BillingSagaAuditServiceTest {

    private BillingSagaAuditService auditService;
    private ObjectMapper objectMapper;
    private TestSagaAuditLogRepository testRepository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        testRepository = new TestSagaAuditLogRepository();
        auditService = new BillingSagaAuditService(testRepository, objectMapper);
    }

    @Test
    void shouldLogSagaStateChangeSuccessfully() {
        // Given
        String sagaId = "test-saga-123";
        String sagaType = "monthly-billing";
        String previousState = "GATHER_METRICS";
        String newState = "CALCULATE_CHARGES";
        String userId = "user-456";
        String billingAccountId = "billing-account-789";
        
        Map<String, Object> sagaData = new HashMap<>();
        sagaData.put("totalExposures", 15000);
        sagaData.put("billingPeriod", "2024-01");
        
        // No mocking needed - using test repository with closure-based pattern

        // When
        Result<String> result = auditService.logSagaStateChange(
            sagaId, sagaType, previousState, newState, sagaData, userId, billingAccountId
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isPresent();
        assertThat(testRepository.getSavedAuditLogs()).hasSize(1);
        
        SagaAuditLogEntity savedLog = testRepository.getSavedAuditLogs().get(0);
        assertThat(savedLog.getSagaId()).isEqualTo(sagaId);
        assertThat(savedLog.getSagaType()).isEqualTo(sagaType);
        assertThat(savedLog.getEventType()).isEqualTo("STATE_CHANGE");
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getBillingAccountId()).isEqualTo(billingAccountId);
    }

    @Test
    void shouldLogBillingCalculationSuccessfully() {
        // Given
        String sagaId = "test-saga-123";
        String sagaType = "monthly-billing";
        String userId = "user-456";
        String billingAccountId = "billing-account-789";
        
        Map<String, Object> calculationDetails = new HashMap<>();
        calculationDetails.put("subscriptionAmount", "500.00");
        calculationDetails.put("overageAmount", "250.00");
        calculationDetails.put("totalAmount", "750.00");
        calculationDetails.put("totalExposures", 15000);
        calculationDetails.put("exposureLimit", 10000);
        
        // No mocking needed - using test repository with closure-based pattern

        // When
        Result<String> result = auditService.logBillingCalculation(
            sagaId, sagaType, userId, billingAccountId, calculationDetails
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isPresent();
        assertThat(testRepository.getSavedAuditLogs()).hasSize(1);
        
        SagaAuditLogEntity savedLog = testRepository.getSavedAuditLogs().get(0);
        assertThat(savedLog.getEventType()).isEqualTo("BILLING_CALCULATION");
        assertThat(savedLog.getEventData()).contains("subscriptionAmount");
        assertThat(savedLog.getEventData()).contains("overageAmount");
    }

    @Test
    void shouldLogInvoiceAmountDeterminationSuccessfully() {
        // Given
        String sagaId = "test-saga-123";
        String sagaType = "monthly-billing";
        String userId = "user-456";
        String billingAccountId = "billing-account-789";
        String invoiceId = "invoice-999";
        
        Map<String, Object> amountDetails = new HashMap<>();
        amountDetails.put("baseSubscription", "500.00");
        amountDetails.put("usageOverage", "250.00");
        amountDetails.put("finalAmount", "750.00");
        amountDetails.put("calculationMethod", "Base subscription + usage overage");
        
        // No mocking needed - using test repository with closure-based pattern

        // When
        Result<String> result = auditService.logInvoiceAmountDetermination(
            sagaId, sagaType, userId, billingAccountId, invoiceId, amountDetails
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isPresent();
        assertThat(testRepository.getSavedAuditLogs()).hasSize(1);
        
        SagaAuditLogEntity savedLog = testRepository.getSavedAuditLogs().get(0);
        assertThat(savedLog.getEventType()).isEqualTo("INVOICE_AMOUNT_DETERMINATION");
        assertThat(savedLog.getEventData()).contains("invoiceId");
        assertThat(savedLog.getEventData()).contains("finalAmount");
    }

    @Test
    void shouldLogBillingAccountStatusChangeSuccessfully() {
        // Given
        String sagaId = "test-saga-123";
        String sagaType = "monthly-billing";
        String userId = "user-456";
        String billingAccountId = "billing-account-789";
        String previousStatus = "PENDING_VERIFICATION";
        String newStatus = "ACTIVE";
        String reason = "Payment verified successfully";
        
        // No mocking needed - using test repository with closure-based pattern

        // When
        Result<String> result = auditService.logBillingAccountStatusChange(
            sagaId, sagaType, userId, billingAccountId, previousStatus, newStatus, reason
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isPresent();
        assertThat(testRepository.getSavedAuditLogs()).hasSize(1);
        
        SagaAuditLogEntity savedLog = testRepository.getSavedAuditLogs().get(0);
        assertThat(savedLog.getEventType()).isEqualTo("BILLING_ACCOUNT_STATUS_CHANGE");
        assertThat(savedLog.getEventData()).contains("previousStatus");
        assertThat(savedLog.getEventData()).contains("newStatus");
    }

    @Test
    void shouldLogSagaCompletionSuccessfully() {
        // Given
        String sagaId = "test-saga-123";
        String sagaType = "monthly-billing";
        String userId = "user-456";
        String billingAccountId = "billing-account-789";
        boolean successful = true;
        String completionReason = "Billing completed successfully";
        
        Map<String, Object> finalState = new HashMap<>();
        finalState.put("currentStep", "FINALIZE_BILLING");
        finalState.put("totalCharges", "750.00");
        finalState.put("invoiceGenerated", true);
        
        // No mocking needed - using test repository with closure-based pattern

        // When
        Result<String> result = auditService.logSagaCompletion(
            sagaId, sagaType, userId, billingAccountId, successful, completionReason, finalState
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isPresent();
        assertThat(testRepository.getSavedAuditLogs()).hasSize(1);
        
        SagaAuditLogEntity savedLog = testRepository.getSavedAuditLogs().get(0);
        assertThat(savedLog.getEventType()).isEqualTo("SAGA_COMPLETED_SUCCESS");
        assertThat(savedLog.getEventData()).contains("successful");
        assertThat(savedLog.getEventData()).contains("complianceReportingReady");
    }

    @Test
    void shouldGenerateComplianceReportSuccessfully() {
        // Given
        String sagaType = "monthly-billing";
        int days = 30;
        
        // Mock recent events
        List<SagaAuditLogEntity> recentEvents = List.of(
            createMockAuditLogEntity("saga-1", "SAGA_COMPLETED_SUCCESS"),
            createMockAuditLogEntity("saga-2", "SAGA_COMPLETED_SUCCESS"),
            createMockAuditLogEntity("saga-3", "SAGA_COMPLETED_FAILURE")
        );
        
        // Mock statistics
        List<Object[]> statistics = List.of(
            new Object[]{"SAGA_COMPLETED_SUCCESS", 2L},
            new Object[]{"SAGA_COMPLETED_FAILURE", 1L}
        );
        
        // Set up test data in the test repository
        testRepository.setRecentEvents(recentEvents);
        testRepository.setStatistics(statistics);

        // When
        Map<String, Object> report = auditService.generateComplianceReport(sagaType, days);

        // Then
        assertThat(report).isNotNull();
        assertThat(report.get("sagaType")).isEqualTo(sagaType);
        assertThat(report.get("reportPeriodDays")).isEqualTo(days);
        assertThat(report.get("totalEvents")).isEqualTo(3);
        assertThat(report.get("successfulCompletions")).isEqualTo(2L);
        assertThat(report.get("failedCompletions")).isEqualTo(1L);
        assertThat(report.get("successRate")).isEqualTo("66.67%");
        assertThat(report.get("generatedAt")).isNotNull();
    }

    private SagaAuditLogEntity createMockAuditLogEntity(String sagaId, String eventType) {
        return new SagaAuditLogEntity(
            "audit-" + sagaId,
            sagaId,
            "monthly-billing",
            eventType,
            "{}",
            "user-123",
            "billing-account-456"
        );
    }
}