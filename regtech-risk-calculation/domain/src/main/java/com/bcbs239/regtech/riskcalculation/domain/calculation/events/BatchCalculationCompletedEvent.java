package com.bcbs239.regtech.riskcalculation.domain.calculation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;

/**
 * Domain event indicating that a batch calculation has been completed successfully.
 */
public class BatchCalculationCompletedEvent extends DomainEvent {
    private final String batchId;
    private final String bankId;
    private final int totalExposures;
    private final double totalAmountEur;
    private final String resultFileUri;

    public BatchCalculationCompletedEvent(String batchId, String bankId, int totalExposures, 
                                         double totalAmountEur, String resultFileUri) {
        super(batchId, "BatchCalculationCompleted");
        this.batchId = batchId;
        this.bankId = bankId;
        this.totalExposures = totalExposures;
        this.totalAmountEur = totalAmountEur;
        this.resultFileUri = resultFileUri;
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

    public int getTotalExposures() {
        return totalExposures;
    }

    public double getTotalAmountEur() {
        return totalAmountEur;
    }

    public String getResultFileUri() {
        return resultFileUri;
    }

    @Override
    public String toString() {
        return "BatchCalculationCompletedEvent{" +
                "batchId='" + batchId + '\'' +
                ", bankId='" + bankId + '\'' +
                ", totalExposures=" + totalExposures +
                ", totalAmountEur=" + totalAmountEur +
                ", resultFileUri='" + resultFileUri + '\'' +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
