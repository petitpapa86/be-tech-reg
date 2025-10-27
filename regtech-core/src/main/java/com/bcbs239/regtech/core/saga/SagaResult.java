package com.bcbs239.regtech.core.saga;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;

/**
 * Result wrapper for saga operations.
 */
public class SagaResult {

    private final Result<Void> result;
    private final SagaId sagaId;
    private final String sagaType;

    private SagaResult(Result<Void> result, SagaId sagaId, String sagaType) {
        this.result = result;
        this.sagaId = sagaId;
        this.sagaType = sagaType;
    }

    public static SagaResult success(SagaId sagaId, String sagaType) {
        return new SagaResult(Result.success(null), sagaId, sagaType);
    }

    public static SagaResult success() {
        return new SagaResult(Result.success(null), null, null);
    }

    public static SagaResult failure(Exception error, SagaId sagaId, String sagaType) {
        return new SagaResult(Result.failure(ErrorDetail.of("SAGA_ERROR", error.getMessage(), "saga.error")), sagaId, sagaType);
    }

    public static SagaResult failure(String message) {
        return new SagaResult(Result.failure(ErrorDetail.of("SAGA_ERROR", message, "saga.error")), null, null);
    }

    public boolean isSuccess() {
        return result.isSuccess();
    }

    public boolean isFailure() {
        return result.isFailure();
    }

    public Void getValue() {
        return result.getValue().orElse(null);
    }

    public ErrorDetail getError() {
        return result.getError().orElse(null);
    }

    public SagaId getSagaId() {
        return sagaId;
    }

    public String getSagaType() {
        return sagaType;
    }
}