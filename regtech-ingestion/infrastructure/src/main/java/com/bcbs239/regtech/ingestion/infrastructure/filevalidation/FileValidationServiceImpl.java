package com.bcbs239.regtech.ingestion.infrastructure.filevalidation;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.domain.model.LoanExposure;
import com.bcbs239.regtech.ingestion.domain.model.ParsedFileData;
import com.bcbs239.regtech.ingestion.domain.validation.FileContentValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of file content validation service for processed batch data.
 * Validates structure and business rules of parsed file data.
 */
@Service
public class FileValidationServiceImpl implements FileContentValidationService {

    private static final Logger log = LoggerFactory.getLogger(FileValidationServiceImpl.class);

    @Override
    public <T> Result<ValidationResult> validateStructure(T parsedData) {
        log.debug("Validating structure of parsed file data");

        if (!(parsedData instanceof ParsedFileData data)) {
            return Result.failure(ErrorDetail.of("INVALID_TYPE", ErrorType.SYSTEM_ERROR, "Expected ParsedFileData", "generic.error"));
        }

        // Validate exposures list
        List<LoanExposure> exposures = data.exposures();
        if (exposures == null) {
            return Result.failure(ErrorDetail.of("NULL_EXPOSURES", ErrorType.SYSTEM_ERROR, "Exposures list cannot be null", "generic.error"));
        }

        // Validate that we have at least one exposure
        if (exposures.isEmpty()) {
            return Result.failure(ErrorDetail.of("EMPTY_EXPOSURES", ErrorType.SYSTEM_ERROR, "File must contain at least one loan exposure", "generic.error"));
        }

        // Validate each exposure has required fields
        for (int i = 0; i < exposures.size(); i++) {
            LoanExposure exposure = exposures.get(i);
            if (exposure == null) {
                return Result.failure(ErrorDetail.of("NULL_EXPOSURE", ErrorType.VALIDATION_ERROR,
                    String.format("Exposure at index %d cannot be null", i), "validation.null.exposure"));
            }

            // Basic structural validation
            if (exposure.loanId() == null || exposure.loanId().trim().isEmpty()) {
                return Result.failure(ErrorDetail.of("MISSING_LOAN_ID", ErrorType.VALIDATION_ERROR,
                    String.format("Loan ID is required for exposure at index %d", i), "validation.missing.loan.id"));
            }

            if (exposure.loanAmount() < 0) {
                return Result.failure(ErrorDetail.of("INVALID_LOAN_AMOUNT", ErrorType.VALIDATION_ERROR,
                    String.format("Loan amount must be non-negative for exposure at index %d", i), "validation.invalid.loan.amount"));
            }

            if (exposure.grossExposureAmount() < 0) {
                return Result.failure(ErrorDetail.of("INVALID_GROSS_EXPOSURE", ErrorType.VALIDATION_ERROR,
                    String.format("Gross exposure amount must be non-negative for exposure at index %d", i), "validation.invalid.gross.exposure"));
            }
        }

        return Result.success(new ValidationResult(exposures.size()));
    }

    @Override
    public <T> Result<ValidationResult> validateBusinessRules(T parsedData) {
        log.debug("Validating business rules of parsed file data");

        if (!(parsedData instanceof ParsedFileData data)) {
            return Result.failure(ErrorDetail.of("INVALID_TYPE", ErrorType.SYSTEM_ERROR, "Expected ParsedFileData", "generic.error"));
        }

        if (data == null || data.exposures() == null) {
            return Result.failure(ErrorDetail.of("INVALID_DATA", ErrorType.SYSTEM_ERROR, "Parsed data or exposures cannot be null", "generic.error"));
        }

        List<LoanExposure> exposures = data.exposures();

        // Business rule: Loan IDs should be unique within the file
        long uniqueLoanIds = exposures.stream()
            .map(LoanExposure::loanId)
            .distinct()
            .count();

        if (uniqueLoanIds != exposures.size()) {
            return Result.failure(ErrorDetail.of("DUPLICATE_LOAN_IDS", ErrorType.SYSTEM_ERROR, "Loan IDs must be unique within the file", "generic.error"));
        }

        // Business rule: Total exposure amount should be reasonable (not zero or excessively high)
        double totalExposure = exposures.stream()
            .mapToDouble(LoanExposure::grossExposureAmount)
            .sum();

        if (totalExposure <= 0) {
            return Result.failure(ErrorDetail.of("INVALID_TOTAL_EXPOSURE", ErrorType.SYSTEM_ERROR, "Total exposure amount must be greater than zero", "generic.error"));
        }

        log.info("Business rules validation passed for {} exposures with total exposure: {}", exposures.size(), totalExposure);
        return Result.success(new ValidationResult(exposures.size()));
    }
}
