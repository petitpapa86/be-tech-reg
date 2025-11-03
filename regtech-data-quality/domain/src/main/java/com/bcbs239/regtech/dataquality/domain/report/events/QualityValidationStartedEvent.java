package com.bcbs239.regtech.dataquality.domain.report.events;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.dataquality.domain.report.QualityReportId;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;

import java.time.Instant;

/**
 * Domain event raised when quality validation starts for a batch.
 */
public record QualityValidationStartedEvent(
    QualityReportId reportId,
    BatchId batchId,
    BankId bankId,
    Instant occurredAt
) implements DomainEvent {
    
    public QualityValidationStartedEvent {
        if (reportId == null) throw new IllegalArgumentException("Report ID cannot be null");
        if (batchId == null) throw new IllegalArgumentException("Batch ID cannot be null");
        if (bankId == null) throw new IllegalArgumentException("Bank ID cannot be null");
        if (occurredAt == null) throw new IllegalArgumentException("Occurred at cannot be null");
    }
    
    @Override
    public String eventType() {
        return "QualityValidationStarted";
    }
}