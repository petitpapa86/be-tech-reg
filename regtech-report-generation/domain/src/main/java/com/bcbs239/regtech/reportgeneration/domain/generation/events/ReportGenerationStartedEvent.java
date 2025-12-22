package com.bcbs239.regtech.reportgeneration.domain.generation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BankId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportId;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain event raised when report generation starts
 * Signals the beginning of the report generation process
 * 
 * Requirements: 14.1
 */
@Getter
public class ReportGenerationStartedEvent extends DomainEvent {
    
    private final ReportId reportId;
    private final BatchId batchId;
    private final BankId bankId;
    private final Instant startedAt;
    
    public ReportGenerationStartedEvent(
            String correlationId,
            ReportId reportId,
            BatchId batchId,
            BankId bankId,
            Instant startedAt) {
        super(correlationId);
        this.reportId = reportId;
        this.batchId = batchId;
        this.bankId = bankId;
        this.startedAt = startedAt;
    }
}
