package com.bcbs239.regtech.reportgeneration.application.generation.events;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

@Getter
public class DataQualityFailedIntegrationEvent {
    private static final String EVENT_VERSION = "1.0";

    private final String batchId;
    private final String bankId;
    private final String s3ReferenceUri;
    private final double overallScore;
    private final String qualityGrade;
    private final Instant completedAt;
    private final String eventVersion;

    @JsonCreator
    public DataQualityFailedIntegrationEvent(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("s3ReferenceUri") String s3ReferenceUri,
            @JsonProperty("overallScore") double overallScore,
            @JsonProperty("qualityGrade") String qualityGrade,
            @JsonProperty("completedAt") Instant completedAt
    ) {
        super();
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3ReferenceUri = s3ReferenceUri;
        this.overallScore = overallScore;
        this.qualityGrade = qualityGrade;
        this.completedAt = completedAt;
        this.eventVersion = EVENT_VERSION;
    }

    public boolean isValid() {
        return batchId != null && !batchId.isEmpty()
                && bankId != null && !bankId.isEmpty()
                && s3ReferenceUri != null && !s3ReferenceUri.isEmpty()
                && overallScore >= 0.0 && overallScore <= 100.0
                && qualityGrade != null && !qualityGrade.isEmpty();
    }
}
