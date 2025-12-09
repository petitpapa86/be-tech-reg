package com.bcbs239.regtech.reportgeneration.domain.generation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BankId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.FailureReason;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportId;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain event raised when report generation fails
 * Contains failure information for alerting and recovery
 * 
 * Requirements: 14.1
 */
@Getter
public class ReportGenerationFailedEvent extends DomainEvent {
    
    private final ReportId reportId;
    private final BatchId batchId;
    private final BankId bankId;
    private final FailureReason failureReason;
    private final Instant failedAt;
    
    public ReportGenerationFailedEvent(
            String correlationId,
            ReportId reportId,
            BatchId batchId,
            BankId bankId,
            FailureReason failureReason,
            Instant failedAt) {
        super(correlationId, "ReportGenerationFailed");
        this.reportId = reportId;
        this.batchId = batchId;
        this.bankId = bankId;
        this.failureReason = failureReason;
        this.failedAt = failedAt;
    }
    
    @Override
    public String eventType() {
        return "ReportGenerationFailed";
    }
}
