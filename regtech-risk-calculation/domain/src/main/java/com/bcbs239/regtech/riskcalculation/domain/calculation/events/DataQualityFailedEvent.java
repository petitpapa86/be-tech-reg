package com.bcbs239.regtech.riskcalculation.domain.calculation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Domain event indicating that a batch calculation has failed.
 * Raised by the Batch aggregate when calculation fails.
 */
public class BatchCalculationFailedEvent extends DomainEvent {
    private final String batchId;
    private final String bankId;
    private final String reason;
    private final Instant failedAt;

    @JsonCreator
    public BatchCalculationFailedEvent(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("reason") String reason,
            @JsonProperty("failedAt") Instant failedAt) {
        super(batchId, "BatchCalculationFailed");
        this.batchId = batchId;
        this.bankId = bankId;
        this.reason = reason;
        this.failedAt = failedAt;
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

    public String getReason() {
        return reason;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    @Override
    public String toString() {
        return "BatchCalculationFailedEvent{" +
                "batchId='" + batchId + '\'' +
                ", bankId='" + bankId + '\'' +
                ", reason='" + reason + '\'' +
                ", failedAt=" + failedAt +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
