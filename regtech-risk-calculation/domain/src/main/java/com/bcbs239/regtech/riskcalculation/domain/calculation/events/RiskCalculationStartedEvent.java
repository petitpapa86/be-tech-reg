package com.bcbs239.regtech.riskcalculation.domain.calculation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain event raised when a batch calculation is started.
 * This event marks the beginning of the calculation process.
 */
@Getter
public class DataQualityStartedEvent extends DomainEvent {
    
    private final String batchId;
    private final String bankId;
    private final int totalExposures;
    private final Instant startedAt;
    
    @JsonCreator
    public DataQualityStartedEvent(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("totalExposures") int totalExposures,
            @JsonProperty("startedAt") Instant startedAt) {
        super(batchId, "BatchCalculationStarted");
        this.batchId = batchId;
        this.bankId = bankId;
        this.totalExposures = totalExposures;
        this.startedAt = startedAt;
    }
    
    @Override
    public String eventType() {
        return "BatchCalculationStarted";
    }

    @Override
    public String toString() {
        return "BatchCalculationStartedEvent{" +
                "batchId='" + batchId + '\'' +
                ", bankId='" + bankId + '\'' +
                ", totalExposures=" + totalExposures +
                ", startedAt=" + startedAt +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
