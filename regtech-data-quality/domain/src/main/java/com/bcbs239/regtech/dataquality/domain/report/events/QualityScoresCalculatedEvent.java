package com.bcbs239.regtech.dataquality.domain.report.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.core.domain.shared.valueobjects.QualityReportId;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain event raised when quality scores are calculated for a quality report.
 */
@Getter
public class QualityScoresCalculatedEvent extends DomainEvent {

    private final QualityReportId reportId;
    private final BatchId batchId;
    private final BankId bankId;
    private final QualityScores qualityScores;
    private final Instant occurredAt;

    @JsonCreator
    public QualityScoresCalculatedEvent(
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("reportId") QualityReportId reportId,
            @JsonProperty("batchId") BatchId batchId,
            @JsonProperty("bankId") BankId bankId,
            @JsonProperty("qualityScores") QualityScores qualityScores,
            @JsonProperty("occurredAt") Instant occurredAt) {
        super(correlationId);
        if (reportId == null) throw new IllegalArgumentException("Report ID cannot be null");
        if (batchId == null) throw new IllegalArgumentException("Batch ID cannot be null");
        if (bankId == null) throw new IllegalArgumentException("Bank ID cannot be null");
        if (qualityScores == null) throw new IllegalArgumentException("Quality scores cannot be null");
        if (occurredAt == null) throw new IllegalArgumentException("Occurred at cannot be null");
        this.reportId = reportId;
        this.batchId = batchId;
        this.bankId = bankId;
        this.qualityScores = qualityScores;
        this.occurredAt = occurredAt;
    }


}

