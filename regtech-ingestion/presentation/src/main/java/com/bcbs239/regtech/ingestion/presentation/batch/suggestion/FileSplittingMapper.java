package com.bcbs239.regtech.ingestion.presentation.batch.suggestion;

import com.bcbs239.regtech.ingestion.domain.performance.FileSplittingSuggestion;

public final class FileSplittingMapper {

    private FileSplittingMapper() {}

    public static FileSplittingSuggestionDto toDto(FileSplittingSuggestion domain) {
        if (domain == null) return null;
        return new FileSplittingSuggestionDto(
            domain.fileName(),
            domain.fileSizeBytes(),
            domain.estimatedExposureCount(),
            domain.splittingRequired(),
            domain.splittingRecommended(),
            domain.severity(),
            domain.reason(),
            domain.recommendation(),
            domain.estimatedOptimalFileCount()
        );
    }
}


