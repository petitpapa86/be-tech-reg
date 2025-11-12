package com.bcbs239.regtech.dataquality.domain.report.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.dataquality.domain.report.QualityReportId;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;

import java.time.Instant;

/**
 * Domain event raised when quality validation starts for a batch.
 */
public class QualityValidationStartedEvent extends DomainEvent {

    private final QualityReportId reportId;
    private final BatchId batchId;
    private final BankId bankId;
    private final Instant occurredAt;

    public QualityValidationStartedEvent(QualityReportId reportId, BatchId batchId, BankId bankId, Instant occurredAt) {
        super("QualityValidationStartedEvent", null, "QualityValidationStarted");
        if (reportId == null) throw new IllegalArgumentException("Report ID cannot be null");
        if (batchId == null) throw new IllegalArgumentException("Batch ID cannot be null");
        if (bankId == null) throw new IllegalArgumentException("Bank ID cannot be null");
        if (occurredAt == null) throw new IllegalArgumentException("Occurred at cannot be null");
        this.reportId = reportId;
        this.batchId = batchId;
        this.bankId = bankId;
        this.occurredAt = occurredAt;
    }

    public QualityReportId getReportId() {
        return reportId;
    }

    public BatchId getBatchId() {
        return batchId;
    }

    public BankId getBankId() {
        return bankId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String eventType() {
        return "QualityValidationStarted";
    }
}

