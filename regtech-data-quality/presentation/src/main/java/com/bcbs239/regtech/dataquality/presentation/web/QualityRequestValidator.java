package com.bcbs239.regtech.dataquality.presentation.web;


import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.domain.report.QualityStatus;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for quality report requests.
 * Handles validation of batch IDs, query parameters, and business rules.
 */
@Component
public class QualityRequestValidator {
    
    /**
     * Validates batch ID parameter.
     */
    public Result<BatchId> validateBatchId(String batchIdStr) {
        List<FieldError> fieldErrors = new ArrayList<>();
        
        if (batchIdStr == null || batchIdStr.trim().isEmpty()) {
            fieldErrors.add(new FieldError("batchId", "REQUIRED", "Batch ID is required"));
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }
        
        try {
            BatchId batchId = BatchId.of(batchIdStr.trim());
            return Result.success(batchId);
        } catch (IllegalArgumentException e) {
            fieldErrors.add(new FieldError("batchId", "INVALID_FORMAT", 
                "Invalid batch ID format: " + e.getMessage()));
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }
    }

    /**
     * Validates bank ID query parameter.
     */
    public Result<BankId> validateBankId(String bankIdStr) {
        List<FieldError> fieldErrors = new ArrayList<>();

        if (bankIdStr == null || bankIdStr.trim().isEmpty()) {
            fieldErrors.add(new FieldError("bankId", "REQUIRED", "Bank ID is required"));
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }

        try {
            BankId bankId = BankId.of(bankIdStr.trim());
            return Result.success(bankId);
        } catch (IllegalArgumentException e) {
            fieldErrors.add(new FieldError("bankId", "INVALID_FORMAT",
                "Invalid bank ID format: " + e.getMessage()));
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }
    }

    /**
     * Validates QualityStatus query parameter.
     * If the parameter is missing/blank, returns the provided default.
     */
    public Result<QualityStatus> validateQualityStatus(String statusStr, QualityStatus defaultStatus) {
        List<FieldError> fieldErrors = new ArrayList<>();

        if (statusStr == null || statusStr.trim().isEmpty()) {
            return Result.success(defaultStatus);
        }

        try {
            QualityStatus status = QualityStatus.valueOf(statusStr.trim().toUpperCase());
            return Result.success(status);
        } catch (IllegalArgumentException e) {
            fieldErrors.add(new FieldError("status", "INVALID_VALUE",
                "Invalid status. Allowed: PENDING, IN_PROGRESS, COMPLETED, FAILED"));
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }
    }
    
    /**
     * Parses and validates query parameters for trends endpoint.
     */
    public Result<TrendsQueryParams> parseTrendsQueryParams(ServerRequest request) {
        List<FieldError> fieldErrors = new ArrayList<>();
        
        try {
            // Parse days parameter (default: 30)
            int days = request.param("days")
                .map(Integer::parseInt)
                .orElse(30);
            
            if (days <= 0) {
                fieldErrors.add(new FieldError("days", "INVALID_VALUE", 
                    "Days must be positive"));
            }
            if (days > 365) {
                fieldErrors.add(new FieldError("days", "INVALID_VALUE", 
                    "Days cannot exceed 365"));
            }
            
            // Parse limit parameter (default: 100)
            int limit = request.param("limit")
                .map(Integer::parseInt)
                .orElse(100);
            
            if (limit <= 0) {
                fieldErrors.add(new FieldError("limit", "INVALID_VALUE", 
                    "Limit must be positive"));
            }
            if (limit > 1000) {
                fieldErrors.add(new FieldError("limit", "INVALID_VALUE", 
                    "Limit cannot exceed 1000"));
            }
            
            if (!fieldErrors.isEmpty()) {
                return Result.failure(ErrorDetail.validationError(fieldErrors));
            }
            
            // Calculate time range
            Instant endTime = Instant.now();
            Instant startTime = endTime.minus(days, ChronoUnit.DAYS);
            
            return Result.success(new TrendsQueryParams(startTime, endTime, limit));
            
        } catch (NumberFormatException e) {
            fieldErrors.add(new FieldError("queryParams", "INVALID_FORMAT", 
                "Invalid number format in query parameters"));
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of(
                "SYSTEM_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to parse query parameters: " + e.getMessage(),
                "system"
            ));
        }
    }
    
    /**
     * Record for trends query parameters.
     */
    public record TrendsQueryParams(
        Instant startTime,
        Instant endTime,
        int limit
    ) {}
}

