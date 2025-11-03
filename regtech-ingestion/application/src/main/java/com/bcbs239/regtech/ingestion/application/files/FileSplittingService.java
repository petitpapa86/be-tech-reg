package com.bcbs239.regtech.modules.ingestion.application.files;

import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.performance.FileSplittingSuggestion;

public interface FileSplittingService {
    FileSplittingSuggestion suggestSplitting(FileMetadata metadata);
}

