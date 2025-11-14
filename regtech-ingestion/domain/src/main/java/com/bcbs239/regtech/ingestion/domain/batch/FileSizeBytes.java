package com.bcbs239.regtech.ingestion.domain.batch;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

/**
 * Value object representing file size in bytes.
 * Provides conversion methods and validation for file size operations.
 */
public record FileSizeBytes(long value) {
    
    private static final long KB = 1024L;
    private static final long MB = 1024L * 1024L;
    private static final long GB = 1024L * 1024L * 1024L;
    
    /**
     * Create a FileSizeBytes with validation.
     */
    public static Result<FileSizeBytes> create(long bytes) {
        if (bytes < 0) {
            return Result.failure(ErrorDetail.of(
                "INVALID_FILE_SIZE",
                ErrorType.VALIDATION_ERROR,
                "File size cannot be negative, got: " + bytes,
                "batch.filesize.invalid"
            ));
        }
        return Result.success(new FileSizeBytes(bytes));
    }
    
    /**
     * Create FileSizeBytes from megabytes.
     */
    public static FileSizeBytes fromMB(double megabytes) {
        return new FileSizeBytes((long) (megabytes * MB));
    }
    
    /**
     * Create FileSizeBytes from kilobytes.
     */
    public static FileSizeBytes fromKB(double kilobytes) {
        return new FileSizeBytes((long) (kilobytes * KB));
    }
    
    /**
     * Create a zero file size.
     */
    public static FileSizeBytes zero() {
        return new FileSizeBytes(0);
    }
    
    /**
     * Get size in kilobytes.
     */
    public double toKB() {
        return value / (double) KB;
    }
    
    /**
     * Get size in megabytes.
     */
    public double toMB() {
        return value / (double) MB;
    }
    
    /**
     * Get size in gigabytes.
     */
    public double toGB() {
        return value / (double) GB;
    }
    
    /**
     * Check if file size is zero.
     */
    public boolean isEmpty() {
        return value == 0;
    }
    
    /**
     * Check if file size exceeds a threshold.
     */
    public boolean exceeds(FileSizeBytes threshold) {
        return value > threshold.value;
    }
    
    /**
     * Check if file size is within a maximum limit.
     */
    public boolean isWithinLimit(FileSizeBytes maxSize) {
        return value <= maxSize.value;
    }
    
    /**
     * Add another file size.
     */
    public FileSizeBytes plus(FileSizeBytes other) {
        return new FileSizeBytes(this.value + other.value);
    }
    
    /**
     * Calculate size multiplier relative to a base size.
     * Useful for estimating processing time based on file size.
     */
    public double multiplierRelativeTo(FileSizeBytes baseSize) {
        if (baseSize.value == 0) {
            return 1.0;
        }
        return (double) this.value / baseSize.value;
    }
    
    /**
     * Get a human-readable representation.
     */
    public String toHumanReadable() {
        if (value < KB) {
            return value + " bytes";
        } else if (value < MB) {
            return String.format("%.2f KB", toKB());
        } else if (value < GB) {
            return String.format("%.2f MB", toMB());
        } else {
            return String.format("%.2f GB", toGB());
        }
    }
    
    @Override
    public String toString() {
        return toHumanReadable();
    }
}
