package com.bcbs239.regtech.ingestion.domain.integrationevents;

import com.bcbs239.regtech.core.application.IntegrationEvent;
import lombok.Getter;

import java.time.Instant;

/**
 * Integration event published when a batch has been successfully ingested.
 * This event notifies downstream modules (risk calculation, data quality, billing)
 * that exposure data is available for processing.
 * 
 * Event versioning: v1.0 - Initial version with core batch information
 */
@Getter
public class BatchIngestedEvent extends IntegrationEvent {
    
    private static final String EVENT_VERSION = "1.0";
    
    private final String batchId;
    private final String bankId;
    private final String s3Uri;
    private final int totalExposures;
    private final long fileSizeBytes;
    private final Instant completedAt;
    private final String eventVersion;
    
    public BatchIngestedEvent(
            String batchId,
            String bankId, 
            String s3Uri,
            int totalExposures,
            long fileSizeBytes,
            Instant completedAt) {
        super();
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3Uri = s3Uri;
        this.totalExposures = totalExposures;
        this.fileSizeBytes = fileSizeBytes;
        this.completedAt = completedAt;
        this.eventVersion = EVENT_VERSION;
    }
    
    @Override
    public String toString() {
        return String.format(
            "BatchIngestedEvent{batchId='%s', bankId='%s', s3Uri='%s', totalExposures=%d, fileSizeBytes=%d, completedAt=%s, version='%s'}",
            batchId, bankId, s3Uri, totalExposures, fileSizeBytes, completedAt, eventVersion
        );
    }
}

