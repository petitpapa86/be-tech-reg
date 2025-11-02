package com.bcbs239.regtech.ingestion.web.controller;

import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.infrastructure.compliance.ComplianceReportData;
import com.bcbs239.regtech.ingestion.infrastructure.compliance.DataCleanupService;
import com.bcbs239.regtech.ingestion.infrastructure.compliance.DataRetentionPolicy;
import com.bcbs239.regtech.ingestion.infrastructure.compliance.DataRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * REST controller for data retention and compliance management.
 * Provides endpoints for compliance reporting and retention policy management.
 */
@RestController
@RequestMapping("/api/v1/ingestion/compliance")
@RequiredArgsConstructor
@Slf4j
public class ComplianceController extends BaseController {
    
    private final DataRetentionService dataRetentionService;
    private final DataCleanupService dataCleanupService;
    
    /**
     * Generates a comprehensive compliance report for the specified period.
     */
    @GetMapping("/report")
    public ResponseEntity<ApiResponse<ComplianceReportData>> generateComplianceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Generating compliance report for period {} to {}", startDate, endDate);
        
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        
        Result<ComplianceReportData> result = dataRetentionService.generateComplianceReport(
                startInstant, endInstant);
        
        if (result.isSuccess()) {
            ComplianceReportData report = result.getValue();
            log.info("Generated compliance report: {}", report.getComplianceSummary());
            return ok(report);
        } else {
            log.error("Failed to generate compliance report: {}", result.getError().orElse(null));
            return handleError(result);
        }
    }
    
    /**
     * Gets the current retention policies.
     */
    @GetMapping("/retention-policies")
    public ResponseEntity<ApiResponse<Map<String, DataRetentionPolicy>>> getRetentionPolicies() {
        log.info("Retrieving current retention policies");
        
        Map<String, DataRetentionPolicy> policies = dataRetentionService.getRetentionPolicies();
        return ok(policies);
    }
    
    /**
     * Updates or creates a retention policy.
     */
    @PutMapping("/retention-policies/{policyId}")
    public ResponseEntity<ApiResponse<Void>> updateRetentionPolicy(
            @PathVariable String policyId,
            @RequestBody DataRetentionPolicy policy) {
        
        log.info("Updating retention policy: {}", policyId);
        
        // Ensure the policy ID in the path matches the policy object
        if (!policyId.equals(policy.getPolicyId())) {
            return badRequest("Policy ID in path does not match policy object");
        }
        
        Result<Void> result = dataRetentionService.updateRetentionPolicy(policy);
        
        if (result.isSuccess()) {
            log.info("Successfully updated retention policy: {}", policyId);
            return ok();
        } else {
            log.error("Failed to update retention policy {}: {}", policyId, result.getError().orElse(null));
            return handleError(result);
        }
    }
    
    /**
     * Configures S3 lifecycle policies based on current retention policies.
     */
    @PostMapping("/configure-lifecycle-policies")
    public ResponseEntity<ApiResponse<Void>> configureLifecyclePolicies() {
        log.info("Configuring S3 lifecycle policies");
        
        Result<Void> result = dataRetentionService.configureS3LifecyclePolicies();
        
        if (result.isSuccess()) {
            log.info("Successfully configured S3 lifecycle policies");
            return ok();
        } else {
            log.error("Failed to configure S3 lifecycle policies: {}", result.getError().orElse(null));
            return handleError(result);
        }
    }
    
    /**
     * Gets a summary of compliance status without generating a full report.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ComplianceStatusSummary>> getComplianceStatus() {
        log.info("Retrieving compliance status summary");
        
        // Generate a quick report for the last 30 days
        Instant endDate = Instant.now();
        Instant startDate = endDate.minus(30, java.time.temporal.ChronoUnit.DAYS);
        
        Result<ComplianceReportData> result = dataRetentionService.generateComplianceReport(
                startDate, endDate);
        
        if (result.isSuccess()) {
            ComplianceReportData report = result.getValue();
            ComplianceStatusSummary summary = ComplianceStatusSummary.builder()
                    .isCompliant(report.isCompliant())
                    .complianceScore(report.calculateComplianceScore())
                    .totalFilesUnderRetention(report.getTotalFilesUnderRetention())
                    .totalViolations(report.getComplianceViolations().size())
                    .filesApproachingExpiry(report.getFilesApproachingExpiry().size())
                    .filesEligibleForDeletion(report.getFilesEligibleForDeletion().size())
                    .lastReportGenerated(report.getReportGeneratedAt())
                    .summary(report.getComplianceSummary())
                    .build();
            
            return ok(summary);
        } else {
            log.error("Failed to get compliance status: {}", result.getError().orElse(null));
            return handleError(result);
        }
    }
    
    /**
     * Triggers manual data cleanup operation.
     */
    @PostMapping("/cleanup")
    public ResponseEntity<ApiResponse<DataCleanupService.CleanupSummary>> performDataCleanup() {
        log.info("Manual data cleanup requested");
        
        Result<DataCleanupService.CleanupSummary> result = dataCleanupService.performAutomatedCleanup();
        
        if (result.isSuccess()) {
            DataCleanupService.CleanupSummary summary = result.getValue();
            log.info("Manual data cleanup completed: {}", summary.getSummary());
            return ok(summary);
        } else {
            log.error("Failed to perform manual data cleanup: {}", result.getError().orElse(null));
            return handleError(result);
        }
    }
    
    /**
     * Summary data structure for compliance status.
     */
    public static record ComplianceStatusSummary(
            boolean isCompliant,
            double complianceScore,
            long totalFilesUnderRetention,
            int totalViolations,
            int filesApproachingExpiry,
            int filesEligibleForDeletion,
            Instant lastReportGenerated,
            String summary
    ) {
        public static ComplianceStatusSummaryBuilder builder() {
            return new ComplianceStatusSummaryBuilder();
        }
        
        public static class ComplianceStatusSummaryBuilder {
            private boolean isCompliant;
            private double complianceScore;
            private long totalFilesUnderRetention;
            private int totalViolations;
            private int filesApproachingExpiry;
            private int filesEligibleForDeletion;
            private Instant lastReportGenerated;
            private String summary;
            
            public ComplianceStatusSummaryBuilder isCompliant(boolean isCompliant) {
                this.isCompliant = isCompliant;
                return this;
            }
            
            public ComplianceStatusSummaryBuilder complianceScore(double complianceScore) {
                this.complianceScore = complianceScore;
                return this;
            }
            
            public ComplianceStatusSummaryBuilder totalFilesUnderRetention(long totalFilesUnderRetention) {
                this.totalFilesUnderRetention = totalFilesUnderRetention;
                return this;
            }
            
            public ComplianceStatusSummaryBuilder totalViolations(int totalViolations) {
                this.totalViolations = totalViolations;
                return this;
            }
            
            public ComplianceStatusSummaryBuilder filesApproachingExpiry(int filesApproachingExpiry) {
                this.filesApproachingExpiry = filesApproachingExpiry;
                return this;
            }
            
            public ComplianceStatusSummaryBuilder filesEligibleForDeletion(int filesEligibleForDeletion) {
                this.filesEligibleForDeletion = filesEligibleForDeletion;
                return this;
            }
            
            public ComplianceStatusSummaryBuilder lastReportGenerated(Instant lastReportGenerated) {
                this.lastReportGenerated = lastReportGenerated;
                return this;
            }
            
            public ComplianceStatusSummaryBuilder summary(String summary) {
                this.summary = summary;
                return this;
            }
            
            public ComplianceStatusSummary build() {
                return new ComplianceStatusSummary(
                        isCompliant,
                        complianceScore,
                        totalFilesUnderRetention,
                        totalViolations,
                        filesApproachingExpiry,
                        filesEligibleForDeletion,
                        lastReportGenerated,
                        summary
                );
            }
        }
    }
}