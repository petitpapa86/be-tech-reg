package com.bcbs239.regtech.riskcalculation.domain.calculation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;

import java.time.Instant;

/**
 * Domain event indicating that a batch calculation has been completed successfully.
 * Raised by the Batch aggregate when calculation completes.
 */
public class BatchCalculationCompletedEvent extends DomainEvent {
    private final String batchId;
    private final String bankId;
    private final int processedExposures;
    private final String calculationResultsUri;
    private final Instant completedAt;

    public BatchCalculationCompletedEvent(
            String batchId,
            String bankId,
            int processedExposures,
            String calculationResultsUri,
            Instant completedAt) {
        super(batchId, "BatchCalculationCompleted");
        this.batchId = batchId;
        this.bankId = bankId;
        this.processedExposures = processedExposures;
        this.calculationResultsUri = calculationResultsUri;
        this.completedAt = completedAt;
    }

    @Override
    public String eventType() {
        return "BatchCalculationCompleted";
    }

    public String getBatchId() {
        return batchId;
    }

    public String getBankId() {
        return bankId;
    }

    public int getProcessedExposures() {
        return processedExposures;
    }

    public String getCalculationResultsUri() {
        return calculationResultsUri;
    }

    public Instant getCompletedAt() {
        return completedAt;
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
