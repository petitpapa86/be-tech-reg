package com.bcbs239.regtech.ingestion.domain.batch.rules;


import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.specifications.Specification;
import com.bcbs239.regtech.ingestion.domain.batch.BatchStatus;
import com.bcbs239.regtech.ingestion.domain.batch.IngestionBatch;

import java.util.Objects;

public final class BatchSpecifications {

    private BatchSpecifications() {}

    public static Specification<IngestionBatch> mustNotBeTerminal() {
        return batch -> {
            Objects.requireNonNull(batch);
            if (batch.isTerminal()) {
                return Result.failure(ErrorDetail.of("BATCH_TERMINAL", ErrorType.BUSINESS_RULE_ERROR, "Batch is in a terminal state and cannot be modified", "batch.terminal"));
            }
            return Result.success(null);
        };
    }

    public static Specification<IngestionBatch> mustHaveS3Reference() {
        return batch -> {
            Objects.requireNonNull(batch);
            if (batch.getS3Reference() == null) {
                return Result.failure(ErrorDetail.of("MISSING_S3_REFERENCE", ErrorType.VALIDATION_ERROR, "S3 reference is required", "batch.missing.s3.reference"));
            }
            return Result.success(null);
        };
    }

    public static Specification<IngestionBatch> mustHaveExposureCount() {
        return batch -> {
            Objects.requireNonNull(batch);
            if (batch.getTotalExposures() == null || batch.getTotalExposures() < 0) {
                return Result.failure(ErrorDetail.of("MISSING_EXPOSURE_COUNT", ErrorType.VALIDATION_ERROR, "Exposure count is required and cannot be negative", "batch.missing.exposure.count"));
            }
            return Result.success(null);
        };
    }

    public static Specification<IngestionBatch> mustBeUploaded() {
        return batch -> {
            if (batch.getStatus() != BatchStatus.UPLOADED) {
                return Result.failure(ErrorDetail.of("INVALID_STATE", ErrorType.BUSINESS_RULE_ERROR, "Batch must be in UPLOADED status", "batch.invalid.state"));
            }
            return Result.success(null);
        };
    }

}

