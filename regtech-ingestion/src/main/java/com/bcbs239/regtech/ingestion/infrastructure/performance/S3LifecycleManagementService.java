package com.bcbs239.regtech.ingestion.infrastructure.performance;

import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.model.BatchStatus;
import com.bcbs239.regtech.ingestion.infrastructure.persistence.IngestionBatchJpaRepository;
import com.bcbs239.regtech.ingestion.infrastructure.persistence.IngestionBatchEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for managing S3 lifecycle policies and automatically archiving old files to Glacier.
 * Implements cost optimization strategies for long-term data retention.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3LifecycleManagementService {

    private final S3Client s3Client;
    private final IngestionBatchJpaRepository batchRepository;

    @Value("${ingestion.s3.bucket-name:regtech-data-storage}")
    private String bucketName;

    @Value("${ingestion.s3.lifecycle.glacier-transition-days:180}")
    private int glacierTransitionDays;

    @Value("${ingestion.s3.lifecycle.deep-archive-transition-days:365}")
    private int deepArchiveTransitionDays;

    @Value("${ingestion.s3.lifecycle.deletion-days:2555}") // 7 years
    private int deletionDays;

    @Value("${ingestion.s3.lifecycle.incomplete-multipart-days:7}")
    private int incompleteMultipartDays;

    /**
     * Configure S3 lifecycle policies for automatic archiving.
     */
    public Result<Void> configureLifecyclePolicies() {
        log.info("Configuring S3 lifecycle policies for bucket: {}", bucketName);

        try {
            // Create lifecycle configuration
            LifecycleConfiguration lifecycleConfig = LifecycleConfiguration.builder()
                .rules(
                    // Rule for raw ingestion files
                    LifecycleRule.builder()
                        .id("ingestion-files-lifecycle")
                        .status(ExpirationStatus.ENABLED)
                        .filter(LifecycleRuleFilter.builder()
                            .prefix("raw/")
                            .build())
                        .transitions(
                            Transition.builder()
                                .days(glacierTransitionDays)
                                .storageClass(StorageClass.GLACIER)
                                .build(),
                            Transition.builder()
                                .days(deepArchiveTransitionDays)
                                .storageClass(StorageClass.DEEP_ARCHIVE)
                                .build()
                        )
                        .expiration(LifecycleExpiration.builder()
                            .days(deletionDays)
                            .build())
                        .build(),

                    // Rule for incomplete multipart uploads cleanup
                    LifecycleRule.builder()
                        .id("cleanup-incomplete-multipart")
                        .status(ExpirationStatus.ENABLED)
                        .abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder()
                            .daysAfterInitiation(incompleteMultipartDays)
                            .build())
                        .build()
                )
                .build();

            // Apply lifecycle configuration
            PutBucketLifecycleConfigurationRequest request = PutBucketLifecycleConfigurationRequest.builder()
                .bucket(bucketName)
                .lifecycleConfiguration(lifecycleConfig)
                .build();

            s3Client.putBucketLifecycleConfiguration(request);

            log.info("Successfully configured S3 lifecycle policies: Glacier after {} days, Deep Archive after {} days, Deletion after {} days",
                glacierTransitionDays, deepArchiveTransitionDays, deletionDays);

            LoggingConfiguration.logStructured("S3_LIFECYCLE_CONFIGURED", Map.of(
                "bucketName", bucketName,
                "glacierTransitionDays", glacierTransitionDays,
                "deepArchiveTransitionDays", deepArchiveTransitionDays,
                "deletionDays", deletionDays,
                "incompleteMultipartDays", incompleteMultipartDays
            ));

            return Result.success(null);

        } catch (S3Exception e) {
            log.error("Failed to configure S3 lifecycle policies for bucket: {}", bucketName, e);
            return Result.failure(ErrorDetail.of("S3_LIFECYCLE_ERROR",
                "Failed to configure lifecycle policies: " + e.getMessage()));
        }
    }

    /**
     * Manually archive old files to Glacier for immediate cost savings.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void archiveOldFiles() {
        log.info("Starting manual archival of old files to Glacier");

        try {
            Instant cutoffDate = Instant.now().minus(glacierTransitionDays, ChronoUnit.DAYS);
            
            // Find completed batches older than the cutoff date
            List<IngestionBatchEntity> oldBatches = batchRepository.findByUploadedAtBetween(
                Instant.EPOCH, cutoffDate)
                .stream()
                .filter(batch -> batch.getStatus() == BatchStatus.COMPLETED)
                .filter(batch -> batch.getS3Key() != null)
                .toList();

            if (oldBatches.isEmpty()) {
                log.info("No old files found for archival");
                return;
            }

            log.info("Found {} old files for potential archival", oldBatches.size());

            AtomicInteger archivedCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            oldBatches.parallelStream().forEach(batch -> {
                try {
                    Result<Void> result = archiveFileToGlacier(batch.getS3Key(), batch.getBatchId());
                    if (result.isSuccess()) {
                        archivedCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                        log.warn("Failed to archive file for batch {}: {}", 
                            batch.getBatchId(), result.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.error("Error archiving file for batch {}: {}", batch.getBatchId(), e.getMessage(), e);
                }
            });

            log.info("Archival completed: {} files archived, {} errors", 
                archivedCount.get(), errorCount.get());

            LoggingConfiguration.logStructured("ARCHIVAL_COMPLETED", Map.of(
                "totalFilesProcessed", oldBatches.size(),
                "filesArchived", archivedCount.get(),
                "errors", errorCount.get(),
                "cutoffDate", cutoffDate.toString()
            ));

        } catch (Exception e) {
            log.error("Error during scheduled archival process", e);
        }
    }

    /**
     * Archive a specific file to Glacier storage class.
     */
    public Result<Void> archiveFileToGlacier(String s3Key, String batchId) {
        try {
            // Check current storage class
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

            HeadObjectResponse headResponse = s3Client.headObject(headRequest);
            
            // Skip if already in Glacier or Deep Archive
            if (headResponse.storageClass() == StorageClass.GLACIER || 
                headResponse.storageClass() == StorageClass.DEEP_ARCHIVE) {
                log.debug("File {} is already archived (storage class: {})", s3Key, headResponse.storageClass());
                return Result.success(null);
            }

            // Copy object to change storage class to Glacier
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(s3Key)
                .destinationBucket(bucketName)
                .destinationKey(s3Key)
                .storageClass(StorageClass.GLACIER)
                .metadataDirective(MetadataDirective.COPY)
                .build();

            s3Client.copyObject(copyRequest);

            log.info("Successfully archived file {} to Glacier for batch {}", s3Key, batchId);

            LoggingConfiguration.logStructured("FILE_ARCHIVED_TO_GLACIER", Map.of(
                "s3Key", s3Key,
                "batchId", batchId,
                "previousStorageClass", headResponse.storageClass() != null ? headResponse.storageClass().toString() : "STANDARD",
                "newStorageClass", "GLACIER"
            ));

            return Result.success(null);

        } catch (S3Exception e) {
            log.error("Failed to archive file {} to Glacier for batch {}: {}", s3Key, batchId, e.getMessage());
            return Result.failure(ErrorDetail.of("ARCHIVAL_ERROR",
                "Failed to archive file to Glacier: " + e.getMessage()));
        }
    }

    /**
     * Get storage cost analysis for files in different storage classes.
     */
    public Result<StorageCostAnalysis> analyzeStorageCosts() {
        log.info("Analyzing storage costs for bucket: {}", bucketName);

        try {
            // List objects with their storage classes
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix("raw/")
                .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            StorageCostAnalysis.Builder analysisBuilder = StorageCostAnalysis.builder()
                .bucketName(bucketName)
                .analysisDate(Instant.now());

            long standardStorage = 0;
            long glacierStorage = 0;
            long deepArchiveStorage = 0;
            int standardCount = 0;
            int glacierCount = 0;
            int deepArchiveCount = 0;

            for (S3Object object : listResponse.contents()) {
                StorageClass storageClass = object.storageClass();
                long size = object.size();

                if (storageClass == null || storageClass == StorageClass.STANDARD) {
                    standardStorage += size;
                    standardCount++;
                } else if (storageClass == StorageClass.GLACIER) {
                    glacierStorage += size;
                    glacierCount++;
                } else if (storageClass == StorageClass.DEEP_ARCHIVE) {
                    deepArchiveStorage += size;
                    deepArchiveCount++;
                }
            }

            // Calculate estimated monthly costs (approximate AWS pricing)
            double standardCostPerGB = 0.023; // $0.023 per GB/month
            double glacierCostPerGB = 0.004;   // $0.004 per GB/month
            double deepArchiveCostPerGB = 0.00099; // $0.00099 per GB/month

            double standardMonthlyCost = (standardStorage / 1024.0 / 1024.0 / 1024.0) * standardCostPerGB;
            double glacierMonthlyCost = (glacierStorage / 1024.0 / 1024.0 / 1024.0) * glacierCostPerGB;
            double deepArchiveMonthlyCost = (deepArchiveStorage / 1024.0 / 1024.0 / 1024.0) * deepArchiveCostPerGB;

            StorageCostAnalysis analysis = analysisBuilder
                .standardStorageBytes(standardStorage)
                .glacierStorageBytes(glacierStorage)
                .deepArchiveStorageBytes(deepArchiveStorage)
                .standardFileCount(standardCount)
                .glacierFileCount(glacierCount)
                .deepArchiveFileCount(deepArchiveCount)
                .estimatedStandardMonthlyCost(standardMonthlyCost)
                .estimatedGlacierMonthlyCost(glacierMonthlyCost)
                .estimatedDeepArchiveMonthlyCost(deepArchiveMonthlyCost)
                .totalEstimatedMonthlyCost(standardMonthlyCost + glacierMonthlyCost + deepArchiveMonthlyCost)
                .build();

            log.info("Storage cost analysis completed: {} files, {:.2f} GB total, ${:.2f} estimated monthly cost",
                standardCount + glacierCount + deepArchiveCount,
                (standardStorage + glacierStorage + deepArchiveStorage) / 1024.0 / 1024.0 / 1024.0,
                analysis.getTotalEstimatedMonthlyCost());

            return Result.success(analysis);

        } catch (S3Exception e) {
            log.error("Failed to analyze storage costs for bucket: {}", bucketName, e);
            return Result.failure(ErrorDetail.of("COST_ANALYSIS_ERROR",
                "Failed to analyze storage costs: " + e.getMessage()));
        }
    }

    /**
     * Get lifecycle policy status and configuration.
     */
    public Result<LifecyclePolicyStatus> getLifecyclePolicyStatus() {
        try {
            GetBucketLifecycleConfigurationRequest request = GetBucketLifecycleConfigurationRequest.builder()
                .bucket(bucketName)
                .build();

            GetBucketLifecycleConfigurationResponse response = s3Client.getBucketLifecycleConfiguration(request);

            LifecyclePolicyStatus status = LifecyclePolicyStatus.builder()
                .bucketName(bucketName)
                .policyConfigured(true)
                .rules(response.rules().stream()
                    .map(rule -> Map.of(
                        "id", rule.id(),
                        "status", rule.status().toString(),
                        "glacierTransitionDays", rule.transitions().stream()
                            .filter(t -> t.storageClass() == StorageClass.GLACIER)
                            .findFirst()
                            .map(t -> t.days().toString())
                            .orElse("Not configured"),
                        "deepArchiveTransitionDays", rule.transitions().stream()
                            .filter(t -> t.storageClass() == StorageClass.DEEP_ARCHIVE)
                            .findFirst()
                            .map(t -> t.days().toString())
                            .orElse("Not configured")
                    ))
                    .toList())
                .build();

            return Result.success(status);

        } catch (NoSuchLifecycleConfigurationException e) {
            LifecyclePolicyStatus status = LifecyclePolicyStatus.builder()
                .bucketName(bucketName)
                .policyConfigured(false)
                .rules(List.of())
                .build();

            return Result.success(status);

        } catch (S3Exception e) {
            log.error("Failed to get lifecycle policy status for bucket: {}", bucketName, e);
            return Result.failure(ErrorDetail.of("LIFECYCLE_STATUS_ERROR",
                "Failed to get lifecycle policy status: " + e.getMessage()));
        }
    }

    // Data classes for analysis results

    public static class StorageCostAnalysis {
        private final String bucketName;
        private final Instant analysisDate;
        private final long standardStorageBytes;
        private final long glacierStorageBytes;
        private final long deepArchiveStorageBytes;
        private final int standardFileCount;
        private final int glacierFileCount;
        private final int deepArchiveFileCount;
        private final double estimatedStandardMonthlyCost;
        private final double estimatedGlacierMonthlyCost;
        private final double estimatedDeepArchiveMonthlyCost;
        private final double totalEstimatedMonthlyCost;

        private StorageCostAnalysis(Builder builder) {
            this.bucketName = builder.bucketName;
            this.analysisDate = builder.analysisDate;
            this.standardStorageBytes = builder.standardStorageBytes;
            this.glacierStorageBytes = builder.glacierStorageBytes;
            this.deepArchiveStorageBytes = builder.deepArchiveStorageBytes;
            this.standardFileCount = builder.standardFileCount;
            this.glacierFileCount = builder.glacierFileCount;
            this.deepArchiveFileCount = builder.deepArchiveFileCount;
            this.estimatedStandardMonthlyCost = builder.estimatedStandardMonthlyCost;
            this.estimatedGlacierMonthlyCost = builder.estimatedGlacierMonthlyCost;
            this.estimatedDeepArchiveMonthlyCost = builder.estimatedDeepArchiveMonthlyCost;
            this.totalEstimatedMonthlyCost = builder.totalEstimatedMonthlyCost;
        }

        public static Builder builder() { return new Builder(); }

        // Getters
        public String getBucketName() { return bucketName; }
        public Instant getAnalysisDate() { return analysisDate; }
        public long getStandardStorageBytes() { return standardStorageBytes; }
        public long getGlacierStorageBytes() { return glacierStorageBytes; }
        public long getDeepArchiveStorageBytes() { return deepArchiveStorageBytes; }
        public int getStandardFileCount() { return standardFileCount; }
        public int getGlacierFileCount() { return glacierFileCount; }
        public int getDeepArchiveFileCount() { return deepArchiveFileCount; }
        public double getEstimatedStandardMonthlyCost() { return estimatedStandardMonthlyCost; }
        public double getEstimatedGlacierMonthlyCost() { return estimatedGlacierMonthlyCost; }
        public double getEstimatedDeepArchiveMonthlyCost() { return estimatedDeepArchiveMonthlyCost; }
        public double getTotalEstimatedMonthlyCost() { return totalEstimatedMonthlyCost; }

        public static class Builder {
            private String bucketName;
            private Instant analysisDate;
            private long standardStorageBytes;
            private long glacierStorageBytes;
            private long deepArchiveStorageBytes;
            private int standardFileCount;
            private int glacierFileCount;
            private int deepArchiveFileCount;
            private double estimatedStandardMonthlyCost;
            private double estimatedGlacierMonthlyCost;
            private double estimatedDeepArchiveMonthlyCost;
            private double totalEstimatedMonthlyCost;

            public Builder bucketName(String bucketName) { this.bucketName = bucketName; return this; }
            public Builder analysisDate(Instant analysisDate) { this.analysisDate = analysisDate; return this; }
            public Builder standardStorageBytes(long standardStorageBytes) { this.standardStorageBytes = standardStorageBytes; return this; }
            public Builder glacierStorageBytes(long glacierStorageBytes) { this.glacierStorageBytes = glacierStorageBytes; return this; }
            public Builder deepArchiveStorageBytes(long deepArchiveStorageBytes) { this.deepArchiveStorageBytes = deepArchiveStorageBytes; return this; }
            public Builder standardFileCount(int standardFileCount) { this.standardFileCount = standardFileCount; return this; }
            public Builder glacierFileCount(int glacierFileCount) { this.glacierFileCount = glacierFileCount; return this; }
            public Builder deepArchiveFileCount(int deepArchiveFileCount) { this.deepArchiveFileCount = deepArchiveFileCount; return this; }
            public Builder estimatedStandardMonthlyCost(double estimatedStandardMonthlyCost) { this.estimatedStandardMonthlyCost = estimatedStandardMonthlyCost; return this; }
            public Builder estimatedGlacierMonthlyCost(double estimatedGlacierMonthlyCost) { this.estimatedGlacierMonthlyCost = estimatedGlacierMonthlyCost; return this; }
            public Builder estimatedDeepArchiveMonthlyCost(double estimatedDeepArchiveMonthlyCost) { this.estimatedDeepArchiveMonthlyCost = estimatedDeepArchiveMonthlyCost; return this; }
            public Builder totalEstimatedMonthlyCost(double totalEstimatedMonthlyCost) { this.totalEstimatedMonthlyCost = totalEstimatedMonthlyCost; return this; }

            public StorageCostAnalysis build() { return new StorageCostAnalysis(this); }
        }
    }

    public static class LifecyclePolicyStatus {
        private final String bucketName;
        private final boolean policyConfigured;
        private final List<Map<String, String>> rules;

        private LifecyclePolicyStatus(Builder builder) {
            this.bucketName = builder.bucketName;
            this.policyConfigured = builder.policyConfigured;
            this.rules = builder.rules;
        }

        public static Builder builder() { return new Builder(); }

        // Getters
        public String getBucketName() { return bucketName; }
        public boolean isPolicyConfigured() { return policyConfigured; }
        public List<Map<String, String>> getRules() { return rules; }

        public static class Builder {
            private String bucketName;
            private boolean policyConfigured;
            private List<Map<String, String>> rules;

            public Builder bucketName(String bucketName) { this.bucketName = bucketName; return this; }
            public Builder policyConfigured(boolean policyConfigured) { this.policyConfigured = policyConfigured; return this; }
            public Builder rules(List<Map<String, String>> rules) { this.rules = rules; return this; }

            public LifecyclePolicyStatus build() { return new LifecyclePolicyStatus(this); }
        }
    }
}