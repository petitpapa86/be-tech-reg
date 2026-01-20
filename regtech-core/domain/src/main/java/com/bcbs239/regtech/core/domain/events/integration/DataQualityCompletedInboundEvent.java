package com.bcbs239.regtech.core.domain.events.integration;


import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

@Getter
public class DataQualityCompletedInboundEvent extends DomainEvent {
    private static final String EVENT_VERSION = "1.0";

    private final String batchId;
    private final String bankId;
    private final String s3ReferenceUri;
    private final double overallScore;
    private final String qualityGrade;
    private final Instant completedAt;

    // Optional enrichment from dataquality.quality_reports
    private final Integer totalExposures;
    private final Integer validExposures;
    private final Integer totalErrors;
    private final Boolean complianceStatus;

    // Optional dimension scores from QualityValidationCompletedEvent
    private final Double completenessScore;
    private final Double accuracyScore;
    private final Double consistencyScore;
    private final Double timelinessScore;
    private final Double uniquenessScore;
    private final Double validityScore;

    private final String eventVersion;
    private final String fileName;


    @JsonCreator
    public DataQualityCompletedInboundEvent(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("s3ReferenceUri") String s3ReferenceUri,
            @JsonProperty("overallScore") double overallScore,
            @JsonProperty("qualityGrade") String qualityGrade,
            @JsonProperty("completedAt") Instant completedAt,
            @JsonProperty("totalExposures") Integer totalExposures,
            @JsonProperty("validExposures") Integer validExposures,
            @JsonProperty("totalErrors") Integer totalErrors,
            @JsonProperty("complianceStatus") Boolean complianceStatus,
            @JsonProperty("completenessScore") Double completenessScore,
            @JsonProperty("accuracyScore") Double accuracyScore,
            @JsonProperty("consistencyScore") Double consistencyScore,
            @JsonProperty("timelinessScore") Double timelinessScore,
            @JsonProperty("uniquenessScore") Double uniquenessScore,
            @JsonProperty("validityScore") Double validityScore,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("fileName") String fileName
    ) {
        super(correlationId);
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3ReferenceUri = s3ReferenceUri;
        this.overallScore = overallScore;
        this.qualityGrade = qualityGrade;
        this.completedAt = completedAt;
        this.totalExposures = totalExposures;
        this.validExposures = validExposures;
        this.totalErrors = totalErrors;
        this.complianceStatus = complianceStatus;

        this.completenessScore = completenessScore;
        this.accuracyScore = accuracyScore;
        this.consistencyScore = consistencyScore;
        this.timelinessScore = timelinessScore;
        this.uniquenessScore = uniquenessScore;
        this.validityScore = validityScore;

        this.eventVersion = EVENT_VERSION;

        this.fileName = fileName;

    }




    public boolean isValid() {
        return batchId != null && !batchId.isEmpty()
                && bankId != null && !bankId.isEmpty()
                && s3ReferenceUri != null && !s3ReferenceUri.isEmpty()
                && overallScore >= 0.0 && overallScore <= 100.0
                && qualityGrade != null && !qualityGrade.isEmpty();
    }

}
