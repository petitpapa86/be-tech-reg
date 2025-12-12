package com.bcbs239.regtech.riskcalculation.application.integration.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

@Getter
public class BatchIngestedEvent extends DomainEvent {
    private static final String EVENT_VERSION = "1.0";
    private final String batchId;
    private final String bankId;
    private final String s3Uri;
    private final int totalExposures;
    private final long fileSizeBytes;
    private final Instant completedAt;
    private final String eventVersion;

    @JsonCreator
    public BatchIngestedEvent(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("s3Uri") String s3Uri,
            @JsonProperty("totalExposures") int totalExposures,
            @JsonProperty("fileSizeBytes") long fileSizeBytes,
            @JsonProperty("completedAt") Instant completedAt) {
        super(batchId, Maybe.none(), "BatchIngestedEvent");
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3Uri = s3Uri;
        this.totalExposures = totalExposures;
        this.fileSizeBytes = fileSizeBytes;
        this.completedAt = completedAt;
        this.eventVersion = EVENT_VERSION;
    }

    @Override
    public String eventType() {
        return getEventType();
    }

    @Override
    public String toString() {
        return String.format(
                "BatchIngestedEvent{batchId='%s', bankId='%s', s3Uri='%s', totalExposures=%d, fileSizeBytes=%d, completedAt=%s, version='%s'}",
                batchId, bankId, s3Uri, totalExposures, fileSizeBytes, completedAt, eventVersion
        );
    }
}
