package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/**
 * Command to initiate quality validation for a batch of exposures.
 * Contains all necessary metadata to process the batch validation workflow.
 */
public record ValidateBatchQualityCommand(
    BatchId batchId,
    BankId bankId,
    String s3Uri,
    int expectedExposureCount,
    @Nullable LocalDate uploadDate,
    String correlationId
) {
    
    /**
     * Creates a command with required parameters.
     */
    public static ValidateBatchQualityCommand of(
        BatchId batchId,
        BankId bankId,
        String s3Uri,
        int expectedExposureCount
    ) {
        return new ValidateBatchQualityCommand(
            batchId,
            bankId,
            s3Uri,
            expectedExposureCount,
            null,
            null
        );
    }
    
    /**
     * Creates a command with correlation ID for tracing.
     */
    public static ValidateBatchQualityCommand withCorrelation(
        BatchId batchId,
        BankId bankId,
        String s3Uri,
        int expectedExposureCount,
        String correlationId
    ) {
        return new ValidateBatchQualityCommand(
            batchId,
            bankId,
            s3Uri,
            expectedExposureCount,
            null,
            correlationId
        );
    }
    
    /**
     * Creates a command with upload date and correlation ID.
     */
    public static ValidateBatchQualityCommand withUploadDate(
        BatchId batchId,
        BankId bankId,
        String s3Uri,
        int expectedExposureCount,
        LocalDate uploadDate,
        String correlationId
    ) {
        return new ValidateBatchQualityCommand(
            batchId,
            bankId,
            s3Uri,
            expectedExposureCount,
            uploadDate,
            correlationId
        );
    }
    
    /**
     * Validates the command parameters.
     */
    public void validate() {
        if (batchId == null) {
            throw new IllegalArgumentException("Batch ID cannot be null");
        }
        if (bankId == null) {
            throw new IllegalArgumentException("Bank ID cannot be null");
        }
        if (s3Uri == null || s3Uri.trim().isEmpty()) {
            throw new IllegalArgumentException("S3 URI cannot be null or empty");
        }
        if (expectedExposureCount < 0) {
            throw new IllegalArgumentException("Expected exposure count cannot be negative");
        }
    }
    
    /**
     * Gets the S3 bucket name from the URI.
     */
    public String getS3Bucket() {
        if (s3Uri == null || !s3Uri.startsWith("s3://")) {
            throw new IllegalArgumentException("Invalid S3 URI format: " + s3Uri);
        }
        
        String withoutProtocol = s3Uri.substring(5); // Remove "s3://"
        int slashIndex = withoutProtocol.indexOf('/');
        
        if (slashIndex == -1) {
            return withoutProtocol;
        }
        
        return withoutProtocol.substring(0, slashIndex);
    }
    
    /**
     * Gets the S3 key from the URI.
     */
    public String getS3Key() {
        if (s3Uri == null || !s3Uri.startsWith("s3://")) {
            throw new IllegalArgumentException("Invalid S3 URI format: " + s3Uri);
        }
        
        String withoutProtocol = s3Uri.substring(5); // Remove "s3://"
        int slashIndex = withoutProtocol.indexOf('/');
        
        if (slashIndex == -1) {
            throw new IllegalArgumentException("S3 URI must contain a key: " + s3Uri);
        }
        
        return withoutProtocol.substring(slashIndex + 1);
    }
    
    /**
     * Creates a copy with a new correlation ID.
     */
    public ValidateBatchQualityCommand withCorrelationId(String newCorrelationId) {
        return new ValidateBatchQualityCommand(
            batchId,
            bankId,
            s3Uri,
            expectedExposureCount,
            uploadDate,
            newCorrelationId
        );
    }
}

