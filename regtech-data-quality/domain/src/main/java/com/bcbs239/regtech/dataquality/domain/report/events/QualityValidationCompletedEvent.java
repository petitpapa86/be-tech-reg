package com.bcbs239.regtech.dataquality.domain.report.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.dataquality.domain.quality.QualityGrade;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.core.domain.shared.valueobjects.QualityReportId;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain event raised when quality validation completes successfully for a batch.
 */
@Getter
public class QualityValidationCompletedEvent extends DomainEvent {

    private final QualityReportId reportId;
    private final BatchId batchId;
    private final BankId bankId;
    private final QualityScores qualityScores;
    private final QualityGrade qualityGrade;
    private final S3Reference detailsReference;
    private final Instant occurredAt;
    private final String filename;

    @JsonCreator
    public QualityValidationCompletedEvent(
            @JsonProperty("reportId") QualityReportId reportId,
            @JsonProperty("batchId") BatchId batchId,
            @JsonProperty("bankId") BankId bankId,
            @JsonProperty("qualityScores") QualityScores qualityScores,
            @JsonProperty("qualityGrade") QualityGrade qualityGrade,
            @JsonProperty("detailsReference") S3Reference detailsReference,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("filename") String filename){
        super("QualityValidationCompletedEvent");
        if (reportId == null) throw new IllegalArgumentException("Report ID cannot be null");
        if (batchId == null) throw new IllegalArgumentException("Batch ID cannot be null");
        if (bankId == null) throw new IllegalArgumentException("Bank ID cannot be null");
        if (qualityScores == null) throw new IllegalArgumentException("Quality scores cannot be null");
        if (detailsReference == null) throw new IllegalArgumentException("Details reference cannot be null");
        if (occurredAt == null) throw new IllegalArgumentException("Occurred at cannot be null");
        if (qualityGrade == null) throw new IllegalArgumentException("Quality grade cannot be null");
        if (filename == null || filename.isEmpty()) throw new IllegalArgumentException("Filename cannot be null or empty");
        this.qualityGrade = qualityGrade;
        this.reportId = reportId;
        this.batchId = batchId;
        this.bankId = bankId;
        this.qualityScores = qualityScores;
        this.detailsReference = detailsReference;
        this.occurredAt = occurredAt;
        this.filename = filename;
    }

}

