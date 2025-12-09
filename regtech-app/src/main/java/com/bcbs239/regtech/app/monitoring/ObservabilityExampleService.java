package com.bcbs239.regtech.app.monitoring;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Example service demonstrating @Observed annotations for business operations.
 * 
 * This service shows how to use Spring Boot 4's @Observed, @Timed, and @Counted annotations
 * to automatically create traces and metrics for business operations.
 * 
 * Requirements: 1.1, 2.1, 2.3 - Annotation-based observability
 */
@Service
public class ObservabilityExampleService {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityExampleService.class);
    
    private final TraceContextManager traceContextManager;
    private final BusinessMetricsCollector businessMetricsCollector;

    public ObservabilityExampleService(TraceContextManager traceContextManager, 
                                     BusinessMetricsCollector businessMetricsCollector) {
        this.traceContextManager = traceContextManager;
        this.businessMetricsCollector = businessMetricsCollector;
    }

    /**
     * Example of IAM authentication operation with observability.
     */
    @Observed(
        name = "business.iam.authentication",
        contextualName = "authenticate-user",
        lowCardinalityKeyValues = {"module", "iam", "operation", "authenticate"}
    )
    public boolean authenticateUser(String userId, String password, String authMethod) {
        logger.info("Authenticating user: {} with method: {}", userId, authMethod);
        
        // Add business context to the trace
        traceContextManager.addUserContext(userId, "USER");
        traceContextManager.addOperationContext("authenticate", "user-login");
        
        try {
            // Simulate authentication logic
            boolean success = performAuthentication(userId, password, authMethod);
            
            // Record business metrics
            businessMetricsCollector.recordAuthentication(authMethod, success, "USER");
            
            if (success) {
                traceContextManager.addBusinessContext("auth.outcome", "success");
                logger.info("Authentication successful for user: {}", userId);
            } else {
                traceContextManager.addBusinessContext("auth.outcome", "failure");
                logger.warn("Authentication failed for user: {}", userId);
            }
            
            return success;
            
        } catch (Exception e) {
            traceContextManager.addErrorContext("AUTH_ERROR", "authentication");
            logger.error("Authentication error for user: {}", userId, e);
            throw e;
        }
    }

    /**
     * Example of batch processing operation with observability.
     */
    @Observed(
        name = "business.ingestion.batch.processing",
        contextualName = "process-batch",
        lowCardinalityKeyValues = {"module", "ingestion", "operation", "process"}
    )
    public void processBatch(String batchId, int recordCount) {
        logger.info("Processing batch: {} with {} records", batchId, recordCount);
        
        // Add business context to the trace
        traceContextManager.addBatchContext(batchId, "DAILY");
        traceContextManager.addOperationContext("process", "batch-ingestion");
        traceContextManager.addPerformanceContext("normal", (long) recordCount);
        
        try {
            // Simulate batch processing
            String status = performBatchProcessing(batchId, recordCount);
            
            // Record business metrics
            businessMetricsCollector.recordBatchProcessing(batchId, recordCount, status);
            
            traceContextManager.addBusinessContext("batch.outcome", status);
            logger.info("Batch processing completed: {} with status: {}", batchId, status);
            
        } catch (Exception e) {
            traceContextManager.addErrorContext("BATCH_ERROR", "processing");
            businessMetricsCollector.recordBatchProcessing(batchId, recordCount, "FAILED");
            logger.error("Batch processing failed: {}", batchId, e);
            throw e;
        }
    }

    /**
     * Example of risk calculation operation with observability.
     */
    @Observed(
        name = "business.risk.calculation",
        contextualName = "calculate-portfolio-risk",
        lowCardinalityKeyValues = {"module", "risk-calculation", "operation", "calculate"}
    )
    public double calculatePortfolioRisk(String portfolioId, int exposureCount) {
        logger.info("Calculating risk for portfolio: {} with {} exposures", portfolioId, exposureCount);
        
        // Add business context to the trace
        traceContextManager.addPortfolioContext(portfolioId, "CREDIT");
        traceContextManager.addOperationContext("calculate", "portfolio-risk");
        traceContextManager.addPerformanceContext("normal", (long) exposureCount);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Simulate risk calculation
            double riskScore = performRiskCalculation(portfolioId, exposureCount);
            
            long calculationTime = System.currentTimeMillis() - startTime;
            
            // Record business metrics
            businessMetricsCollector.recordRiskCalculationCompletion(portfolioId, exposureCount, calculationTime);
            
            traceContextManager.addBusinessContext("risk.score", String.valueOf(riskScore));
            traceContextManager.addBusinessContext("calculation.outcome", "success");
            
            logger.info("Risk calculation completed for portfolio: {} with score: {}", portfolioId, riskScore);
            return riskScore;
            
        } catch (Exception e) {
            long calculationTime = System.currentTimeMillis() - startTime;
            traceContextManager.addErrorContext("CALC_ERROR", "calculation");
            logger.error("Risk calculation failed for portfolio: {}", portfolioId, e);
            throw e;
        }
    }

    /**
     * Example of data quality validation with observability.
     */
    @Observed(
        name = "business.data.quality.validation",
        contextualName = "validate-data-quality",
        lowCardinalityKeyValues = {"module", "data-quality", "operation", "validate"}
    )
    public double validateDataQuality(String batchId, int recordCount) {
        logger.info("Validating data quality for batch: {} with {} records", batchId, recordCount);
        
        // Add business context to the trace
        traceContextManager.addBatchContext(batchId, "VALIDATION");
        traceContextManager.addOperationContext("validate", "data-quality");
        traceContextManager.addPerformanceContext("normal", (long) recordCount);
        
        try {
            // Simulate data quality validation
            double qualityScore = performDataQualityValidation(batchId, recordCount);
            
            // Record business metrics
            businessMetricsCollector.recordDataQualityScore(batchId, qualityScore);
            
            traceContextManager.addBusinessContext("quality.score", String.valueOf(qualityScore));
            traceContextManager.addBusinessContext("validation.outcome", "completed");
            
            logger.info("Data quality validation completed for batch: {} with score: {}", batchId, qualityScore);
            return qualityScore;
            
        } catch (Exception e) {
            traceContextManager.addErrorContext("QUALITY_ERROR", "validation");
            logger.error("Data quality validation failed for batch: {}", batchId, e);
            throw e;
        }
    }

    /**
     * Example of report generation with observability.
     */
    @Observed(
        name = "business.report.generation",
        contextualName = "generate-report",
        lowCardinalityKeyValues = {"module", "report-generation", "operation", "generate"}
    )
    public String generateReport(String reportType, String portfolioId) {
        logger.info("Generating report: {} for portfolio: {}", reportType, portfolioId);
        
        // Add business context to the trace
        traceContextManager.addPortfolioContext(portfolioId, "CREDIT");
        traceContextManager.addOperationContext("generate", "report-creation");
        traceContextManager.addBusinessContext("report.type", reportType);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Simulate report generation
            String reportContent = performReportGeneration(reportType, portfolioId);
            
            long generationTime = System.currentTimeMillis() - startTime;
            long reportSize = reportContent.length();
            
            // Record business metrics
            businessMetricsCollector.recordReportGeneration(reportType, reportSize, generationTime);
            
            traceContextManager.addBusinessContext("report.size", String.valueOf(reportSize));
            traceContextManager.addBusinessContext("generation.outcome", "success");
            
            logger.info("Report generation completed: {} for portfolio: {}", reportType, portfolioId);
            return reportContent;
            
        } catch (Exception e) {
            long generationTime = System.currentTimeMillis() - startTime;
            traceContextManager.addErrorContext("REPORT_ERROR", "generation");
            logger.error("Report generation failed: {} for portfolio: {}", reportType, portfolioId, e);
            throw e;
        }
    }

    /**
     * Example of async operation with observability.
     */
    @Observed(
        name = "business.async.processing",
        contextualName = "process-async",
        lowCardinalityKeyValues = {"module", "core", "operation", "async-process"}
    )
    public CompletableFuture<String> processAsync(String taskId, String taskType) {
        logger.info("Starting async processing: {} of type: {}", taskId, taskType);
        
        // Add business context to the trace
        traceContextManager.addBusinessContext("task.id", taskId);
        traceContextManager.addBusinessContext("task.type", taskType);
        traceContextManager.addOperationContext("process", "async-task");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate async processing
                Thread.sleep(100); // Simulate work
                
                String result = "Processed task: " + taskId;
                
                // Note: In real async operations, trace context propagation would be handled
                // by Spring Boot 4's ObservationTaskDecorator
                logger.info("Async processing completed: {}", taskId);
                return result;
                
            } catch (Exception e) {
                logger.error("Async processing failed: {}", taskId, e);
                throw new RuntimeException("Async processing failed", e);
            }
        });
    }

    // Simulation methods for business operations

    private boolean performAuthentication(String userId, String password, String authMethod) {
        // Simulate authentication logic
        return userId != null && !userId.isEmpty() && password != null && password.length() >= 8;
    }

    private String performBatchProcessing(String batchId, int recordCount) {
        // Simulate batch processing
        if (recordCount > 0) {
            return "COMPLETED";
        } else {
            throw new RuntimeException("No records to process");
        }
    }

    private double performRiskCalculation(String portfolioId, int exposureCount) {
        // Simulate risk calculation
        if (exposureCount <= 0) {
            throw new RuntimeException("Invalid exposure count");
        }
        return Math.random() * 100; // Random risk score between 0-100
    }

    private double performDataQualityValidation(String batchId, int recordCount) {
        // Simulate data quality validation
        if (recordCount <= 0) {
            throw new RuntimeException("No records to validate");
        }
        return 0.8 + (Math.random() * 0.2); // Quality score between 0.8-1.0
    }

    private String performReportGeneration(String reportType, String portfolioId) {
        // Simulate report generation
        if (reportType == null || portfolioId == null) {
            throw new RuntimeException("Invalid report parameters");
        }
        return String.format("Report[%s] for Portfolio[%s]: Generated content...", reportType, portfolioId);
    }
}