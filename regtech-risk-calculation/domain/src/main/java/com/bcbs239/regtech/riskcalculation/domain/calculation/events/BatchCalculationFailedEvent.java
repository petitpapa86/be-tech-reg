package com.bcbs239.regtech.riskcalculation.domain.calculation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankId;

import java.time.Instant;

/**
 * Domain event published when risk calculation fails for a batch
 */
public class BatchCalculationFailedEvent extends DomainEvent {
    
    private final BatchId batchId;
    private final BankId bankId;
    private final String errorMessage;
    private final Instant failedAt;
    
    public BatchCalculationFailedEvent(BatchId batchId, BankId bankId, String errorMessage, Instant failedAt) {
        super(batchId.value(), "BatchCalculationFailedEvent");
        this.batchId = batchId;
        this.bankId = bankId;
        this.errorMessage = errorMessage;
        this.failedAt = failedAt;
    }
    
    public BatchId getBatchId() {
        return batchId;
    }
    
    public BankId getBankId() {
        return bankId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public Instant getFailedAt() {
        return failedAt;
    }
    
    @Override
    public String eventType() {
        return "BatchCalculationFailedEvent";
    }
}