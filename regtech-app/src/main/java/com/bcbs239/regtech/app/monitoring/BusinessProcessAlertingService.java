package com.bcbs239.regtech.app.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Business process failure alerting service for domain-specific monitoring.
 * Monitors batch processing failures, calculation errors, and data quality issues.
 * 
 * Requirements: 5.5
 * - Add domain-specific alert rules for batch processing failures, calculation errors, and data quality issues
 * - Implement business context in alert messages
 * - Add escalation rules for critical business process failures
 */
@Service
public class BusinessProcessAlertingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BusinessProcessAlertingService.class);
    
    private final MeterRegistry meterRegistry;
    private final NotificationService notificationService;
    private final AlertingService alertingService;
    
    // Track business process failures for escalation
    private final Map<String, BusinessProcessFailure> recentFailures = new ConcurrentHashMap<>();
    
    // Escalation thresholds
    private static final int ESCALATION_THRESHOLD = 3; // Number of failures before escalation
    private static final Duration ESCALATION_WINDOW = Duration.ofMinutes(15);
    
    public BusinessProcessAlertingService(
            MeterRegistry meterRegistry,
            NotificationService notificationService,
            AlertingService alertingService) {
        this.meterRegistry = meterRegistry;
        this.notificationService = notificationService;
        this.alertingService = alertingService;
        
        // Initialize business process alert rules
        initializeBusinessProcessAlertRules();
        
        logger.info("BusinessProcessAlertingService initialized");
    }
    
    /**
     * Initializes business process-specific alert rules.
     */
    private void initializeBusinessProcessAlertRules() {
        // Batch processing failure alerts
        alertingService.addAlertRule(new AlertingService.AlertRule(
            "batch-processing-failure",
            "Batch Processing Failure Detected",
            AlertingService.AlertSeverity.CRITICAL,
            "One or more batch processing operations have failed",
            (metrics) -> {
                Double failureRate = getBatchProcessingFailureRate();
                return failureRate != null && failureRate > 0.0; // Any batch failure
            },
            null
        ));
        
        // Risk calculation failure alerts
        alertingService.addAlertRule(new AlertingService.AlertRule(
            "risk-calculation-failure",
            "Risk Calculation Failure Detected",
            AlertingService.AlertSeverity.CRITICAL,
            "One or more risk calculations have failed",
            (metrics) -> {
                Double failureRate = getRiskCalculationFailureRate();
                return failureRate != null && failureRate > 0.0; // Any calculation failure
            },
            null
        ));
        
        // Data quality validation failure alerts
        alertingService.addAlertRule(new AlertingService.AlertRule(
            "data-quality-failure",
            "Data Quality Validation Failure Detected",
            AlertingService.AlertSeverity.WARNING,
            "Data quality validation failure rate exceeded 20%",
            (metrics) -> {
                Double failureRate = getDataQualityFailureRate();
                return failureRate != null && failureRate > 0.20; // 20% failure rate threshold
            },
            null
        ));
        
        // Report generation failure alerts
        alertingService.addAlertRule(new AlertingService.AlertRule(
            "report-generation-failure",
            "Report Generation Failure Detected",
            AlertingService.AlertSeverity.WARNING,
            "One or more report generation operations have failed",
            (metrics) -> {
                Double failureRate = getReportGenerationFailureRate();
                return failureRate != null && failureRate > 0.0; // Any report generation failure
            },
            null
        ));
        
        // Authentication failure spike alerts
        alertingService.addAlertRule(new AlertingService.AlertRule(
            "authentication-failure-spike",
            "Authentication Failure Spike Detected",
            AlertingService.AlertSeverity.WARNING,
            "Authentication failure rate exceeded 30%",
            (metrics) -> {
                Double failureRate = getAuthenticationFailureRate();
                return failureRate != null && failureRate > 0.30; // 30% failure rate threshold
            },
            null
        ));
        
        // Billing operation failure alerts
        alertingService.addAlertRule(new AlertingService.AlertRule(
            "billing-operation-failure",
            "Billing Operation Failure Detected",
            AlertingService.AlertSeverity.CRITICAL,
            "One or more billing operations have failed",
            (metrics) -> {
                Double failureRate = getBillingOperationFailureRate();
                return failureRate != null && failureRate > 0.0; // Any billing failure
            },
            null
        ));
    }
    
    /**
     * Records a business process failure with context for escalation tracking.
     */
    public void recordBusinessProcessFailure(
            String processType,
            String processId,
            String errorMessage,
            Map<String, String> businessContext) {
        
        String key = processType + "_" + processId;
        BusinessProcessFailure failure = new BusinessProcessFailure(
            processType,
            processId,
            errorMessage,
            businessContext,
            Instant.now()
        );
        
        recentFailures.put(key, failure);
        
        logger.warn("Business process failure recorded: {} ({})", processType, processId);
        
        // Check for escalation
        checkForEscalation(processType);
        
        // Clean up old failures
        cleanupOldFailures();
    }
    
    /**
     * Checks if a business process should be escalated based on failure frequency.
     */
    private void checkForEscalation(String processType) {
        Instant windowStart = Instant.now().minus(ESCALATION_WINDOW);
        
        long failureCount = recentFailures.values().stream()
            .filter(f -> f.getProcessType().equals(processType))
            .filter(f -> f.getTimestamp().isAfter(windowStart))
            .count();
        
        if (failureCount >= ESCALATION_THRESHOLD) {
            escalateBusinessProcessFailure(processType, failureCount);
        }
    }
    
    /**
     * Escalates a business process failure to critical severity.
     */
    private void escalateBusinessProcessFailure(String processType, long failureCount) {
        logger.error("Escalating business process failure: {} ({} failures in {} minutes)",
            processType, failureCount, ESCALATION_WINDOW.toMinutes());
        
        // Create escalated alert
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("processType", processType);
        metrics.put("failureCount", failureCount);
        metrics.put("windowMinutes", ESCALATION_WINDOW.toMinutes());
        
        // Get recent failures for this process type
        List<BusinessProcessFailure> recentProcessFailures = recentFailures.values().stream()
            .filter(f -> f.getProcessType().equals(processType))
            .filter(f -> f.getTimestamp().isAfter(Instant.now().minus(ESCALATION_WINDOW)))
            .sorted(Comparator.comparing(BusinessProcessFailure::getTimestamp).reversed())
            .limit(5)
            .toList();
        
        // Add failure details to metrics
        List<Map<String, Object>> failureDetails = new ArrayList<>();
        for (BusinessProcessFailure failure : recentProcessFailures) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("processId", failure.getProcessId());
            detail.put("errorMessage", failure.getErrorMessage());
            detail.put("timestamp", failure.getTimestamp().toString());
            detail.putAll(failure.getBusinessContext());
            failureDetails.add(detail);
        }
        metrics.put("recentFailures", failureDetails);
        
        AlertingService.Alert escalatedAlert = new AlertingService.Alert(
            "escalated-" + processType,
            String.format("ESCALATED: %s Process Failures", formatProcessType(processType)),
            AlertingService.AlertSeverity.CRITICAL,
            String.format("%d %s failures detected in the last %d minutes. Immediate attention required.",
                failureCount, formatProcessType(processType), ESCALATION_WINDOW.toMinutes()),
            Instant.now(),
            metrics
        );
        
        // Send escalated alert
        notificationService.sendAlert(escalatedAlert);
    }
    
    /**
     * Formats process type for display.
     */
    private String formatProcessType(String processType) {
        return Arrays.stream(processType.split("-"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .reduce((a, b) -> a + " " + b)
            .orElse(processType);
    }
    
    /**
     * Cleans up old failures outside the escalation window.
     */
    private void cleanupOldFailures() {
        Instant cutoff = Instant.now().minus(ESCALATION_WINDOW.multipliedBy(2));
        
        recentFailures.entrySet().removeIf(entry -> 
            entry.getValue().getTimestamp().isBefore(cutoff)
        );
    }
    
    /**
     * Scheduled check for business process failures (runs every 2 minutes).
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    public void checkBusinessProcessHealth() {
        logger.debug("Checking business process health");
        
        // Check each business process type
        checkProcessHealth("batch-processing", this::getBatchProcessingFailureRate);
        checkProcessHealth("risk-calculation", this::getRiskCalculationFailureRate);
        checkProcessHealth("data-quality", this::getDataQualityFailureRate);
        checkProcessHealth("report-generation", this::getReportGenerationFailureRate);
        checkProcessHealth("authentication", this::getAuthenticationFailureRate);
        checkProcessHealth("billing", this::getBillingOperationFailureRate);
    }
    
    /**
     * Checks the health of a specific business process.
     */
    private void checkProcessHealth(String processType, java.util.function.Supplier<Double> failureRateSupplier) {
        try {
            Double failureRate = failureRateSupplier.get();
            if (failureRate != null && failureRate > 0.0) {
                logger.debug("Business process {} has failure rate: {}", processType, failureRate);
            }
        } catch (Exception e) {
            logger.debug("Error checking {} health", processType, e);
        }
    }
    
    /**
     * Gets batch processing failure rate.
     */
    private Double getBatchProcessingFailureRate() {
        try {
            Double totalBatches = Search.in(meterRegistry)
                .name("business.batch.processing")
                .meters()
                .stream()
                .filter(m -> m instanceof Counter)
                .mapToDouble(m -> ((Counter) m).count())
                .sum();
            
            Double failedBatches = Search.in(meterRegistry)
                .name("business.batch.processing")
                .tag("status", "FAILED")
                .meters()
                .stream()
                .filter(m -> m instanceof Counter)
                .mapToDouble(m -> ((Counter) m).count())
                .reduce(0.0, Double::sum);
            
            if (totalBatches > 0) {
                return failedBatches / totalBatches;
            }
        } catch (Exception e) {
            logger.debug("Error calculating batch processing failure rate", e);
        }
        return null;
    }
    
    /**
     * Gets risk calculation failure rate.
     */
    private Double getRiskCalculationFailureRate() {
        try {
            Double totalCalculations = Search.in(meterRegistry)
                .name("business.risk_calculation.calculations")
                .meters()
                .stream()
                .filter(m -> m instanceof Counter)
                .mapToDouble(m -> ((Counter) m).count())
                .sum();
            
            Double failedCalculations = Search.in(meterRegistry)
                .name("business.risk_calculation.calculations")
                .tag("status", "FAILED")
                .meters()
                .stream()
                .filter(m -> m instanceof Counter)
                .mapToDouble(m -> ((Counter) m).count())
                .sum();
            
            if (totalCalculations > 0) {
                return failedCalculations / totalCalculations;
            }
        } catch (Exception e) {
            logger.debug("Error calculating risk calculation failure rate", e);
        }
        return null;
    }
    
    /**
     * Gets data quality validation failure rate.
     */
    private Double getDataQualityFailureRate() {
        try {
            Double totalValidations = Search.in(meterRegistry)
                .name("business.data_quality.validations")
                .meters()
                .stream()
                .filter(m -> m instanceof Counter)
                .mapToDouble(m -> ((Counter) m).count())
                .sum();
            
            Double failedValidations = Search.in(meterRegistry)
                .name("business.data_quality.validations")
                .tag("status", "FAILED")
                .meters()
                .stream()
                .filter(m -> m instanceof Counter)
                .mapToDouble(m -> ((Counter) m).count())
                .sum();
            
            if (totalValidations > 0) {
                return failedValidations / totalValidations;
            }
        } catch (Exception e) {
            logger.debug("Error calculating data quality failure rate", e);
        }
        return null;
    }
    
    /**
     * Gets report generation failure rate.
     */
    private Double getReportGenerationFailureRate() {
        try {
            Double totalReports = Search.in(meterRegistry)
                .name("business.report.generation")
                .meters()
                .stream()
                .filter(m -> m instanceof Counter)
                .mapToDouble(m -> ((Counter) m).count())
                .sum();
            
            Double failedReports = Search.in(meterRegistry)
                .name("business.report.generation")
                .tag("status", "FAILED")
                .meters()
                .stream()
                .filter(m -> m instanceof Counter)
                .mapToDouble(m -> ((Counter) m).count())
                .sum();
            
            if (totalReports > 0) {
                return failedReports / totalReports;
            }
        } catch (Exception e) {
            logger.debug("Error calculating report generation failure rate", e);
        }
        return null;
    }
    
    /**
     * Gets authentication failure rate.
     */
    private Double getAuthenticationFailureRate() {
        try {
            Double totalAttempts = Search.in(meterRegistry)
                .name("business.authentication.attempts")
                .meters()
                .stream()
                .filter(m -> m instanceof Counter)
                .mapToDouble(m -> ((Counter) m).count())
                .sum();
            
            Double failedAttempts = Search.in(meterRegistry)
                .name("business.authentication.attempts")
                .tag("success", "false")
                .meters()
                .stream()
                .filter(m -> m instanceof Counter)
                .mapToDouble(m -> ((Counter) m).count())
                .sum();
            
            if (totalAttempts > 0) {
                return failedAttempts / totalAttempts;
            }
        } catch (Exception e) {
            logger.debug("Error calculating authentication failure rate", e);
        }
        return null;
    }
    
    /**
     * Gets billing operation failure rate.
     */
    private Double getBillingOperationFailureRate() {
        try {
            Double totalOperations = Search.in(meterRegistry)
                .name("business.billing.operations")
                .meters()
                .stream()
                .filter(m -> m instanceof Counter)
                .mapToDouble(m -> ((Counter) m).count())
                .sum();
            
            Double failedOperations = Search.in(meterRegistry)
                .name("business.billing.operations")
                .tag("success", "false")
                .meters()
                .stream()
                .filter(m -> m instanceof Counter)
                .mapToDouble(m -> ((Counter) m).count())
                .sum();
            
            if (totalOperations > 0) {
                return failedOperations / totalOperations;
            }
        } catch (Exception e) {
            logger.debug("Error calculating billing operation failure rate", e);
        }
        return null;
    }
    
    /**
     * Gets recent business process failures for monitoring.
     */
    public Collection<BusinessProcessFailure> getRecentFailures() {
        return Collections.unmodifiableCollection(recentFailures.values());
    }
    
    /**
     * Gets business process failure statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFailures", recentFailures.size());
        
        Map<String, Long> failuresByType = new HashMap<>();
        for (BusinessProcessFailure failure : recentFailures.values()) {
            failuresByType.merge(failure.getProcessType(), 1L, Long::sum);
        }
        stats.put("failuresByType", failuresByType);
        
        stats.put("escalationThreshold", ESCALATION_THRESHOLD);
        stats.put("escalationWindowMinutes", ESCALATION_WINDOW.toMinutes());
        
        return stats;
    }
    
    /**
     * Represents a business process failure with context.
     */
    public static class BusinessProcessFailure {
        private final String processType;
        private final String processId;
        private final String errorMessage;
        private final Map<String, String> businessContext;
        private final Instant timestamp;
        
        public BusinessProcessFailure(String processType, String processId, String errorMessage,
                                     Map<String, String> businessContext, Instant timestamp) {
            this.processType = processType;
            this.processId = processId;
            this.errorMessage = errorMessage;
            this.businessContext = Map.copyOf(businessContext);
            this.timestamp = timestamp;
        }
        
        public String getProcessType() { return processType; }
        public String getProcessId() { return processId; }
        public String getErrorMessage() { return errorMessage; }
        public Map<String, String> getBusinessContext() { return businessContext; }
        public Instant getTimestamp() { return timestamp; }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("processType", processType);
            map.put("processId", processId);
            map.put("errorMessage", errorMessage);
            map.put("businessContext", businessContext);
            map.put("timestamp", timestamp.toString());
            return map;
        }
    }
}
