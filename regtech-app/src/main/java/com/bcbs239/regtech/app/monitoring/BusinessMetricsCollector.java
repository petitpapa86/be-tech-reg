package com.bcbs239.regtech.app.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Business Metrics Collector for domain-specific metrics that can't be captured via annotations.
 * 
 * This collector focuses only on business metrics that require custom logic,
 * as Spring Boot 4's @Observed, @Timed, and @Counted annotations handle most infrastructure metrics automatically.
 * 
 * Requirements: 2.5 - Business metrics collection
 */
@Component
public class BusinessMetricsCollector {

    private final MeterRegistry meterRegistry;
    
    // Counters for business events
    private final Counter dataQualityValidationsCounter;
    private final Counter riskCalculationsCounter;
    private final Counter batchProcessingCounter;
    private final Counter reportGenerationCounter;
    
    // Gauges for business state metrics
    private final Map<String, AtomicLong> activeProcesses = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> qualityScores = new ConcurrentHashMap<>();
    
    public BusinessMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize business event counters
        this.dataQualityValidationsCounter = Counter.builder("business.data_quality.validations")
            .description("Number of data quality validations performed")
            .tag("component", "data-quality")
            .register(meterRegistry);
            
        this.riskCalculationsCounter = Counter.builder("business.risk_calculation.calculations")
            .description("Number of risk calculations performed")
            .tag("component", "risk-calculation")
            .register(meterRegistry);
            
        this.batchProcessingCounter = Counter.builder("business.batch.processing")
            .description("Number of batch processing operations")
            .tag("component", "ingestion")
            .register(meterRegistry);
            
