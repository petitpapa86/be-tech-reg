package com.bcbs239.regtech.ingestion.infrastructure.files;

import com.bcbs239.regtech.ingestion.application.files.FileSplittingService;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.performance.FileSplittingSuggestion;
import com.bcbs239.regtech.ingestion.infrastructure.performance.FileProcessingPerformanceOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FileSplittingServiceImpl implements FileSplittingService {

    private static final Logger log = LoggerFactory.getLogger(FileSplittingServiceImpl.class);

    private final FileProcessingPerformanceOptimizer optimizer;

    public FileSplittingServiceImpl(FileProcessingPerformanceOptimizer optimizer) {
        this.optimizer = optimizer;
    }

    @Override
    public FileSplittingSuggestion suggestSplitting(FileMetadata metadata) {
        log.debug("[infra] Generating file splitting suggestion for {} ({} bytes)", metadata.fileName(), metadata.fileSizeBytes());
        FileSplittingSuggestion suggestion = optimizer.suggestSplitting(metadata);
        log.debug("[infra] Suggestion: required={}, recommended={}, optimalFiles={}", suggestion.splittingRequired(), suggestion.splittingRecommended(), suggestion.estimatedOptimalFileCount());
        return suggestion;
    }
}


