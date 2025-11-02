package com.bcbs239.regtech.ingestion.infrastructure.compliance;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.repository.IngestionBatchRepository;
import com.bcbs239.regtech.ingestion.infrastructure.persistence.IngestionBatchEntity;
import com.bcbs239.regtech.ingestion.infrastructure.persistence.IngestionBatchRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing data retention and lifecycle policies according to regulatory requirements.
 * Implements comprehensive lifecycle management for ingested files with compliance reporting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataRetentionService {
    
    private final S3Client s3Client;
    private final IngestionBatchRepository batchRepository;
    private final IngestionBatchRepositoryImpl batchRepositoryImpl;
    
    @Value("${regtech.s3.bucket:regtech-data-storage}")
    private String bucketName;
    
    @Value("${regtech.compliance.retention.default-policy:FINANCIAL_DATA_DEFAULT}")
    private String defaultRetentionPolicy;
    
    private final Map<String, DataRetentionPolicy> retentionPolicies = new HashMap<>();
    
    /**
     * Initializes the service with default retention policies.
     */
    public void initializeRetentionPolicies() {
        // Load default policies
        DataRetentionPolicy financialDataPolicy = DataRetentionPolicy.createDefaultFinancialDataPolicy();
        DataRetentionPolicy tempProcessingPolicy = DataRetentionPolicy.createTemporaryProcessingDataPolicy();
        
        retentionPolicies.put(financialDataPolicy.getPolicyId(), financialDataPolicy);
        retentionPolicies.put(tempProcessingPolicy.getPolicyId(), tempProcessingPolicy);
        
        log.info("Initialized {} retention policies", retentionPolicies.size());
    }
    
    /**
     * Configures comprehensive S3 lifecycle policies based on regulatory requirements.
     */
    public Result<Void> configureS3LifecyclePolicies() {
        try {
            log.info("Configuring comprehensive S3 lifecycle policies for bucket {}", bucketName);
            
            List<LifecycleRule> lifecycleRules = new ArrayList<>();
            
            // Create lifecycle rules for each retention policy
            for (DataRetentionPolicy policy : retentionPolicies.values()) {
                LifecycleRule rule = createLifecycleRuleForPolicy(policy);
                lifecycleRules.add(rule);
            }
            
            // Add rule for incomplete multipart uploads cleanup
            LifecycleRule incompleteMultipartRule = LifecycleRule.builder()
                    .id("cleanup-incomplete-multipart-uploads")
                    .status(ExpirationStatus.ENABLED)
                    .filter(LifecycleRuleFilter.builder()
                            .prefix("raw/")
                            .build())
                    .abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder()
                            .daysAfterInitiation(7) // Clean up incomplete uploads after 7 days
                            .build())
                    .build();
            lifecycleRules.add(incompleteMultipartRule);
            
            // Create and apply lifecycle configuration
            BucketLifecycleConfiguration lifecycleConfig = BucketLifecycleConfiguration.builder()
                    .rules(lifecycleRules)
                    .build();
            
            PutBucketLifecycleConfigurationRequest putRequest = 
                PutBucketLifecycleConfigurationRequest.builder()
                    .bucket(bucketName)
                    .lifecycleConfiguration(lifecycleConfig)
                    .build();
            
            s3Client.putBucketLifecycleConfiguration(putRequest);
            
            log.info("Successfully configured {} lifecycle rules for bucket {}", 
                    lifecycleRules.size(), bucketName);
            
            return Result.success();
            
        } catch (Exception e) {
            log.error("Failed to configure S3 lifecycle policies: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of("LIFECYCLE_CONFIG_FAILED", 
                "Failed to configure S3 lifecycle policies: " + e.getMessage()));
        }
    }
    
    /**
     * Creates a lifecycle rule for a specific retention policy.
     */
    private LifecycleRule createLifecycleRuleForPolicy(DataRetentionPolicy policy) {
        String ruleId = "retention-policy-" + policy.getPolicyId().toLowerCase();
        
        LifecycleRule.Builder ruleBuilder = LifecycleRule.builder()
                .id(ruleId)
                .status(ExpirationStatus.ENABLED)
                .filter(LifecycleRuleFilter.builder()
                        .prefix("raw/")
                        .build());
        
        // Add transition to Glacier
        if (policy.getStandardStorageDuration().toDays() > 0) {
            ruleBuilder.transitions(Transition.builder()
                    .days((int) policy.getStandardStorageDuration().toDays())
                    .storageClass(TransitionStorageClass.GLACIER)
                    .build());
        }
        
        // Add transition to Deep Archive for longer retention
        if (policy.getArchiveStorageDuration().toDays() > 365) {
            ruleBuilder.transitions(
                    Transition.builder()
                            .days((int) policy.getStandardStorageDuration().toDays())
                            .storageClass(TransitionStorageClass.GLACIER)
                            .build(),
                    Transition.builder()
                            .days((int) (policy.getStandardStorageDuration().toDays() + 365))
                            .storageClass(TransitionStorageClass.DEEP_ARCHIVE)
                            .build()
            );
        }
        
        // Add expiration rule if early deletion is allowed and no legal hold
        if (policy.isAllowsEarlyDeletion() && !policy.isHasLegalHoldRequirements()) {
            ruleBuilder.expiration(LifecycleExpiration.builder()
                    .days((int) policy.getTotalRetentionPeriod().toDays())
                    .build());
        }
        
        return ruleBuilder.build();
    }
    
    /**
     * Generates a comprehensive compliance report for data retention.
     */
    public Result<ComplianceReportData> generateComplianceReport(Instant reportPeriodStart, 
                                                               Instant reportPeriodEnd) {
        try {
            log.info("Generating compliance report for period {} to {}", 
                    reportPeriodStart, reportPeriodEnd);
            
            // Get all batches in the reporting period
            List<IngestionBatchEntity> batches = batchRepositoryImpl.findBatchEntitiesInPeriod(
                    reportPeriodStart, reportPeriodEnd);
            
            // Analyze S3 storage classes
            Map<String, ComplianceReportData.StorageClassSummary> storageClassBreakdown = 
                    analyzeStorageClasses(batches);
            
            // Analyze retention policies
            Map<String, ComplianceReportData.RetentionPolicySummary> retentionPolicyBreakdown = 
                    analyzeRetentionPolicies(batches);
            
            // Find files approaching expiry
            List<ComplianceReportData.FileRetentionStatus> filesApproachingExpiry = 
                    findFilesApproachingExpiry(batches, 30); // 30 days warning
            
            // Find files eligible for deletion
            List<ComplianceReportData.FileRetentionStatus> filesEligibleForDeletion = 
                    findFilesEligibleForDeletion(batches);
            
            // Check for compliance violations
            List<ComplianceReportData.ComplianceViolation> violations = 
                    checkComplianceViolations(batches);
            
            // Generate audit trail summary
            ComplianceReportData.AuditTrailSummary auditSummary = 
                    generateAuditTrailSummary(reportPeriodStart, reportPeriodEnd);
            
            ComplianceReportData report = ComplianceReportData.builder()
                    .reportGeneratedAt(Instant.now())
                    .reportPeriodStart(reportPeriodStart)
                    .reportPeriodEnd(reportPeriodEnd)
                    .totalFilesUnderRetention(batches.size())
                    .totalStorageSizeBytes(batches.stream()
                            .mapToLong(batch -> batch.getFileSizeBytes() != null ? batch.getFileSizeBytes() : 0L)
                            .sum())
                    .storageClassBreakdown(storageClassBreakdown)
                    .retentionPolicyBreakdown(retentionPolicyBreakdown)
                    .filesApproachingExpiry(filesApproachingExpiry)
                    .filesEligibleForDeletion(filesEligibleForDeletion)
                    .complianceViolations(violations)
                    .auditTrailSummary(auditSummary)
                    .build();
            
            log.info("Generated compliance report: {}", report.getComplianceSummary());
            return Result.success(report);
            
        } catch (Exception e) {
            log.error("Failed to generate compliance report: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of("COMPLIANCE_REPORT_FAILED", 
                "Failed to generate compliance report: " + e.getMessage()));
        }
    }
    
    /**
     * Analyzes storage classes for files in S3.
     */
    private Map<String, ComplianceReportData.StorageClassSummary> analyzeStorageClasses(
            List<IngestionBatchEntity> batches) {
        
        Map<String, List<IngestionBatchEntity>> batchesByStorageClass = new HashMap<>();
        
        // Group batches by storage class (would need to query S3 for actual storage class)
        // For now, we'll simulate based on age
        for (IngestionBatchEntity batch : batches) {
            String storageClass = determineStorageClass(batch);
            batchesByStorageClass.computeIfAbsent(storageClass, k -> new ArrayList<>()).add(batch);
        }
        
        Map<String, ComplianceReportData.StorageClassSummary> summaries = new HashMap<>();
        
        for (Map.Entry<String, List<IngestionBatchEntity>> entry : batchesByStorageClass.entrySet()) {
            String storageClass = entry.getKey();
            List<IngestionBatchEntity> classBatches = entry.getValue();
            
            long totalSize = classBatches.stream()
                    .mapToLong(batch -> batch.getFileSizeBytes() != null ? batch.getFileSizeBytes() : 0L)
                    .sum();
            
            double averageSize = classBatches.isEmpty() ? 0.0 : (double) totalSize / classBatches.size();
            
            Instant oldest = classBatches.stream()
                    .map(IngestionBatchEntity::getUploadedAt)
                    .filter(Objects::nonNull)
                    .min(Instant::compareTo)
                    .orElse(Instant.now());
            
            Instant newest = classBatches.stream()
                    .map(IngestionBatchEntity::getUploadedAt)
                    .filter(Objects::nonNull)
                    .max(Instant::compareTo)
                    .orElse(Instant.now());
            
            ComplianceReportData.StorageClassSummary summary = 
                    ComplianceReportData.StorageClassSummary.builder()
                            .storageClass(storageClass)
                            .fileCount(classBatches.size())
                            .totalSizeBytes(totalSize)
                            .averageFileSizeBytes(averageSize)
                            .oldestFileTimestamp(oldest)
                            .newestFileTimestamp(newest)
                            .build();
            
            summaries.put(storageClass, summary);
        }
        
        return summaries;
    }
    
    /**
     * Determines the likely storage class based on file age.
     */
    private String determineStorageClass(IngestionBatchEntity batch) {
        if (batch.getUploadedAt() == null) {
            return "STANDARD";
        }
        
        long daysOld = ChronoUnit.DAYS.between(batch.getUploadedAt(), Instant.now());
        
        if (daysOld < 180) {
            return "STANDARD";
        } else if (daysOld < 545) { // 18 months
            return "GLACIER";
        } else {
            return "DEEP_ARCHIVE";
        }
    }
    
    /**
     * Analyzes retention policies applied to files.
     */
    private Map<String, ComplianceReportData.RetentionPolicySummary> analyzeRetentionPolicies(
            List<IngestionBatchEntity> batches) {
        
        Map<String, ComplianceReportData.RetentionPolicySummary> summaries = new HashMap<>();
        
        // For each retention policy, analyze the files under it
        for (DataRetentionPolicy policy : retentionPolicies.values()) {
            List<IngestionBatchEntity> policyBatches = batches.stream()
                    .filter(batch -> getPolicyForBatch(batch).equals(policy.getPolicyId()))
                    .collect(Collectors.toList());
            
            long totalSize = policyBatches.stream()
                    .mapToLong(batch -> batch.getFileSizeBytes() != null ? batch.getFileSizeBytes() : 0L)
                    .sum();
            
            long standardStorage = policyBatches.stream()
                    .filter(batch -> "STANDARD".equals(determineStorageClass(batch)))
                    .count();
            
            long archiveStorage = policyBatches.stream()
                    .filter(batch -> !"STANDARD".equals(determineStorageClass(batch)))
                    .count();
            
            long approachingExpiry = policyBatches.stream()
                    .filter(this::isApproachingExpiry)
                    .count();
            
            long eligibleForDeletion = policyBatches.stream()
                    .filter(this::isEligibleForDeletion)
                    .count();
            
            ComplianceReportData.RetentionPolicySummary summary = 
                    ComplianceReportData.RetentionPolicySummary.builder()
                            .policyId(policy.getPolicyId())
                            .policyName(policy.getPolicyName())
                            .fileCount(policyBatches.size())
                            .totalSizeBytes(totalSize)
                            .filesInStandardStorage(standardStorage)
                            .filesInArchiveStorage(archiveStorage)
                            .filesApproachingExpiry(approachingExpiry)
                            .filesEligibleForDeletion(eligibleForDeletion)
                            .build();
            
            summaries.put(policy.getPolicyId(), summary);
        }
        
        return summaries;
    }
    
    /**
     * Finds files approaching their retention expiry date.
     */
    private List<ComplianceReportData.FileRetentionStatus> findFilesApproachingExpiry(
            List<IngestionBatchEntity> batches, int warningDays) {
        
        return batches.stream()
                .filter(batch -> isApproachingExpiry(batch, warningDays))
                .map(this::createFileRetentionStatus)
                .collect(Collectors.toList());
    }
    
    /**
     * Finds files eligible for deletion based on retention policies.
     */
    private List<ComplianceReportData.FileRetentionStatus> findFilesEligibleForDeletion(
            List<IngestionBatchEntity> batches) {
        
        return batches.stream()
                .filter(this::isEligibleForDeletion)
                .map(this::createFileRetentionStatus)
                .collect(Collectors.toList());
    }
    
    /**
     * Creates a file retention status object for a batch.
     */
    private ComplianceReportData.FileRetentionStatus createFileRetentionStatus(IngestionBatchEntity batch) {
        String policyId = getPolicyForBatch(batch);
        DataRetentionPolicy policy = retentionPolicies.get(policyId);
        
        Instant expiryDate = batch.getUploadedAt() != null && policy != null
                ? batch.getUploadedAt().plus(policy.getTotalRetentionPeriod())
                : Instant.now().plus(policy != null ? policy.getTotalRetentionPeriod() : 
                      DataRetentionPolicy.createDefaultFinancialDataPolicy().getTotalRetentionPeriod());
        
        long daysUntilExpiry = ChronoUnit.DAYS.between(Instant.now(), expiryDate);
        
        return ComplianceReportData.FileRetentionStatus.builder()
                .batchId(batch.getBatchId())
                .bankId(batch.getBankId())
                .s3Uri(batch.getS3Uri())
                .currentStorageClass(determineStorageClass(batch))
                .retentionPolicyId(policyId)
                .uploadedAt(batch.getUploadedAt())
                .expiryDate(expiryDate)
                .daysUntilExpiry(daysUntilExpiry)
                .hasLegalHold(policy != null && policy.isHasLegalHoldRequirements())
                .fileSizeBytes(batch.getFileSizeBytes() != null ? batch.getFileSizeBytes() : 0L)
                .build();
    }
    
    /**
     * Checks for compliance violations in the batch data.
     */
    private List<ComplianceReportData.ComplianceViolation> checkComplianceViolations(
            List<IngestionBatchEntity> batches) {
        
        List<ComplianceReportData.ComplianceViolation> violations = new ArrayList<>();
        
        for (IngestionBatchEntity batch : batches) {
            // Check for files that should have been deleted but weren't
            if (isEligibleForDeletion(batch)) {
                String policyId = getPolicyForBatch(batch);
                DataRetentionPolicy policy = retentionPolicies.get(policyId);
                
                if (policy != null && policy.isAllowsEarlyDeletion() && !policy.isHasLegalHoldRequirements()) {
                    violations.add(ComplianceReportData.ComplianceViolation.builder()
                            .violationType("OVERRETENTION")
                            .description("File has exceeded retention period and should be deleted")
                            .batchId(batch.getBatchId())
                            .bankId(batch.getBankId())
                            .s3Uri(batch.getS3Uri())
                            .detectedAt(Instant.now())
                            .severity("MEDIUM")
                            .recommendedAction("Schedule file for deletion")
                            .build());
                }
            }
            
            // Check for missing metadata
            if (batch.getS3Uri() == null || batch.getS3Uri().isEmpty()) {
                violations.add(ComplianceReportData.ComplianceViolation.builder()
                        .violationType("MISSING_S3_REFERENCE")
                        .description("Batch record missing S3 URI reference")
                        .batchId(batch.getBatchId())
                        .bankId(batch.getBankId())
                        .s3Uri(batch.getS3Uri())
                        .detectedAt(Instant.now())
                        .severity("HIGH")
                        .recommendedAction("Investigate and restore S3 reference")
                        .build());
            }
        }
        
        return violations;
    }
    
    /**
     * Generates audit trail summary for the reporting period.
     */
    private ComplianceReportData.AuditTrailSummary generateAuditTrailSummary(
            Instant periodStart, Instant periodEnd) {
        
        // This would typically query audit logs from a dedicated audit table
        // For now, we'll return a placeholder summary
        return ComplianceReportData.AuditTrailSummary.builder()
                .totalAuditEvents(0L)
                .dataAccessEvents(0L)
                .policyChangeEvents(0L)
                .dataDeletionEvents(0L)
                .complianceViolationEvents(0L)
                .oldestAuditEvent(periodStart)
                .newestAuditEvent(periodEnd)
                .build();
    }
    
    /**
     * Determines the retention policy for a given batch.
     */
    private String getPolicyForBatch(IngestionBatchEntity batch) {
        // For now, use default policy for all batches
        // In a real implementation, this might be based on bank type, data type, etc.
        return defaultRetentionPolicy;
    }
    
    /**
     * Checks if a batch is approaching its retention expiry.
     */
    private boolean isApproachingExpiry(IngestionBatchEntity batch) {
        return isApproachingExpiry(batch, 30); // Default 30-day warning
    }
    
    /**
     * Checks if a batch is approaching its retention expiry within the specified warning days.
     */
    private boolean isApproachingExpiry(IngestionBatchEntity batch, int warningDays) {
        if (batch.getUploadedAt() == null) {
            return false;
        }
        
        String policyId = getPolicyForBatch(batch);
        DataRetentionPolicy policy = retentionPolicies.get(policyId);
        
        if (policy == null) {
            return false;
        }
        
        Instant expiryDate = batch.getUploadedAt().plus(policy.getTotalRetentionPeriod());
        Instant warningDate = expiryDate.minus(warningDays, ChronoUnit.DAYS);
        
        return Instant.now().isAfter(warningDate) && Instant.now().isBefore(expiryDate);
    }
    
    /**
     * Checks if a batch is eligible for deletion based on its retention policy.
     */
    private boolean isEligibleForDeletion(IngestionBatchEntity batch) {
        if (batch.getUploadedAt() == null) {
            return false;
        }
        
        String policyId = getPolicyForBatch(batch);
        DataRetentionPolicy policy = retentionPolicies.get(policyId);
        
        if (policy == null || policy.isHasLegalHoldRequirements()) {
            return false;
        }
        
        Instant expiryDate = batch.getUploadedAt().plus(policy.getTotalRetentionPeriod());
        return Instant.now().isAfter(expiryDate);
    }
    
    /**
     * Gets the current retention policies.
     */
    public Map<String, DataRetentionPolicy> getRetentionPolicies() {
        return new HashMap<>(retentionPolicies);
    }
    
    /**
     * Adds or updates a retention policy.
     */
    public Result<Void> updateRetentionPolicy(DataRetentionPolicy policy) {
        if (!policy.isCompliantWithRegulations()) {
            return Result.failure(ErrorDetail.of("POLICY_NOT_COMPLIANT", 
                "Retention policy does not meet regulatory requirements"));
        }
        
        retentionPolicies.put(policy.getPolicyId(), policy);
        log.info("Updated retention policy: {}", policy.getPolicyId());
        
        // Reconfigure S3 lifecycle policies
        return configureS3LifecyclePolicies();
    }
}