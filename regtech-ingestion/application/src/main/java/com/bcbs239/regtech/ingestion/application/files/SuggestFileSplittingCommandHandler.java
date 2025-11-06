package com.bcbs239.regtech.ingestion.application.files;


import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.performance.FileSplittingSuggestion;
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
                return Result.failure(ErrorDetail.of("INVALID_COMMAND", ErrorType.VALIDATION_ERROR, "Command or metadata cannot be null", "file.splitting.invalid.command"));
            }

            FileMetadata metadata = command.fileMetadata();

            // Delegate to splitting service (no security checks here; handled centrally)
            FileSplittingSuggestion suggestion = fileSplittingService.suggestSplitting(metadata);

            log.debug("Generated splitting suggestion for {}: required={}, recommended={}", metadata.fileName(), suggestion.splittingRequired(), suggestion.splittingRecommended());

            return Result.success(suggestion);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("SUGGESTION_ERROR", ErrorType.SYSTEM_ERROR, e.getMessage(), "file.splitting.suggestion.error"));
        }
    }
}

