package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration event published when batch quality validation completes successfully.
 * This event notifies downstream modules (reporting) that quality validation results
 * are available for a batch. Detailed results are stored in S3 and can be accessed
 * via the S3 reference URI.
 *
 * Event versioning: v1.0 - Initial version with essential quality data
 */
@Getter
public class BatchQualityCompletedIntegrationEvent extends IntegrationEvent {

    private static final String EVENT_VERSION = "1.0";

    private final String batchId;
    private final String bankId;
    private final String s3ReferenceUri;
    private final Map<String, Object> validationSummary;
    private final Map<String, Object> processingMetadata;
    private final Instant completedAt;
    private final String eventVersion;

    @JsonCreator
    public BatchQualityCompletedIntegrationEvent(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("s3ReferenceUri") String s3ReferenceUri,
            @JsonProperty("validationSummary") Map<String, Object> validationSummary,
            @JsonProperty("processingMetadata") Map<String, Object> processingMetadata,
            @JsonProperty("completedAt") Instant completedAt
    ) {
        super(batchId, Maybe.none(), "BatchQualityCompletedIntegrationEvent");
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3ReferenceUri = s3ReferenceUri;
        this.validationSummary = validationSummary != null ? validationSummary : new HashMap<>();
        this.processingMetadata = processingMetadata != null ? processingMetadata : new HashMap<>();
        this.completedAt = completedAt;
        this.eventVersion = EVENT_VERSION;
    }

    // Convenience constructor without completedAt for creation
    public BatchQualityCompletedIntegrationEvent(
            String batchId,
            String bankId,
            String s3ReferenceUri,
            Map<String, Object> validationSummary,
            Map<String, Object> processingMetadata
    ) {
        this(batchId, bankId, s3ReferenceUri, validationSummary, processingMetadata, Instant.now());
    }

    @Override
    public String eventType() {
        return "BatchQualityCompletedIntegrationEvent";
    }

    @Override
    public String toString() {
        return "BatchQualityCompletedIntegrationEvent{" +
                "batchId='" + batchId + '\'' +
                ", bankId='" + bankId + '\'' +
                ", s3ReferenceUri='" + s3ReferenceUri + '\'' +
                ", completedAt=" + completedAt +
                ", version='" + eventVersion + '\'' +
                '}';
    }
}