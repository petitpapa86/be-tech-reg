package com.bcbs239.regtech.modules.ingestion.domain.batch.rules;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.modules.ingestion.domain.batch.BatchStatus;
import com.bcbs239.regtech.modules.ingestion.domain.batch.IngestionBatch;

public final class BatchTransitions {

    private BatchTransitions() {}

    public static Result<Void> validateTransition(IngestionBatch batch, BatchStatus target) {
        if (batch == null) return Result.failure(new ErrorDetail("INVALID_BATCH", "Batch cannot be null"));
        BatchStatus current = batch.getStatus();
        boolean ok = switch (current) {
            case UPLOADED -> target == BatchStatus.PARSING || target == BatchStatus.FAILED;
            case PARSING -> target == BatchStatus.VALIDATED || target == BatchStatus.FAILED;
            case VALIDATED -> target == BatchStatus.STORING || target == BatchStatus.FAILED;
            case STORING -> target == BatchStatus.COMPLETED || target == BatchStatus.FAILED;
            case COMPLETED, FAILED -> false;
        };

        if (!ok) {
            return Result.failure(new ErrorDetail("INVALID_STATE_TRANSITION", String.format("Cannot transition from %s to %s", current, target)));
        }
        return Result.success(null);
    }

    public static void applyTransition(IngestionBatch batch, BatchStatus target) {
        // apply transition without checking; callers should validate first
        batch.updateStatus(target);
    }
}

