package com.bcbs239.regtech.dataquality.domain.report.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.dataquality.domain.report.QualityReportId;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;

import java.time.Instant;

/**
 * Domain event raised when quality validation fails for a batch.
 */
public class QualityValidationFailedEvent extends DomainEvent {

    private final QualityReportId reportId;
    private final BatchId batchId;
    private final BankId bankId;
    private final String errorMessage;
    private final Instant occurredAt;

    public QualityValidationFailedEvent(QualityReportId reportId, BatchId batchId, BankId bankId, String errorMessage, Instant occurredAt) {
        super("QualityValidationFailedEvent", null, "QualityValidationFailed");
        if (reportId == null) throw new IllegalArgumentException("Report ID cannot be null");
        if (batchId == null) throw new IllegalArgumentException("Batch ID cannot be null");
        if (bankId == null) throw new IllegalArgumentException("Bank ID cannot be null");
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be null or empty");
        }
        if (occurredAt == null) throw new IllegalArgumentException("Occurred at cannot be null");
        this.reportId = reportId;
        this.batchId = batchId;
        this.bankId = bankId;
        this.errorMessage = errorMessage;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String eventType() {
        return "QualityValidationFailed";
    }
}

