package com.bcbs239.regtech.dataquality.application.integration.events;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Cross-module event published when batch quality validation fails.
 */
@Getter
public class BatchQualityFailedEvent extends com.bcbs239.regtech.core.domain.events.IntegrationEvent {
    private static final String EVENT_TYPE = "BatchQualityFailed";
    
    private final BatchId batchId;
    private final BankId bankId;
    private final String errorMessage;
    private final Map<String, Object> errorDetails;
    private final Map<String, Object> processingMetadata;
    private final Instant timestamp;

    @JsonCreator
    public BatchQualityFailedEvent(
            @JsonProperty("batchId") BatchId batchId,
            @JsonProperty("bankId") BankId bankId,
            @JsonProperty("errorMessage") String errorMessage,
            @JsonProperty("errorDetails") Map<String, Object> errorDetails,
            @JsonProperty("processingMetadata") Map<String, Object> processingMetadata
    ) {
        super(batchId.value(), Maybe.none(), EVENT_TYPE);
        this.batchId = batchId;
        this.bankId = bankId;
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails != null ? errorDetails : new HashMap<>();
        this.processingMetadata = processingMetadata != null ? processingMetadata : new HashMap<>();
        this.timestamp = Instant.now();
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public String toString() {
        return "BatchQualityFailedEvent{" +
                "batchId=" + batchId.value() +
                ", bankId=" + bankId.value() +
                ", errorMessage='" + errorMessage + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
