package com.bcbs239.regtech.modules.ingestion.application.files;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.modules.ingestion.domain.performance.FileSplittingSuggestion;
import com.bcbs239.regtech.modules.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.modules.ingestion.application.batch.IngestionSecurityService;
import com.bcbs239.regtech.modules.ingestion.application.batch.IngestionLoggingService;
import com.bcbs239.regtech.modules.ingestion.application.files.FileSplittingService;
import com.bcbs239.regtech.modules.ingestion.domain.bankinfo.BankId;
import org.springframework.stereotype.Component;

@Component
public class SuggestFileSplittingCommandHandler {

    private final FileSplittingService fileSplittingService;
    private final IngestionSecurityService securityService;
    private final IngestionLoggingService loggingService;

    public SuggestFileSplittingCommandHandler(
            FileSplittingService fileSplittingService,
            IngestionSecurityService securityService,
            IngestionLoggingService loggingService) {
        this.fileSplittingService = fileSplittingService;
        this.securityService = securityService;
        this.loggingService = loggingService;
    }

    public Result<FileSplittingSuggestion> handle(SuggestFileSplittingCommand command) {
        try {
            if (command == null || command.fileMetadata() == null) {
                return Result.failure(new ErrorDetail("INVALID_COMMAND", "Command or metadata cannot be null"));
            }

            FileMetadata metadata = command.fileMetadata();
            String token = command.authToken();

            // If token present, validate and extract bank id
            BankId bankId = null;
            if (token != null && !token.trim().isEmpty()) {
                Result<BankId> bankIdResult = securityService.validateTokenAndExtractBankId(token);
                if (bankIdResult.isFailure()) {
                    return Result.failure(bankIdResult.getError().orElseThrow());
                }
                bankId = bankIdResult.getValue().orElseThrow();

                // Verify ingestion suggestion permission
                Result<Void> perm = securityService.verifyIngestionPermissions("suggest-splitting");
                if (perm.isFailure()) {
                    return Result.failure(perm.getError().orElseThrow());
                }
            }

            // Delegate to splitting service
            FileSplittingSuggestion suggestion = fileSplittingService.suggestSplitting(metadata);

            // Optionally log
            if (bankId != null) {
                loggingService.logRequestFlowStep("SPLITTING_SUGGESTION", "SUGGESTION_GENERATED",
                    java.util.Map.of("fileName", metadata.fileName(), "bankId", bankId.value()));
            }

            return Result.success(suggestion);
        } catch (Exception e) {
            return Result.failure(new ErrorDetail("SUGGESTION_ERROR", e.getMessage()));
        }
    }
}
