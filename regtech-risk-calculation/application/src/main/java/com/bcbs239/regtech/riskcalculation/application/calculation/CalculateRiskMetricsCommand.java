package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.TotalExposures;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Command to calculate risk metrics for a batch of exposures.
 * Contains all necessary information to process risk calculations including
 * batch identification, source file location, and optional correlation tracking.
 */
public record CalculateRiskMetricsCommand(
    @NotNull(message = "Batch ID is required")
    BatchId batchId,
    
    @NotNull(message = "Bank ID is required")
    BankId bankId,
    
    @NotNull(message = "Source file URI is required")
    FileStorageUri sourceFileUri,
    
    @NotNull(message = "Expected exposures count is required")
    TotalExposures expectedExposures,
    
    @NotNull(message = "Correlation ID cannot be null")
    Maybe<String> correlationId
) {
    
    /**
     * Factory method to create and validate CalculateRiskMetricsCommand
     * 
     * @param batchId The batch identifier
     * @param bankId The bank identifier
     * @param sourceFileUri The URI of the source file containing exposures
     * @param expectedExposures The expected number of exposures in the batch
     * @param correlationId Optional correlation ID for tracking
     * @return Result containing the validated command or error details
     */
    public static Result<CalculateRiskMetricsCommand> create(
        String batchId,
        String bankId,
        String sourceFileUri,
        int expectedExposures,
        String correlationId
    ) {
        // Validate batch ID
        if (batchId == null || batchId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "BATCH_ID_REQUIRED", 
                ErrorType.BUSINESS_RULE_ERROR,
                "Batch ID is required and cannot be empty", 
                "validation.batch.id.required"
            ));
        }
        
        // Validate bank ID
        if (bankId == null || bankId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "BANK_ID_REQUIRED", 
                ErrorType.BUSINESS_RULE_ERROR,
                "Bank ID is required and cannot be empty", 
                "validation.bank.id.required"
            ));
        }
        
        // Validate source file URI
        if (sourceFileUri == null || sourceFileUri.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "SOURCE_FILE_URI_REQUIRED", 
                ErrorType.BUSINESS_RULE_ERROR,
                "Source file URI is required and cannot be empty", 
                "validation.source.file.uri.required"
            ));
        }
        
        // Validate expected exposures count
        if (expectedExposures <= 0) {
            return Result.failure(ErrorDetail.of(
                "EXPECTED_EXPOSURES_INVALID", 
                ErrorType.BUSINESS_RULE_ERROR,
                "Expected exposures count must be positive", 
                "validation.expected.exposures.positive"
            ));
        }
        
        try {
            BatchId validatedBatchId = BatchId.of(batchId.trim());
            BankId validatedBankId = BankId.of(bankId.trim());
            FileStorageUri validatedSourceFileUri = new FileStorageUri(sourceFileUri.trim());
            TotalExposures validatedExpectedExposures = new TotalExposures(expectedExposures);
            Maybe<String> validatedCorrelationId = (correlationId == null || correlationId.trim().isEmpty()) 
                ? Maybe.none() 
                : Maybe.some(correlationId.trim());
            
            return Result.success(new CalculateRiskMetricsCommand(
                validatedBatchId,
                validatedBankId,
                validatedSourceFileUri,
                validatedExpectedExposures,
                validatedCorrelationId
            ));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of(
                "VALIDATION_ERROR", 
                ErrorType.BUSINESS_RULE_ERROR,
                "Command validation failed: " + e.getMessage(), 
                "validation.command.failed"
            ));
        }
    }
}