package com.bcbs239.regtech.ingestion.domain.batch;


import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;


public final class BatchTransitions {

    private BatchTransitions() {}

    public static Result<Void> validateTransition(IngestionBatch batch, BatchStatus target) {
        long start = System.currentTimeMillis();
        String from = batch == null ? "<null>" : String.valueOf(batch.getStatus());
        String to = String.valueOf(target);

        if (batch == null) {
            return Result.failure(ErrorDetail.of("INVALID_BATCH", ErrorType.VALIDATION_ERROR, "Batch cannot be null", "batch.invalid"));
        }

        BatchStatus current = batch.getStatus();
        boolean ok = switch (current) {
            case UPLOADED -> target == BatchStatus.PARSING || target == BatchStatus.FAILED;
            case PARSING -> target == BatchStatus.VALIDATED || target == BatchStatus.FAILED;
            case VALIDATED -> target == BatchStatus.STORING || target == BatchStatus.FAILED;
            case STORING -> target == BatchStatus.COMPLETED || target == BatchStatus.FAILED;
            case COMPLETED, FAILED -> false;
        };

        if (!ok) {
            return Result.failure(ErrorDetail.of("INVALID_STATE_TRANSITION", ErrorType.BUSINESS_RULE_ERROR, String.format("Cannot transition from %s to %s", current, target), "batch.invalid.transition"));
        }

        return Result.success(null);
    }

    public static void applyTransition(IngestionBatch batch, BatchStatus target) {
        // apply transition without checking; callers should validate first
        batch.updateStatus(target);
    }
}

