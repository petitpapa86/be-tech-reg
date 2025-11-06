package com.bcbs239.regtech.dataquality.application.integration.events;

import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Cross-module event published when batch quality validation fails.
 */
@Getter
public class BatchQualityFailedEvent extends com.bcbs239.regtech.core.domain.events.IntegrationEvent {
    private final BatchId batchId;
    private final BankId bankId;
    private final String errorMessage;
    private final Map<String, Object> errorDetails;
    private final Map<String, Object> processingMetadata;
    private final Instant timestamp;

    public BatchQualityFailedEvent(
            BatchId batchId,
            BankId bankId,
            String errorMessage,
            Map<String, Object> errorDetails,
            Map<String, Object> processingMetadata
    ) {
        this.batchId = batchId;
        this.bankId = bankId;
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails != null ? errorDetails : new HashMap<>();
        this.processingMetadata = processingMetadata != null ? processingMetadata : new HashMap<>();
        this.timestamp = Instant.now();
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
