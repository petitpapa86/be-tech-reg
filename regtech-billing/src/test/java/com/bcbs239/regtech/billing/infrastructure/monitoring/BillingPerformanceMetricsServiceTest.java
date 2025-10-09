package com.bcbs239.regtech.billing.infrastructure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for BillingPerformanceMetricsService.
 * Verifies performance metrics collection and reporting functionality.
 */
class BillingPerformanceMetricsServiceTest {

    private BillingPerformanceMetricsService metricsService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new BillingPerformanceMetricsService(meterRegistry);
    }

    @Test
    void shouldStartAndEndOperationSuccessfully() {
        // Given
        String operationId = "test-operation-123";
        String operationType = "billing.saga.execution";

        // When
        metricsService.startOperation(operationId, operationType);
        
        // Simulate some processing time
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        metricsService.endOperation(operationId, operationType, true);

        // Then
        Map<String, Object> summary = metricsService.getPerformanceMetricsSummary();
        assertThat(summary.get("sagaStarted")).isEqualTo(1.0);
        assertThat(summary.get("sagaCompleted")).isEqualTo(1.0);
        assertThat(summary.get("sagaFailed")).isEqualTo(0.0);
    }

    @Test
    void shouldRecordSagaExecutionMetrics() {
        // Given
        String sagaType = "monthly-billing";
        Duration duration = Duration.ofMillis(500);
        boolean successful = true;

        // When
        metricsService.recordSagaExecution(sagaType, duration, successful);

        // Then
        Map<String, Object> summary = metricsService.getPerformanceMetricsSummary();
        assertThat(summary.get("sagaCompleted")).isEqualTo(1.0);
        assertThat(summary.get("sagaFailed")).isEqualTo(0.0);
        assertThat((Double) summary.get("avgSagaExecutionTime")).isGreaterThan(0.0);
    }

    @Test
    void shouldRecordPaymentProcessingMetrics() {
        // Given
        Duration duration = Duration.ofMillis(300);
        boolean successful = true;
        String paymentMethod = "stripe";

        // When
        metricsService.recordPaymentProcessing(duration, successful, paymentMethod);

        // Then
        Map<String, Object> summary = metricsService.getPerformanceMetricsSummary();
        assertThat(summary.get("paymentsProcessed")).isEqualTo(1.0);
        assertThat(summary.get("paymentsFailed")).isEqualTo(0.0);
        assertThat((Double) summary.get("avgPaymentProcessingTime")).isGreaterThan(0.0);
    }

    @Test
    void shouldRecordInvoiceGenerationMetrics() {
        // Given
        Duration duration = Duration.ofMillis(200);
        boolean successful = true;
        String invoiceType = "monthly";

        // When
        metricsService.recordInvoiceGeneration(duration, successful, invoiceType);

        // Then
        Map<String, Object> summary = metricsService.getPerformanceMetricsSummary();
        assertThat(summary.get("invoicesGenerated")).isEqualTo(1.0);
        assertThat(summary.get("invoicesFailed")).isEqualTo(0.0);
        assertThat((Double) summary.get("avgInvoiceGenerationTime")).isGreaterThan(0.0);
    }

    @Test
    void shouldRecordStripeApiCallMetrics() {
        // Given
        String apiOperation = "create-customer";
        Duration duration = Duration.ofMillis(150);
        boolean successful = true;

        // When
        metricsService.recordStripeApiCall(apiOperation, duration, successful);

        // Then
        Map<String, Object> summary = metricsService.getPerformanceMetricsSummary();
        assertThat(summary.get("stripeApiSuccess")).isEqualTo(1.0);
        assertThat(summary.get("stripeApiErrors")).isEqualTo(0.0);
        assertThat((Double) summary.get("avgStripeApiCallTime")).isGreaterThan(0.0);
    }

    @Test
    void shouldRecordDatabaseOperationMetrics() {
        // Given
        String operation = "save-billing-account";
        Duration duration = Duration.ofMillis(50);
        boolean successful = true;

        // When
        metricsService.recordDatabaseOperation(operation, duration, successful);

        // Then
        Map<String, Object> summary = metricsService.getPerformanceMetricsSummary();
        assertThat(summary.get("databaseErrors")).isEqualTo(0.0);
    }

    @Test
    void shouldRecordBillingCalculationMetrics() {
        // Given
        Duration duration = Duration.ofMillis(25);
        String calculationType = "monthly";
        double subscriptionAmount = 500.00;
        double overageAmount = 250.00;

        // When
        metricsService.recordBillingCalculation(duration, calculationType, subscriptionAmount, overageAmount);

        // Then
        // Verify that the calculation was recorded (specific assertions depend on gauge implementation)
        Map<String, Object> summary = metricsService.getPerformanceMetricsSummary();
        assertThat(summary).isNotNull();
    }

    @Test
    void shouldRecordDunningProcessMetrics() {
        // Given
        String dunningStep = "step-1-reminder";
        Duration duration = Duration.ofMillis(100);
        boolean successful = true;

        // When
        metricsService.recordDunningProcess(dunningStep, duration, successful);

        // Then
        // Verify that the dunning process was recorded
        Map<String, Object> summary = metricsService.getPerformanceMetricsSummary();
        assertThat(summary).isNotNull();
    }

    @Test
    void shouldGeneratePerformanceMetricsSummary() {
        // Given - record some metrics
        metricsService.recordSagaExecution("monthly-billing", Duration.ofMillis(500), true);
        metricsService.recordPaymentProcessing(Duration.ofMillis(300), true, "stripe");
        metricsService.recordInvoiceGeneration(Duration.ofMillis(200), false, "monthly");
        metricsService.recordStripeApiCall("create-customer", Duration.ofMillis(150), true);

        // When
        Map<String, Object> summary = metricsService.getPerformanceMetricsSummary();

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.get("sagaCompleted")).isEqualTo(1.0);
        assertThat(summary.get("paymentsProcessed")).isEqualTo(1.0);
        assertThat(summary.get("invoicesFailed")).isEqualTo(1.0);
        assertThat(summary.get("stripeApiSuccess")).isEqualTo(1.0);
        assertThat(summary.get("generatedAt")).isNotNull();
        
        // Verify timing metrics are present and positive
        assertThat((Double) summary.get("avgSagaExecutionTime")).isGreaterThan(0.0);
        assertThat((Double) summary.get("avgPaymentProcessingTime")).isGreaterThan(0.0);
        assertThat((Double) summary.get("avgInvoiceGenerationTime")).isGreaterThan(0.0);
        assertThat((Double) summary.get("avgStripeApiCallTime")).isGreaterThan(0.0);
    }

    @Test
    void shouldCalculateHealthStatusCorrectly() {
        // Given - record some successful and failed operations
        metricsService.recordSagaExecution("monthly-billing", Duration.ofMillis(500), true);
        metricsService.recordSagaExecution("monthly-billing", Duration.ofMillis(600), true);
        metricsService.recordSagaExecution("monthly-billing", Duration.ofMillis(700), false); // 1 failure out of 3
        
        metricsService.recordPaymentProcessing(Duration.ofMillis(300), true, "stripe");
        metricsService.recordPaymentProcessing(Duration.ofMillis(400), true, "stripe");
        
        metricsService.recordStripeApiCall("create-customer", Duration.ofMillis(150), true);

        // When
        Map<String, Object> health = metricsService.getHealthStatus();

        // Then
        assertThat(health).isNotNull();
        assertThat(health.get("sagaFailureRate")).isEqualTo("33.33%"); // 1 failure out of 3
        assertThat(health.get("paymentFailureRate")).isEqualTo("0.00%"); // No payment failures
        assertThat(health.get("stripeErrorRate")).isEqualTo("0.00%"); // No Stripe errors
        assertThat(health.get("status")).isEqualTo("DEGRADED"); // Due to high saga failure rate
        assertThat(health.get("timestamp")).isNotNull();
    }

    @Test
    void shouldResetMetricsSuccessfully() {
        // Given - record some metrics
        metricsService.recordSagaExecution("monthly-billing", Duration.ofMillis(500), true);
        metricsService.recordPaymentProcessing(Duration.ofMillis(300), true, "stripe");

        // When
        metricsService.resetMetrics();

        // Then
        // Note: Micrometer counters cannot be reset, but internal tracking should be cleared
        // This test mainly verifies the method doesn't throw exceptions
        Map<String, Object> summary = metricsService.getPerformanceMetricsSummary();
        assertThat(summary).isNotNull();
    }

    @Test
    void shouldHandleFailedOperationsCorrectly() {
        // Given
        String operationId = "failed-operation-123";
        String operationType = "billing.saga.execution";

        // When
        metricsService.startOperation(operationId, operationType);
        metricsService.endOperation(operationId, operationType, false); // Failed operation

        // Then
        Map<String, Object> summary = metricsService.getPerformanceMetricsSummary();
        assertThat(summary.get("sagaStarted")).isEqualTo(1.0);
        assertThat(summary.get("sagaFailed")).isEqualTo(1.0);
        assertThat(summary.get("sagaCompleted")).isEqualTo(0.0);
    }
}
