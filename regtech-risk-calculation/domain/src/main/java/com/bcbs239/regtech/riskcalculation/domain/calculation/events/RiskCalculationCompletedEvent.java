package com.bcbs239.regtech.riskcalculation.domain.calculation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain event indicating that a batch calculation has been completed successfully.
 * Raised by the Batch aggregate when calculation completes.
 */
@Getter
public class DataQualityCompletedEvent extends DomainEvent {
    private final String batchId;
    private final String bankId;
    private final int processedExposures;
    private final String calculationResultsUri;
    private final Instant completedAt;
    private final BigDecimal TotalAmountEur;

    @JsonCreator
    public DataQualityCompletedEvent(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("processedExposures") int processedExposures,
            @JsonProperty("calculationResultsUri") String calculationResultsUri,
            @JsonProperty("completedAt") Instant completedAt,
            @JsonProperty("totalAmountEur")BigDecimal totalAmountEur) {
        super(batchId, "BatchCalculationCompleted");
        this.batchId = batchId;
        this.bankId = bankId;
        this.processedExposures = processedExposures;
        this.calculationResultsUri = calculationResultsUri;
        this.completedAt = completedAt;
        TotalAmountEur = totalAmountEur;
    }

    @Override
    public String eventType() {
        return "BatchCalculationCompleted";
    }

    @Override
    public String toString() {
        return "BatchCalculationCompletedEvent{" +
                "batchId='" + batchId + '\'' +
                ", bankId='" + bankId + '\'' +
                ", processedExposures=" + processedExposures +
                ", calculationResultsUri='" + calculationResultsUri + '\'' +
                ", completedAt=" + completedAt +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
