package com.bcbs239.regtech.dataquality.application.integration;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;

/**
 * Service interface for publishing cross-module events.
 * Handles communication with other modules through the CrossModuleEventBus.
 */
public interface CrossModuleEventPublisher {
    
    /**
     * Publishes a BatchQualityCompleted event when validation is finished.
     * 
     * @param batchId The batch that was validated
     * @param bankId The bank that owns the batch
     * @param qualityScores The calculated quality scores
     * @param detailsReference Reference to detailed results in S3
     * @return Result indicating success or failure of event publishing
     */
    Result<Void> publishBatchQualityCompleted(
        BatchId batchId,
        BankId bankId,
        QualityScores qualityScores,
        S3Reference detailsReference
    );
    
    /**
     * Publishes a BatchQualityCompleted event with additional metadata.
     * 
     * @param batchId The batch that was validated
     * @param bankId The bank that owns the batch
     * @param qualityScores The calculated quality scores
     * @param detailsReference Reference to detailed results in S3
     * @param correlationId Correlation ID for tracing
     * @return Result indicating success or failure of event publishing
     */
    Result<Void> publishBatchQualityCompleted(
        BatchId batchId,
        BankId bankId,
        QualityScores qualityScores,
        S3Reference detailsReference,
        String correlationId
    );
    
    /**
     * Publishes a BatchQualityFailed event when validation fails.
     * 
     * @param batchId The batch that failed validation
     * @param bankId The bank that owns the batch
     * @param errorMessage The error message describing the failure
     * @return Result indicating success or failure of event publishing
     */
    Result<Void> publishBatchQualityFailed(
        BatchId batchId,
        BankId bankId,
        String errorMessage
    );
    
    /**
     * Publishes a BatchQualityFailed event with correlation ID.
     * 
     * @param batchId The batch that failed validation
     * @param bankId The bank that owns the batch
     * @param errorMessage The error message describing the failure
     * @param correlationId Correlation ID for tracing
     * @return Result indicating success or failure of event publishing
     */
    Result<Void> publishBatchQualityFailed(
        BatchId batchId,
        BankId bankId,
        String errorMessage,
        String correlationId
    );
}