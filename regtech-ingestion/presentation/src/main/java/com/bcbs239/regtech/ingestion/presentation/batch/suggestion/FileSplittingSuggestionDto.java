package com.bcbs239.regtech.ingestion.presentation.batch.suggestion;

/**
 * DTO di risposta per suggerimenti di split dei file (presentation layer)
 */
public record FileSplittingSuggestionDto(
    String fileName,
    long fileSizeBytes,
    Integer estimatedExposureCount,
    boolean splittingRequired,
    boolean splittingRecommended,
    String severity,
    String reason,
    String recommendation,
    int estimatedOptimalFileCount
) {}


