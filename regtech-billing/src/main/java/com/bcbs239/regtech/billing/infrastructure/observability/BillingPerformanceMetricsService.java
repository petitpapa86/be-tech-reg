package com.bcbs239.regtech.billing.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for collecting and reporting billing operation performance metrics.
 * Provides observability for billing processes and saga execution.
 */
@Service
public class BillingPerformanceMetricsService {

    private final MeterRegistry meterRegistry;
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Instant> operationStartTimes = new ConcurrentHashMap<>();

    public BillingPerformanceMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeMetrics();
    }

    private void initializeMetrics() {
        // Initialize common billing operation timers
        getTimer("billing.saga.execution");
        getTimer("billing.payment.processing");
        getTimer("billing.invoice.generation");
        getTimer("billing.stripe.api.call");
        getTimer("billing.database.operation");
        
        // Initialize common billing operation counters
        getCounter("billing.saga.started");
        getCounter("billing.saga.completed");
        getCounter("billing.saga.failed");
        getCounter("billing.payment.processed");
        getCounter("billing.payment.failed");
        getCounter("billing.invoice.generated");
        getCounter("billing.invoice.failed");
        getCounter("billing.stripe.api.success");
        getCounter("billing.stripe.api.error");
        getCounter("billing.database.error");
    }

    private Timer getTimer(String name) {
        return timers.computeIfAbsent(name, n -> 
            Timer.builder(n)
                .description("Timer for " + n)
                .register(meterRegistry));
    }

    private Counter getCounter(String name) {
        return counters.computeIfAbsent(name, n -> 
            Counter.builder(n)
                .description("Counter for " + n)
                .register(meterRegistry));
    }

    /**
     * Start timing an operation
     */
    public void startOperation(String operationId, String operationType) {
        operationStartTimes.put(operationId, Instant.now());
        getCounter(operationType + ".started").increment();
    }

    /**
     * End timing an operation and record the duration
     */
    public void endOperation(String operationId, String operationType, boolean successful) {
        Instant startTime = operationStartTimes.remove(operationId);
        if (startTime != null) {
            Duration duration = Duration.between(startTime, Instant.now());
            getTimer(operationType).record(duration);
        }
        
        if (successful) {
            getCounter(operationType + ".completed").increment();
        } else {
            getCounter(operationType + ".failed").increment();
        }
    }

    /**
     * Record saga execution metrics
     */
    public void recordSagaExecution(String sagaType, Duration duration, boolean successful) {
        getTimer("billing.saga.execution").record(duration);
        
        if (successful) {
            getCounter("billing.saga.completed").increment();
        } else {
            getCounter("billing.saga.failed").increment();
        }
        
        // Record saga-specific metrics
        getTimer("billing.saga." + sagaType.toLowerCase() + ".execution").record(duration);
        getCounter("billing.saga." + sagaType.toLowerCase() + (successful ? ".completed" : ".failed")).increment();
    }

    /**
     * Record payment processing metrics
     */
    public void recordPaymentProcessing(Duration duration, boolean successful, String paymentMethod) {
        getTimer("billing.payment.processing").record(duration);
        
        if (successful) {
            getCounter("billing.payment.processed").increment();
        } else {
            getCounter("billing.payment.failed").increment();
        }
        
        // Record payment method specific metrics
        getCounter("billing.payment." + paymentMethod.toLowerCase() + (successful ? ".success" : ".failure")).increment();
    }

    /**
     * Record invoice generation metrics
     */
    public void recordInvoiceGeneration(Duration duration, boolean successful, String invoiceType) {
        getTimer("billing.invoice.generation").record(duration);
        
        if (successful) {
            getCounter("billing.invoice.generated").increment();
        } else {
            getCounter("billing.invoice.failed").increment();
        }
        
        // Record invoice type specific metrics
        getCounter("billing.invoice." + invoiceType.toLowerCase() + (successful ? ".generated" : ".failed")).increment();
    }

    /**
     * Record Stripe API call metrics
     */
    public void recordStripeApiCall(String apiOperation, Duration duration, boolean successful) {
        getTimer("billing.stripe.api.call").record(duration);
        
        if (successful) {
            getCounter("billing.stripe.api.success").increment();
        } else {
            getCounter("billing.stripe.api.error").increment();
        }
        
        // Record operation-specific metrics
        getTimer("billing.stripe." + apiOperation.toLowerCase()).record(duration);
        getCounter("billing.stripe." + apiOperation.toLowerCase() + (successful ? ".success" : ".error")).increment();
    }

    /**
     * Record database operation metrics
     */
    public void recordDatabaseOperation(String operation, Duration duration, boolean successful) {
        getTimer("billing.database.operation").record(duration);
        
        if (!successful) {
            getCounter("billing.database.error").increment();
        }
        
        // Record operation-specific metrics
        getTimer("billing.database." + operation.toLowerCase()).record(duration);
        if (!successful) {
            getCounter("billing.database." + operation.toLowerCase() + ".error").increment();
        }
    }

    /**
     * Record billing calculation metrics
     */
    public void recordBillingCalculation(Duration duration, String calculationType, 
                                       double subscriptionAmount, double overageAmount) {
        getTimer("billing.calculation." + calculationType.toLowerCase()).record(duration);
        getCounter("billing.calculation.performed").increment();
        
        // Record amount-based metrics
        meterRegistry.gauge("billing.calculation.subscription.amount", subscriptionAmount);
        meterRegistry.gauge("billing.calculation.overage.amount", overageAmount);
        meterRegistry.gauge("billing.calculation.total.amount", subscriptionAmount + overageAmount);
    }

    /**
     * Record dunning process metrics
     */
    public void recordDunningProcess(String dunningStep, Duration duration, boolean successful) {
        getTimer("billing.dunning." + dunningStep.toLowerCase()).record(duration);
        
        if (successful) {
            getCounter("billing.dunning." + dunningStep.toLowerCase() + ".success").increment();
        } else {
            getCounter("billing.dunning." + dunningStep.toLowerCase() + ".failed").increment();
        }
    }

    /**
     * Get current performance metrics summary
     */
    public Map<String, Object> getPerformanceMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // Saga metrics
        summary.put("sagaStarted", getCounterValue("billing.saga.started"));
        summary.put("sagaCompleted", getCounterValue("billing.saga.completed"));
        summary.put("sagaFailed", getCounterValue("billing.saga.failed"));
        
        // Payment metrics
        summary.put("paymentsProcessed", getCounterValue("billing.payment.processed"));
        summary.put("paymentsFailed", getCounterValue("billing.payment.failed"));
        
        // Invoice metrics
        summary.put("invoicesGenerated", getCounterValue("billing.invoice.generated"));
        summary.put("invoicesFailed", getCounterValue("billing.invoice.failed"));
        
        // Stripe API metrics
        summary.put("stripeApiSuccess", getCounterValue("billing.stripe.api.success"));
        summary.put("stripeApiErrors", getCounterValue("billing.stripe.api.error"));
        
        // Database metrics
        summary.put("databaseErrors", getCounterValue("billing.database.error"));
        
        // Average execution times
        summary.put("avgSagaExecutionTime", getTimerMean("billing.saga.execution"));
        summary.put("avgPaymentProcessingTime", getTimerMean("billing.payment.processing"));
        summary.put("avgInvoiceGenerationTime", getTimerMean("billing.invoice.generation"));
        summary.put("avgStripeApiCallTime", getTimerMean("billing.stripe.api.call"));
        
        summary.put("generatedAt", Instant.now().toString());
        
        return summary;
    }

    private double getCounterValue(String counterName) {
        Counter counter = counters.get(counterName);
        return counter != null ? counter.count() : 0.0;
    }

    private double getTimerMean(String timerName) {
        Timer timer = timers.get(timerName);
        return timer != null ? timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS) : 0.0;
    }

    /**
     * Reset all metrics (useful for testing)
     */
    public void resetMetrics() {
        operationStartTimes.clear();
        // Note: Micrometer counters and timers cannot be reset, 
        // but this clears our internal tracking
    }

    /**
     * Get health status based on error rates
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        double sagaFailureRate = calculateFailureRate("billing.saga.completed", "billing.saga.failed");
        double paymentFailureRate = calculateFailureRate("billing.payment.processed", "billing.payment.failed");
        double stripeErrorRate = calculateFailureRate("billing.stripe.api.success", "billing.stripe.api.error");
        
        health.put("sagaFailureRate", String.format("%.2f%%", sagaFailureRate));
        health.put("paymentFailureRate", String.format("%.2f%%", paymentFailureRate));
        health.put("stripeErrorRate", String.format("%.2f%%", stripeErrorRate));
        
        // Determine overall health
        boolean isHealthy = sagaFailureRate < 5.0 && paymentFailureRate < 2.0 && stripeErrorRate < 1.0;
        health.put("status", isHealthy ? "HEALTHY" : "DEGRADED");
        health.put("timestamp", Instant.now().toString());
        
        return health;
    }

    private double calculateFailureRate(String successCounterName, String failureCounterName) {
        double successCount = getCounterValue(successCounterName);
        double failureCount = getCounterValue(failureCounterName);
        double totalCount = successCount + failureCount;
        
        if (totalCount == 0) {
            return 0.0;
        }
        
        return (failureCount / totalCount) * 100.0;
    }
}