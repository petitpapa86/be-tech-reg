package com.bcbs239.regtech.modules.ingestion.infrastructure.batch.persistence;

import com.bcbs239.regtech.modules.ingestion.domain.batch.*;
import com.bcbs239.regtech.modules.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.modules.ingestion.domain.bankinfo.BankInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity mapping to the ingestion_batches table.
 * Maps domain aggregate IngestionBatch to database representation.
 */
@Setter
@Getter
@Entity
@Table(name = "ingestion_batches", schema = "regtech")
public class IngestionBatchEntity {

    // Getters and setters
    @Id
    @Column(name = "batch_id", length = 50)
    private String batchId;
    
    @Column(name = "bank_id", length = 20, nullable = false)
    private String bankId;
    
    @Column(name = "bank_name", length = 100)
    private String bankName;
    
    @Column(name = "bank_country", length = 3)
    private String bankCountry;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private BatchStatus status;
    
    @Column(name = "total_exposures")
    private Integer totalExposures;
    
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "content_type", length = 100)
    private String contentType;
    
    @Column(name = "s3_uri", length = 500)
    private String s3Uri;
    
    @Column(name = "s3_bucket", length = 100)
    private String s3Bucket;
    
    @Column(name = "s3_key", length = 300)
    private String s3Key;
    
    @Column(name = "s3_version_id", length = 100)
    private String s3VersionId;
    
    @Column(name = "md5_checksum", length = 32)
    private String md5Checksum;
    
    @Column(name = "sha256_checksum", length = 64)
    private String sha256Checksum;
    
    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "processing_duration_ms")
    private Long processingDurationMs;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    // Default constructor for JPA
    protected IngestionBatchEntity() {}
    
    // Constructor for creating new entities
    public IngestionBatchEntity(String batchId, String bankId, BatchStatus status, Instant uploadedAt) {
        this.batchId = Objects.requireNonNull(batchId, "Batch ID cannot be null");
        this.bankId = Objects.requireNonNull(bankId, "Bank ID cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.uploadedAt = Objects.requireNonNull(uploadedAt, "Uploaded at cannot be null");
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Convert from domain aggregate to JPA entity.
     */
    public static IngestionBatchEntity fromDomain(IngestionBatch batch) {
        IngestionBatchEntity entity = new IngestionBatchEntity();
        entity.batchId = batch.getBatchId().value();
        entity.bankId = batch.getBankId().value();
        entity.status = batch.getStatus();
        entity.uploadedAt = batch.getUploadedAt();
        entity.createdAt = batch.getUploadedAt(); // Use uploadedAt as createdAt for new batches
        entity.updatedAt = Instant.now();
        
        // Map file metadata if present
        if (batch.getFileMetadata() != null) {
            FileMetadata fileMetadata = batch.getFileMetadata();
            entity.fileName = fileMetadata.fileName();
            entity.contentType = fileMetadata.contentType();
            entity.fileSizeBytes = fileMetadata.fileSizeBytes();
            entity.md5Checksum = fileMetadata.md5Checksum();
            entity.sha256Checksum = fileMetadata.sha256Checksum();
        }
        
        // Map S3 reference if present
        if (batch.getS3Reference() != null) {
            S3Reference s3Ref = batch.getS3Reference();
            entity.s3Uri = s3Ref.uri();
            entity.s3Bucket = s3Ref.bucket();
            entity.s3Key = s3Ref.key();
            entity.s3VersionId = s3Ref.versionId();
        }
        
        // Map bank info if present
        if (batch.getBankInfo() != null) {
            BankInfo bankInfo = batch.getBankInfo();
            entity.bankName = bankInfo.bankName();
            entity.bankCountry = bankInfo.bankCountry();
        }
        
        // Map other fields
        entity.totalExposures = batch.getTotalExposures();
        entity.completedAt = batch.getCompletedAt();
        entity.errorMessage = batch.getErrorMessage();
        entity.processingDurationMs = batch.getProcessingDurationMs();
        
        return entity;
    }
    
    /**
     * Convert from JPA entity to domain aggregate.
     */
    public IngestionBatch toDomain() {
        com.bcbs239.regtech.modules.ingestion.domain.batch.BatchId batchId = 
            new com.bcbs239.regtech.modules.ingestion.domain.batch.BatchId(this.batchId);
        BankId bankId = new BankId(this.bankId);
        
        // Create file metadata if data is present
        FileMetadata fileMetadata = null;
        if (fileName != null && contentType != null && fileSizeBytes != null) {
            fileMetadata = new FileMetadata(
                fileName,
                contentType,
                fileSizeBytes,
                md5Checksum,
                sha256Checksum
            );
        }
        
        // Create S3 reference if data is present
        S3Reference s3Reference = null;
        if (s3Uri != null && s3Bucket != null && s3Key != null && s3VersionId != null) {
            s3Reference = new S3Reference(s3Bucket, s3Key, s3VersionId, s3Uri);
        }
        
        // Create bank info if data is present
        BankInfo bankInfo = null;
        if (bankName != null && bankCountry != null) {
            // Use a default timestamp since we don't store it in the batch table
            bankInfo = new BankInfo(
                bankId,
                bankName,
                bankCountry,
                BankInfo.BankStatus.ACTIVE, // Default to ACTIVE, will be enriched from bank_info table
                createdAt
            );
        }
        
        return new IngestionBatch(
            batchId,
            bankId,
            status,
            fileMetadata,
            s3Reference,
            bankInfo,
            totalExposures,
            uploadedAt,
            completedAt,
            errorMessage,
            processingDurationMs,
            0, // recoveryAttempts - default to 0
            null, // lastCheckpoint
            null, // checkpointData
            updatedAt
        );
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IngestionBatchEntity that = (IngestionBatchEntity) o;
        return Objects.equals(batchId, that.batchId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(batchId);
    }
    
    @Override
    public String toString() {
        return "IngestionBatchEntity{" +
                "batchId='" + batchId + '\'' +
                ", bankId='" + bankId + '\'' +
                ", status=" + status +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}