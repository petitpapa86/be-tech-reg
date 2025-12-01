package com.bcbs239.regtech.riskcalculation.domain.calculation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;

/**
 * Domain event indicating that a batch calculation has failed.
 */
public class BatchCalculationFailedEvent extends DomainEvent {
    private final String batchId;
    private final String bankId;
    private final String errorCode;
    private final String errorMessage;
    private final String failureReason;

    public BatchCalculationFailedEvent(String batchId, String bankId, String errorCode, 
                                      String errorMessage, String failureReason) {
        super(batchId, "BatchCalculationFailed");
        this.batchId = batchId;
        this.bankId = bankId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.failureReason = failureReason;
    }

    @Override
    public String eventType() {
        return "BatchCalculationFailed";
    }

    public String getBatchId() {
        return batchId;
    }

    public String getBankId() {
        return bankId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public String toString() {
        return "BatchCalculationFailedEvent{" +
                "batchId='" + batchId + '\'' +
                ", bankId='" + bankId + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", failureReason='" + failureReason + '\'' +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
