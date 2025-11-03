package com.bcbs239.regtech.ingestion.application.files;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.ingestion.domain.performance.FileSplittingSuggestion;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SuggestFileSplittingCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(SuggestFileSplittingCommandHandler.class);

    private final FileSplittingService fileSplittingService;

    public SuggestFileSplittingCommandHandler(
            FileSplittingService fileSplittingService) {
        this.fileSplittingService = fileSplittingService;
    }

    public Result<FileSplittingSuggestion> handle(SuggestFileSplittingCommand command) {
        try {
            if (command == null || command.fileMetadata() == null) {
                return Result.failure(new ErrorDetail("INVALID_COMMAND", "Command or metadata cannot be null"));
            }

            FileMetadata metadata = command.fileMetadata();

            // Delegate to splitting service (no security checks here; handled centrally)
            FileSplittingSuggestion suggestion = fileSplittingService.suggestSplitting(metadata);

            log.debug("Generated splitting suggestion for {}: required={}, recommended={}", metadata.fileName(), suggestion.splittingRequired(), suggestion.splittingRecommended());

            return Result.success(suggestion);
        } catch (Exception e) {
            return Result.failure(new ErrorDetail("SUGGESTION_ERROR", e.getMessage()));
        }
    }
}
