package com.bcbs239.regtech.riskcalculation.domain.calculation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankId;

import java.time.Instant;

/**
 * Domain event published when risk calculation starts for a batch
 */
public class BatchCalculationStartedEvent extends DomainEvent {
    
    private final BatchId batchId;
    private final BankId bankId;
    private final Instant startedAt;
    
    public BatchCalculationStartedEvent(BatchId batchId, BankId bankId, Instant startedAt) {
        super(batchId.value(), "BatchCalculationStartedEvent");
        this.batchId = batchId;
        this.bankId = bankId;
        this.startedAt = startedAt;
    }
    
    public BatchId getBatchId() {
        return batchId;
    }
    
    public BankId getBankId() {
        return bankId;
    }
    
    public Instant getStartedAt() {
        return startedAt;
    }
    
    @Override
    public String eventType() {
        return "BatchCalculationStartedEvent";
    }
}