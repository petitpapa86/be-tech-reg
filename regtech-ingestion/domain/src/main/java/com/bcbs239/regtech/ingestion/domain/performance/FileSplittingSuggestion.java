package com.bcbs239.regtech.ingestion.domain.performance;

/**
 * Domain model representing file splitting suggestions for performance optimization.
 */
public record FileSplittingSuggestion(
    String fileName,
    long fileSizeBytes,
    Integer estimatedExposureCount,
    boolean splittingRequired,
    boolean splittingRecommended,
    String severity,
    String reason,
    String recommendation,
    int estimatedOptimalFileCount
) {
    
    public FileSplittingSuggestion {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        if (fileSizeBytes < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        }
        if (estimatedExposureCount != null && estimatedExposureCount < 0) {
            throw new IllegalArgumentException("Estimated exposure count cannot be negative");
        }
        if (severity == null || severity.trim().isEmpty()) {
            throw new IllegalArgumentException("Severity cannot be null or empty");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be null or empty");
        }
        if (recommendation == null || recommendation.trim().isEmpty()) {
            throw new IllegalArgumentException("Recommendation cannot be null or empty");
        }
        if (estimatedOptimalFileCount < 1) {
            throw new IllegalArgumentException("Estimated optimal file count must be at least 1");
        }
    }
    
    /**
     * Check if any action is needed based on the suggestion.
     */
    public boolean requiresAction() {
        return splittingRequired || splittingRecommended;
    }
    
    /**
     * Get the file size in MB for display purposes.
     */
    public double getFileSizeMB() {
        return fileSizeBytes / (1024.0 * 1024.0);
    }
    
    /**
     * Check if this is a critical suggestion that must be addressed.
     */
    public boolean isCritical() {
        return "CRITICAL".equalsIgnoreCase(severity);
    }
    
    /**
     * Check if this is a warning-level suggestion.
     */
    public boolean isWarning() {
        return "WARNING".equalsIgnoreCase(severity);
    }
}