        this.reportGenerationCounter = Counter.builder("business.report.generation")
            .description("Number of reports generated")
            .tag("component", "report-generation")
            .register(meterRegistry);
    }

    /**
     * Records data quality score for a specific batch.
     * This metric cannot be captured via annotations as it requires custom business logic.
     * 
     * @param batchId The batch identifier
     * @param score The quality score (0.0 to 1.0)
     */
    public void recordDataQualityScore(String batchId, double score) {
        // Record the score as a gauge (current value)
        Gauge.builder("business.data_quality.score", score, Number::doubleValue)
            .description("Current data quality score for batch")
            .tags("batch.id", batchId, "component", "data-quality")
            .register(meterRegistry);
            
        // Increment validation counter
        dataQualityValidationsCounter.increment();
        
        // Update internal tracking for aggregated metrics
        String key = "quality_score_" + batchId;
        qualityScores.computeIfAbsent(key, k -> {
            AtomicLong atomicScore = new AtomicLong((long) (score * 1000)); // Store as integer (score * 1000)
            Gauge.builder("business.data_quality.score.current", atomicScore, value -> value.get() / 1000.0)
                .description("Current data quality score")
                .tags("batch.id", batchId)
                .register(meterRegistry);
            return atomicScore;
        }).set((long) (score * 1000));
    }

    /**
     * Records a custom business event with tags.
     * Used for business events that don't fit standard patterns.
     * 
     * @param eventType The type of business event
     * @param tags Additional context tags
     */
    public void recordCustomBusinessEvent(String eventType, Map<String, String> tags) {
        Counter.Builder builder = Counter.builder("business.custom.events")
            .description("Custom business events")
            .tag("event.type", eventType);
            
        // Add all provided tags
        for (Map.Entry<String, String> tag : tags.entrySet()) {
            builder.tag(tag.getKey(), tag.getValue());
        }
        
        builder.register(meterRegistry).increment();
    }

    /**
     * Records risk calculation completion with portfolio context.
     * 
     * @param portfolioId The portfolio identifier
     * @param exposureCount Number of exposures processed
     * @param calculationTimeMs Time taken for calculation in milliseconds
     */
    public void recordRiskCalculationCompletion(String portfolioId, int exposureCount, long calculationTimeMs) {
        // Increment calculation counter
        riskCalculationsCounter.increment();
        
        // Record exposure count as a gauge
        Gauge.builder("business.risk_calculation.exposures.count", exposureCount, Number::intValue)
            .description("Number of exposures in portfolio")
            .tags("portfolio.id", portfolioId, "component", "risk-calculation")
            .register(meterRegistry);
            
        // Record calculation time
        Timer.builder("business.risk_calculation.duration")
            .description("Risk calculation duration")
            .tag("portfolio.id", portfolioId)
            .tag("component", "risk-calculation")
            .register(meterRegistry)
            .record(calculationTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Records batch processing metrics.
     * 
     * @param batchId The batch identifier
     * @param recordCount Number of records processed
     * @param processingStatus The final processing status
     */
    public void recordBatchProcessing(String batchId, int recordCount, String processingStatus) {
        // Increment batch processing counter
        batchProcessingCounter.increment();
        
        // Record batch size
        Gauge.builder("business.batch.records.count", recordCount, Number::intValue)
            .description("Number of records in batch")
            .tags("batch.id", batchId, "status", processingStatus, "component", "ingestion")
            .register(meterRegistry);
            
        // Track active processes
        switch (processingStatus) {
            case "PROCESSING" -> {
                String key = "active_batch_" + batchId;
                activeProcesses.computeIfAbsent(key, k -> {
                    AtomicLong activeCount = new AtomicLong(1);
                    Gauge.builder("business.batch.active.count", activeCount, AtomicLong::get)
                            .description("Number of active batch processes")
                            .tags("batch.id", batchId)
                            .register(meterRegistry);
                    return activeCount;
                }).set(1);
            }
            default -> {
                // Remove from active processes when completed
                String key = "active_batch_" + batchId;
                AtomicLong activeCount = activeProcesses.get(key);
                if (activeCount != null) {
                    activeCount.set(0);
                }
            }
        }
    }

    /**
     * Records report generation metrics.
     * 
     * @param reportType The type of report generated
     * @param reportSize Size of generated report in bytes
     * @param generationTimeMs Time taken to generate report
     */
    public void recordReportGeneration(String reportType, long reportSize, long generationTimeMs) {
        // Increment report generation counter
        reportGenerationCounter.increment();
        
        // Record report size
        Gauge.builder("business.report.size.bytes", reportSize, Number::longValue)
            .description("Size of generated report in bytes")
            .tags("report.type", reportType, "component", "report-generation")
            .register(meterRegistry);
            
        // Record generation time
        Timer.builder("business.report.generation.duration")
            .description("Report generation duration")
            .tag("report.type", reportType)
            .tag("component", "report-generation")
            .register(meterRegistry)
            .record(generationTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Records user authentication metrics for security monitoring.
     * 
     * @param authMethod Authentication method used
     * @param success Whether authentication was successful
     * @param userRole Role of the authenticated user
     */
    public void recordAuthentication(String authMethod, boolean success, String userRole) {
        Counter.builder("business.authentication.attempts")
            .description("Authentication attempts")
            .tag("auth.method", authMethod)
            .tag("success", String.valueOf(success))
            .tag("user.role", userRole)
            .tag("component", "iam")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records billing operation metrics.
     * 
     * @param operation The billing operation type
     * @param amount The monetary amount involved
     * @param currency The currency code
     * @param success Whether the operation was successful
     */
    public void recordBillingOperation(String operation, double amount, String currency, boolean success) {
        Counter.builder("business.billing.operations")
            .description("Billing operations")
            .tag("operation", operation)
            .tag("currency", currency)
            .tag("success", String.valueOf(success))
            .tag("component", "billing")
            .register(meterRegistry)
            .increment();
            
        if (success) {
            // Record transaction amount for successful operations
            Gauge.builder("business.billing.transaction.amount", amount, Number::doubleValue)
                .description("Billing transaction amount")
                .tags("operation", operation, "currency", currency, "component", "billing")
                .register(meterRegistry);
        }
    }

    /**
     * Gets current active process count for monitoring.
     * 
     * @return Total number of active processes
     */
    public long getActiveProcessCount() {
        return activeProcesses.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();
    }

    /**
     * Gets average quality score across all batches.
     * 
     * @return Average quality score (0.0 to 1.0)
     */
    public double getAverageQualityScore() {
        if (qualityScores.isEmpty()) {
            return 0.0;
        }
        
        double sum = qualityScores.values().stream()
            .mapToDouble(value -> value.get() / 1000.0)
            .sum();
            
        return sum / qualityScores.size();
    }
}