package com.bcbs239.regtech.modules.ingestion.domain.batch;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.TransitionMetricsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BatchTransitions {

    private static final Logger log = LoggerFactory.getLogger(BatchTransitions.class);

    private BatchTransitions() {}

    public static Result<Void> validateTransition(IngestionBatch batch, BatchStatus target) {
        long start = System.currentTimeMillis();
        String from = batch == null ? "<null>" : String.valueOf(batch.getStatus());
        String to = String.valueOf(target);

        // emit requested
        if (TransitionMetricsHolder.get() != null) {
            TransitionMetricsHolder.get().onRequested(from, to);
        }

        if (batch == null) {
            long dur = System.currentTimeMillis() - start;
            if (TransitionMetricsHolder.get() != null) TransitionMetricsHolder.get().onFailure(from, to, dur);
            return Result.failure(new ErrorDetail("INVALID_BATCH", "Batch cannot be null"));
        }

        BatchStatus current = batch.getStatus();
        boolean ok = switch (current) {
            case UPLOADED -> target == BatchStatus.PARSING || target == BatchStatus.FAILED;
            case PARSING -> target == BatchStatus.VALIDATED || target == BatchStatus.FAILED;
            case VALIDATED -> target == BatchStatus.STORING || target == BatchStatus.FAILED;
            case STORING -> target == BatchStatus.COMPLETED || target == BatchStatus.FAILED;
            case COMPLETED, FAILED -> false;
        };

        long duration = System.currentTimeMillis() - start;
        if (!ok) {
            if (TransitionMetricsHolder.get() != null) TransitionMetricsHolder.get().onFailure(from, to, duration);
            log.info("Transition validation failed from {} to {}", from, to);
            return Result.failure(new ErrorDetail("INVALID_STATE_TRANSITION", String.format("Cannot transition from %s to %s", current, target)));
        }

        if (TransitionMetricsHolder.get() != null) TransitionMetricsHolder.get().onSuccess(from, to, duration);
        log.debug("Transition validated from {} to {} in {}ms", from, to, duration);
        return Result.success(null);
    }

    public static void applyTransition(IngestionBatch batch, BatchStatus target) {
        long start = System.currentTimeMillis();
        String from = String.valueOf(batch.getStatus());
        String to = String.valueOf(target);
        try {
            // apply transition without checking; callers should validate first
            batch.updateStatus(target);
            long dur = System.currentTimeMillis() - start;
            if (TransitionMetricsHolder.get() != null) TransitionMetricsHolder.get().onSuccess(from, to, dur);
            log.info("Applied transition from {} to {} in {}ms", from, to, dur);
        } catch (Exception e) {
            long dur = System.currentTimeMillis() - start;
            if (TransitionMetricsHolder.get() != null) TransitionMetricsHolder.get().onFailure(from, to, dur);
            log.error("Failed to apply transition from {} to {}: {}", from, to, e.getMessage(), e);
            throw e;
        }
    }
}
