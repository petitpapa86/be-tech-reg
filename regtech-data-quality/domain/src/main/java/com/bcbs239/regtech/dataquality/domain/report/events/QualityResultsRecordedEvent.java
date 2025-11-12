package com.bcbs239.regtech.dataquality.domain.report.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.dataquality.domain.report.QualityReportId;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationSummary;

import java.time.Instant;

/**
 * Domain event raised when validation results are recorded for a quality report.
 */
public class QualityResultsRecordedEvent extends DomainEvent {

    private final QualityReportId reportId;
    private final BatchId batchId;
    private final BankId bankId;
    private final ValidationSummary validationSummary;
    private final Instant occurredAt;

    public QualityResultsRecordedEvent(QualityReportId reportId, BatchId batchId, BankId bankId, ValidationSummary validationSummary, Instant occurredAt) {
        super("QualityResultsRecordedEvent", null, "QualityResultsRecorded");
        if (reportId == null) throw new IllegalArgumentException("Report ID cannot be null");
        if (batchId == null) throw new IllegalArgumentException("Batch ID cannot be null");
        if (bankId == null) throw new IllegalArgumentException("Bank ID cannot be null");
        if (validationSummary == null) throw new IllegalArgumentException("Validation summary cannot be null");
        if (occurredAt == null) throw new IllegalArgumentException("Occurred at cannot be null");
        this.reportId = reportId;
        this.batchId = batchId;
        this.bankId = bankId;
        this.validationSummary = validationSummary;
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

    public ValidationSummary getValidationSummary() {
        return validationSummary;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String eventType() {
        return "QualityResultsRecorded";
    }
}

