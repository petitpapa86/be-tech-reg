package com.bcbs239.regtech.riskcalculation.domain.calculation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.riskcalculation.domain.calculation.ConcentrationIndices;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.*;

import java.time.Instant;

/**
 * Domain event published when risk calculation completes successfully for a batch
 */
public class BatchCalculationCompletedEvent  extends DomainEvent {
    
    private final BatchId batchId;
    private final BankId bankId;
    private final FileStorageUri resultFileUri;
    private final TotalExposures totalExposures;
    private final TotalAmountEur totalAmountEur;
    private final ConcentrationIndices concentrationIndices;
    private final Instant completedAt;
    
    public BatchCalculationCompletedEvent(BatchId batchId, BankId bankId, FileStorageUri resultFileUri,
                                        TotalExposures totalExposures, TotalAmountEur totalAmountEur,
                                        ConcentrationIndices concentrationIndices, Instant completedAt) {
        super(batchId.value(), "BatchCalculationCompletedEvent");
        this.batchId = batchId;
        this.bankId = bankId;
        this.resultFileUri = resultFileUri;
        this.totalExposures = totalExposures;
        this.totalAmountEur = totalAmountEur;
        this.concentrationIndices = concentrationIndices;
        this.completedAt = completedAt;
    }
    
    public BatchId getBatchId() {
        return batchId;
    }
    
    public BankId getBankId() {
        return bankId;
    }
    
    public FileStorageUri getResultFileUri() {
        return resultFileUri;
    }
    
    public TotalExposures getTotalExposures() {
        return totalExposures;
    }
    
    public TotalAmountEur getTotalAmountEur() {
        return totalAmountEur;
    }
    
    public ConcentrationIndices getConcentrationIndices() {
        return concentrationIndices;
    }
    
    public Instant getCompletedAt() {
        return completedAt;
    }
    
    @Override
    public String eventType() {
        return "BatchCalculationCompletedEvent";
    }
}