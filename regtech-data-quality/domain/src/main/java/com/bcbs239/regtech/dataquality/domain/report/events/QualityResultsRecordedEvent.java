package com.bcbs239.regtech.dataquality.domain.report.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.shared.valueobjects.QualityReportId;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationSummary;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain event raised when validation results are recorded for a quality report.
 */
@Getter
public class QualityResultsRecordedEvent extends DomainEvent {

    private final QualityReportId reportId;
    private final BatchId batchId;
    private final BankId bankId;
    private final ValidationSummary validationSummary;
    private final Instant occurredAt;

    @JsonCreator
    public QualityResultsRecordedEvent(
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("reportId") QualityReportId reportId,
            @JsonProperty("batchId") BatchId batchId,
            @JsonProperty("bankId") BankId bankId,
            @JsonProperty("validationSummary") ValidationSummary validationSummary,
            @JsonProperty("occurredAt") Instant occurredAt) {
        super(correlationId);
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

    // Convenience constructor for programmatic creation
    public QualityResultsRecordedEvent(QualityReportId reportId, BatchId batchId, BankId bankId, ValidationSummary validationSummary, Instant occurredAt, String correlationId) {
        super(correlationId);
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

}

