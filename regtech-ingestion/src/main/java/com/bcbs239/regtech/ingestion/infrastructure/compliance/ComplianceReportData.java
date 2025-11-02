package com.bcbs239.regtech.ingestion.infrastructure.compliance;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Data structure for compliance reporting information.
 */
@Value
@Builder
public class ComplianceReportData {
    
    /**
     * Report generation timestamp.
     */
    Instant reportGeneratedAt;
    
    /**
     * Reporting period start.
     */
    Instant reportPeriodStart;
    
    /**
     * Reporting period end.
     */
    Instant reportPeriodEnd;
    
    /**
     * Total number of files under retention management.
     */
    long totalFilesUnderRetention;
    
    /**
     * Total storage size in bytes under retention management.
     */
    long totalStorageSizeBytes;
    
    /**
     * Files by storage class breakdown.
     */
    Map<String, StorageClassSummary> storageClassBreakdown;
    
    /**
     * Files by retention policy breakdown.
     */
    Map<String, RetentionPolicySummary> retentionPolicyBreakdown;
    
    /**
     * Files approaching retention expiry.
     */
    List<FileRetentionStatus> filesApproachingExpiry;
    
    /**
     * Files that have expired and are eligible for deletion.
     */
    List<FileRetentionStatus> filesEligibleForDeletion;
    
    /**
     * Compliance violations or issues found.
     */
    List<ComplianceViolation> complianceViolations;
    
    /**
     * Audit trail summary for the reporting period.
     */
    AuditTrailSummary auditTrailSummary;
    
    @Value
    @Builder
    public static class StorageClassSummary {
        String storageClass;
        long fileCount;
        long totalSizeBytes;
        double averageFileSizeBytes;
        Instant oldestFileTimestamp;
        Instant newestFileTimestamp;
    }
    
    @Value
    @Builder
    public static class RetentionPolicySummary {
        String policyId;
        String policyName;
        long fileCount;
        long totalSizeBytes;
        long filesInStandardStorage;
        long filesInArchiveStorage;
        long filesApproachingExpiry;
        long filesEligibleForDeletion;
    }
    
    @Value
    @Builder
    public static class FileRetentionStatus {
        String batchId;
        String bankId;
        String s3Uri;
        String currentStorageClass;
        String retentionPolicyId;
        Instant uploadedAt;
        Instant expiryDate;
        long daysUntilExpiry;
        boolean hasLegalHold;
        long fileSizeBytes;
    }
    
    @Value
    @Builder
    public static class ComplianceViolation {
        String violationType;
        String description;
        String batchId;
        String bankId;
        String s3Uri;
        Instant detectedAt;
        String severity; // HIGH, MEDIUM, LOW
        String recommendedAction;
    }
    
    @Value
    @Builder
    public static class AuditTrailSummary {
        long totalAuditEvents;
        long dataAccessEvents;
        long policyChangeEvents;
        long dataDeletionEvents;
        long complianceViolationEvents;
        Instant oldestAuditEvent;
        Instant newestAuditEvent;
    }
    
    /**
     * Calculates the compliance score based on violations and policy adherence.
     */
    public double calculateComplianceScore() {
        if (totalFilesUnderRetention == 0) {
            return 100.0; // No files to manage = perfect compliance
        }
        
        // Calculate violation penalty
        double violationPenalty = complianceViolations.stream()
                .mapToDouble(violation -> switch (violation.severity) {
                    case "HIGH" -> 10.0;
                    case "MEDIUM" -> 5.0;
                    case "LOW" -> 1.0;
                    default -> 0.0;
                })
                .sum();
        
        // Calculate base score
        double baseScore = 100.0;
        
        // Deduct points for violations (max 50 points deduction)
        double violationDeduction = Math.min(violationPenalty, 50.0);
        
        // Deduct points for files that should have been deleted but weren't
        double overRetentionPenalty = Math.min(
                (filesEligibleForDeletion.size() * 2.0), 25.0);
        
        return Math.max(baseScore - violationDeduction - overRetentionPenalty, 0.0);
    }
    
    /**
     * Determines if the system is in compliance with regulatory requirements.
     */
    public boolean isCompliant() {
        // Check for high severity violations
        boolean hasHighSeverityViolations = complianceViolations.stream()
                .anyMatch(violation -> "HIGH".equals(violation.severity));
        
        if (hasHighSeverityViolations) {
            return false;
        }
        
        // Check compliance score threshold
        return calculateComplianceScore() >= 85.0;
    }
    
    /**
     * Gets a summary of the compliance status.
     */
    public String getComplianceSummary() {
        double score = calculateComplianceScore();
        boolean compliant = isCompliant();
        
        return String.format(
                "Compliance Status: %s (Score: %.1f%%) - %d files under retention, %d violations found",
                compliant ? "COMPLIANT" : "NON-COMPLIANT",
                score,
                totalFilesUnderRetention,
                complianceViolations.size()
        );
    }
}