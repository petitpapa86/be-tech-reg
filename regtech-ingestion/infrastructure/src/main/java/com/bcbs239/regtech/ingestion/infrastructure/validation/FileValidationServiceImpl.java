package com.bcbs239.regtech.ingestion.infrastructure.validation;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.application.batch.process.ProcessBatchCommandHandler.FileValidationService;
import com.bcbs239.regtech.ingestion.application.batch.process.ProcessBatchCommandHandler.ValidationResult;
import com.bcbs239.regtech.ingestion.application.model.ParsedFileData;
import com.bcbs239.regtech.ingestion.domain.model.LoanExposure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of file validation service for processed batch data.
 * Validates structure and business rules of parsed file data.
 */
@Service
public class FileValidationServiceImpl implements FileValidationService {

    private static final Logger log = LoggerFactory.getLogger(FileValidationServiceImpl.class);

    @Override
    public Result<ValidationResult> validateStructure(ParsedFileData parsedData) {
        log.debug("Validating structure of parsed file data");

        if (parsedData == null) {
            return Result.failure(new ErrorDetail("NULL_DATA", "Parsed data cannot be null"));
        }

        // Validate exposures list
        List<LoanExposure> exposures = parsedData.exposures();
        if (exposures == null) {
            return Result.failure(new ErrorDetail("NULL_EXPOSURES", "Exposures list cannot be null"));
        }

        // Validate that we have at least one exposure
        if (exposures.isEmpty()) {
            return Result.failure(new ErrorDetail("EMPTY_EXPOSURES", "File must contain at least one loan exposure"));
        }

        // Validate each exposure has required fields
        for (int i = 0; i < exposures.size(); i++) {
            LoanExposure exposure = exposures.get(i);
            if (exposure == null) {
                return Result.failure(new ErrorDetail("NULL_EXPOSURE",
                    String.format("Exposure at index %d cannot be null", i)));
            }

            // Basic structural validation
            if (exposure.loanId() == null || exposure.loanId().trim().isEmpty()) {
                return Result.failure(new ErrorDetail("MISSING_LOAN_ID",
                    String.format("Loan ID is required for exposure at index %d", i)));
            }

            if (exposure.loanAmount() < 0) {
                return Result.failure(new ErrorDetail("INVALID_LOAN_AMOUNT",
                    String.format("Loan amount must be non-negative for exposure at index %d", i)));
            }

            if (exposure.grossExposureAmount() < 0) {
                return Result.failure(new ErrorDetail("INVALID_GROSS_EXPOSURE",
                    String.format("Gross exposure amount must be non-negative for exposure at index %d", i)));
            }
        }

        return Result.success(new ValidationResult(exposures.size()));
    }

    @Override
    public Result<ValidationResult> validateBusinessRules(ParsedFileData parsedData) {
        log.debug("Validating business rules of parsed file data");

        if (parsedData == null || parsedData.exposures() == null) {
            return Result.failure(new ErrorDetail("INVALID_DATA", "Parsed data or exposures cannot be null"));
        }

        List<LoanExposure> exposures = parsedData.exposures();

        // Business rule: Loan IDs should be unique within the file
        long uniqueLoanIds = exposures.stream()
            .map(LoanExposure::loanId)
            .distinct()
            .count();

        if (uniqueLoanIds != exposures.size()) {
            return Result.failure(new ErrorDetail("DUPLICATE_LOAN_IDS",
                "Loan IDs must be unique within the file"));
        }

        // Business rule: Total exposure amount should be reasonable (not zero or excessively high)
        double totalExposure = exposures.stream()
            .mapToDouble(LoanExposure::grossExposureAmount)
            .sum();

        if (totalExposure == 0) {
            return Result.failure(new ErrorDetail("ZERO_TOTAL_EXPOSURE",
                "Total gross exposure across all exposures cannot be zero"));
        }

        // Warning for very high total exposure (this might be configurable)
        if (totalExposure > 1_000_000_000) { // 1 billion
            log.warn("Very high total exposure amount detected: {}", totalExposure);
        }

        return Result.success(new ValidationResult(exposures.size()));
    }
}

