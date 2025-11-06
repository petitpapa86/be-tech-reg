package com.bcbs239.regtech.ingestion.domain.batch.rules;


import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
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
                return Result.failure(ErrorDetail.of("BATCH_TERMINAL", "Batch is in a terminal state and cannot be modified","" +
                        "batch.terminal"));
            }
            return Result.success(null);
        };
    }

    public static Specification<IngestionBatch> mustHaveS3Reference() {
        return batch -> {
            Objects.requireNonNull(batch);
            if (batch.getS3Reference() == null) {
                return Result.failure(new ErrorDetail("MISSING_S3_REFERENCE", "S3 reference is required"));
            }
            return Result.success(null);
        };
    }

    public static Specification<IngestionBatch> mustHaveExposureCount() {
        return batch -> {
            Objects.requireNonNull(batch);
            if (batch.getTotalExposures() == null || batch.getTotalExposures() < 0) {
                return Result.failure(new ErrorDetail("MISSING_EXPOSURE_COUNT", "Exposure count is required and cannot be negative"));
            }
            return Result.success(null);
        };
    }

    public static Specification<IngestionBatch> mustBeUploaded() {
        return batch -> {
            if (batch.getStatus() != BatchStatus.UPLOADED) {
                return Result.failure(new ErrorDetail("INVALID_STATE", "Batch must be in UPLOADED status"));
            }
            return Result.success(null);
        };
    }

}

