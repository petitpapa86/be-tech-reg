package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

import java.time.Instant;

/**
 * XBRL Report Metadata value object
 * Contains metadata about a generated XBRL report including storage location,
 * size, access URL, validation status, and generation timestamp
 */
public record XbrlReportMetadata(
    @NonNull S3Uri s3Uri,
    @NonNull FileSize fileSize,
    @NonNull PresignedUrl presignedUrl,
    @NonNull XbrlValidationStatus validationStatus,
    @NonNull Instant generatedAt
) {
    
    /**
     * Compact constructor with validation
     */
    public XbrlReportMetadata {
        if (s3Uri == null) {
            throw new IllegalArgumentException("S3 URI cannot be null");
        }
        if (fileSize == null) {
            throw new IllegalArgumentException("File size cannot be null");
        }
        if (presignedUrl == null) {
            throw new IllegalArgumentException("Presigned URL cannot be null");
        }
        if (validationStatus == null) {
            throw new IllegalArgumentException("Validation status cannot be null");
        }
        if (generatedAt == null) {
            throw new IllegalArgumentException("Generation timestamp cannot be null");
        }
        if (generatedAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Generation timestamp cannot be in the future");
        }
    }
    
    /**
     * Create XBRL report metadata with current timestamp
     */
    public static XbrlReportMetadata create(
        S3Uri s3Uri,
        FileSize fileSize,
        PresignedUrl presignedUrl,
        XbrlValidationStatus validationStatus
    ) {
        return new XbrlReportMetadata(s3Uri, fileSize, presignedUrl, validationStatus, Instant.now());
    }
    
    /**
     * Check if the presigned URL is still valid
     */
    public boolean hasValidUrl() {
        return presignedUrl.isValid();
    }
    
    /**
     * Check if the XBRL report passed validation
     */
    public boolean isValid() {
        return validationStatus == XbrlValidationStatus.VALID;
    }
    
    /**
     * Check if the XBRL report has validation errors
     */
    public boolean hasValidationErrors() {
        return validationStatus == XbrlValidationStatus.INVALID;
    }
    
    /**
     * Get the age of the report in seconds
     */
    public long getAgeInSeconds() {
        return Instant.now().getEpochSecond() - generatedAt.getEpochSecond();
    }
}
