package com.bcbs239.regtech.ingestion.infrastructure.compliance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration class for data retention and compliance management.
 * Handles initialization and scheduled tasks for retention policies.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class DataRetentionConfiguration {
    
    private final DataRetentionService dataRetentionService;
    private final DataCleanupService dataCleanupService;
    
    /**
     * Initializes retention policies when the application starts.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeRetentionPolicies() {
        log.info("Initializing data retention policies on application startup");
        
        try {
            dataRetentionService.initializeRetentionPolicies();
            
            // Configure S3 lifecycle policies
            var result = dataRetentionService.configureS3LifecyclePolicies();
            if (result.isSuccess()) {
                log.info("Successfully configured S3 lifecycle policies on startup");
            } else {
                log.warn("Failed to configure S3 lifecycle policies on startup: {}", 
                        result.getError().map(error -> error.getMessage()).orElse("Unknown error"));
            }
            
        } catch (Exception e) {
            log.error("Failed to initialize data retention policies", e);
        }
    }
    
    /**
     * Scheduled task to check and update lifecycle policies daily.
     * Runs at 2 AM every day to avoid peak usage hours.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledLifecyclePolicyUpdate() {
        log.info("Running scheduled lifecycle policy update");
        
        try {
            var result = dataRetentionService.configureS3LifecyclePolicies();
            if (result.isSuccess()) {
                log.info("Successfully updated S3 lifecycle policies via scheduled task");
            } else {
                log.error("Failed to update S3 lifecycle policies via scheduled task: {}", 
                        result.getError().map(error -> error.getMessage()).orElse("Unknown error"));
            }
        } catch (Exception e) {
            log.error("Error during scheduled lifecycle policy update", e);
        }
    }
    
    /**
     * Scheduled task to generate compliance reports weekly.
     * Runs every Sunday at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void scheduledComplianceReporting() {
        log.info("Running scheduled compliance reporting");
        
        try {
            java.time.Instant endDate = java.time.Instant.now();
            java.time.Instant startDate = endDate.minus(7, java.time.temporal.ChronoUnit.DAYS);
            
            var result = dataRetentionService.generateComplianceReport(startDate, endDate);
            if (result.isSuccess()) {
                ComplianceReportData report = result.getValue();
                log.info("Weekly compliance report generated: {}", report.getComplianceSummary());
                
                // Log any high-severity violations
                report.getComplianceViolations().stream()
                        .filter(violation -> "HIGH".equals(violation.getSeverity()))
                        .forEach(violation -> log.warn("HIGH SEVERITY COMPLIANCE VIOLATION: {} - {} (Batch: {})", 
                                violation.getViolationType(), violation.getDescription(), violation.getBatchId()));
                
                // Log files eligible for deletion
                if (!report.getFilesEligibleForDeletion().isEmpty()) {
                    log.info("Found {} files eligible for deletion", report.getFilesEligibleForDeletion().size());
                }
                
            } else {
                log.error("Failed to generate scheduled compliance report: {}", 
                        result.getError().map(error -> error.getMessage()).orElse("Unknown error"));
            }
        } catch (Exception e) {
            log.error("Error during scheduled compliance reporting", e);
        }
    }
    
    /**
     * Scheduled task to perform automated data cleanup monthly.
     * Runs on the first day of each month at 4 AM.
     */
    @Scheduled(cron = "0 0 4 1 * *")
    public void scheduledDataCleanup() {
        log.info("Running scheduled data cleanup");
        
        try {
            var result = dataCleanupService.performAutomatedCleanup();
            if (result.isSuccess()) {
                DataCleanupService.CleanupSummary summary = result.getValue();
                log.info("Scheduled data cleanup completed: {}", summary.getSummary());
                
                // Log warnings if there were deletion errors
                if (summary.deletionErrors() > 0) {
                    log.warn("Data cleanup completed with {} deletion errors", summary.deletionErrors());
                }
                
            } else {
                log.error("Failed to perform scheduled data cleanup: {}", 
                        result.getError().map(error -> error.getMessage()).orElse("Unknown error"));
            }
        } catch (Exception e) {
            log.error("Error during scheduled data cleanup", e);
        }
    }
}