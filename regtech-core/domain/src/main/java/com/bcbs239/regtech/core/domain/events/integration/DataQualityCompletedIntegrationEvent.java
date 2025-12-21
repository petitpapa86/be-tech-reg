package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

@Getter
public class DataQualityCompletedIntegrationEvent extends IntegrationEvent {

    private static final String EVENT_VERSION = "1.0";

    private final String batchId;
    private final String bankId;
    private final String s3ReferenceUri;
    private final double overallScore;
    private final String qualityGrade;
    private final Instant completedAt;
    private final String eventVersion;

    @JsonCreator
    public DataQualityCompletedIntegrationEvent(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("s3ReferenceUri") String s3ReferenceUri,
            @JsonProperty("overallScore") double overallScore,
            @JsonProperty("qualityGrade") String qualityGrade,
            @JsonProperty("completedAt") Instant completedAt
    ) {
        super(batchId, Maybe.none(), "BatchQualityCompletedIntegrationEvent");
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3ReferenceUri = s3ReferenceUri;
        this.overallScore = overallScore;
        this.qualityGrade = qualityGrade;
        this.completedAt = completedAt;
        this.eventVersion = EVENT_VERSION;
    }

    // Convenience constructor without completedAt for creation
    public DataQualityCompletedIntegrationEvent(
            String batchId,
            String bankId,
            String s3ReferenceUri,
            double overallScore,
            String qualityGrade
    ) {
        this(batchId, bankId, s3ReferenceUri, overallScore, qualityGrade, Instant.now());
    }

    @Override
    public String eventType() {
        return "DataQualityCompletedIntegrationEvent";
    }
}
