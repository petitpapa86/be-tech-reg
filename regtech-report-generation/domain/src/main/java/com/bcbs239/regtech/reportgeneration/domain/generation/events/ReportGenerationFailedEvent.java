package com.bcbs239.regtech.reportgeneration.domain.generation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.FailureReason;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain event raised when report generation fails
 * Contains failure information for alerting and recovery
 */
@Getter
public class ReportGenerationFailedEvent extends DomainEvent {
    
    private final BatchId batchId;
    private final FailureReason failureReason;
    private final Instant failedAt;
    
    public ReportGenerationFailedEvent(
            String correlationId,
            BatchId batchId,
            FailureReason failureReason,
            Instant failedAt) {
        super(correlationId, "ReportGenerationFailed");
        this.batchId = batchId;
        this.failureReason = failureReason;
        this.failedAt = failedAt;
    }
    
    @Override
    public String eventType() {
        return "ReportGenerationFailed";
    }
}
