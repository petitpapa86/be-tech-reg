package com.bcbs239.regtech.dataquality.application.integration.events;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Cross-module event published when batch quality validation completes successfully.
 * This is a simple data carrier published via CrossModuleEventBus (Spring ApplicationEventPublisher under the hood).
 */
@Getter
public class BatchQualityCompletedEvent extends com.bcbs239.regtech.core.domain.events.IntegrationEvent {
    private static final String EVENT_TYPE = "BatchQualityCompleted";
    
    private final BatchId batchId;
    private final BankId bankId;
    private final QualityScores qualityScores;
    private final S3Reference s3Reference;
    private final Map<String, Object> validationSummary;
    private final Map<String, Object> processingMetadata;
    private final Instant timestamp;

    public BatchQualityCompletedEvent(
            BatchId batchId,
            BankId bankId,
            QualityScores qualityScores,
            S3Reference s3Reference,
            Map<String, Object> validationSummary,
            Map<String, Object> processingMetadata
    ) {
        super(batchId.value(), Maybe.none(), EVENT_TYPE);
        this.batchId = batchId;
        this.bankId = bankId;
        this.qualityScores = qualityScores;
        this.s3Reference = s3Reference;
        this.validationSummary = validationSummary != null ? validationSummary : new HashMap<>();
        this.processingMetadata = processingMetadata != null ? processingMetadata : new HashMap<>();
        this.timestamp = Instant.now();
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public String toString() {
        return "BatchQualityCompletedEvent{" +
                "batchId=" + batchId.value() +
                ", bankId=" + bankId.value() +
                ", overallScore=" + qualityScores.overallScore() +
                ", grade=" + qualityScores.grade() +
                ", timestamp=" + timestamp +
                '}';
    }
}
