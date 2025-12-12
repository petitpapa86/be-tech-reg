package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.Value;

/**
 * Command to trigger risk metrics calculation for a batch.
 */
@Getter
public class CalculateRiskMetricsCommand {
    String batchId;
    String bankId;
    String s3Uri;
    int totalExposures;
    String correlationId;

    private CalculateRiskMetricsCommand(String batchId, String bankId, String s3Uri, 
                                       int totalExposures, String correlationId) {
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3Uri = s3Uri;
        this.totalExposures = totalExposures;
        this.correlationId = correlationId;
    }

    public static Result<CalculateRiskMetricsCommand> create(String batchId, String bankId, 
                                                             String s3Uri, int totalExposures, 
                                                             String correlationId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_BATCH_ID", ErrorType.VALIDATION_ERROR, 
                "Batch ID cannot be null or empty", "command.validation.batch.id"));
        }
        if (bankId == null || bankId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_BANK_ID", ErrorType.VALIDATION_ERROR, 
                "Bank ID cannot be null or empty", "command.validation.bank.id"));
        }
        if (s3Uri == null || s3Uri.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_S3_URI", ErrorType.VALIDATION_ERROR, 
                "S3 URI cannot be null or empty", "command.validation.s3.uri"));
        }
        if (totalExposures <= 0) {
            return Result.failure(ErrorDetail.of("INVALID_TOTAL_EXPOSURES", ErrorType.VALIDATION_ERROR, 
                "Total exposures must be greater than zero", "command.validation.total.exposures"));
        }

        return Result.success(new CalculateRiskMetricsCommand(batchId, bankId, s3Uri, 
                                                              totalExposures, correlationId));
    }
}
