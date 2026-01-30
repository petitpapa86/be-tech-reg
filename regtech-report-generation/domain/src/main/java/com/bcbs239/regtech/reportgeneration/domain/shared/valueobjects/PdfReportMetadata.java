package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

import java.time.Instant;

/**
 * PDF Report Metadata value object
 * Contains metadata about a generated PDF report including storage location,
 * size, access URL, and generation timestamp
 */
public record PdfReportMetadata(
    @NonNull S3Uri s3Uri,
    @NonNull FileSize fileSize,
    @NonNull PresignedUrl presignedUrl,
    @NonNull Instant generatedAt
) {
    
    /**
     * Compact constructor with validation
     */
    public PdfReportMetadata {
        if (s3Uri == null) {
            throw new IllegalArgumentException("S3 URI cannot be null");
        }
        if (fileSize == null) {
            throw new IllegalArgumentException("File size cannot be null");
        }
        if (presignedUrl == null) {
            throw new IllegalArgumentException("Presigned URL cannot be null");
        }
        if (generatedAt == null) {
            throw new IllegalArgumentException("Generation timestamp cannot be null");
        }
        if (generatedAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Generation timestamp cannot be in the future");
        }
    }
    
    /**
     * Create PDF report metadata with current timestamp
     */
    public static PdfReportMetadata create(
        S3Uri s3Uri,
        FileSize fileSize,
        PresignedUrl presignedUrl
    ) {
        return new PdfReportMetadata(s3Uri, fileSize, presignedUrl, Instant.now());
    }
    
    /**
     * Check if the presigned URL is still valid
     */
    public boolean hasValidUrl() {
        return presignedUrl.isValid();
    }
    
    /**
     * Get the age of the report in seconds
     */
    public long getAgeInSeconds() {
        return Instant.now().getEpochSecond() - generatedAt.getEpochSecond();
    }
}
