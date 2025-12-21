package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

/**
 * Integration event published when a batch processing has been completed successfully.
 * This event notifies downstream modules (risk calculation, data quality, billing)
 * that batch processing has finished and results are available.
 * 
 * Event versioning: v1.0 - Initial version with core batch completion information
 */
@Getter
public class BatchCompletedIntegrationEvent extends IntegrationEvent {
    
    private static final String EVENT_VERSION = "1.0";
    
    private final String batchId;
    private final String bankId;
    private final String s3Uri;
    private final int totalExposures;
    private final long fileSizeBytes;
    private final Instant completedAt;
    private final String eventVersion;
    
    @JsonCreator
    public BatchCompletedIntegrationEvent(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("s3Uri") String s3Uri,
            @JsonProperty("totalExposures") int totalExposures,
            @JsonProperty("fileSizeBytes") long fileSizeBytes,
            @JsonProperty("completedAt") Instant completedAt,
            @JsonProperty("correlationId") String correlationId) {
        super(correlationId);
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

    //verify event fields are correctly  set
    public boolean isValid() {
        return batchId != null && !batchId.isEmpty()
            && bankId != null && !bankId.isEmpty()
            && s3Uri != null && !s3Uri.isEmpty()
            && totalExposures >= 0
            && fileSizeBytes >= 0;
    }
    @Override
    public String toString() {
        return String.format(
            "BatchCompletedIntegrationEvent{batchId='%s', bankId='%s', s3Uri='%s', totalExposures=%d, fileSizeBytes=%d, completedAt=%s, version='%s'}",
            batchId, bankId, s3Uri, totalExposures, fileSizeBytes, completedAt, eventVersion
        );
    }
}
