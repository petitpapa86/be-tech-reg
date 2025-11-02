package com.bcbs239.regtech.ingestion.application.query;

import com.bcbs239.regtech.ingestion.domain.model.BatchId;

/**
 * Query for retrieving batch status and processing information.
 * Contains batch ID and authentication information for security.
 */
public record BatchStatusQuery(
    BatchId batchId,
    String authToken
) {
    
    public BatchStatusQuery {
        if (batchId == null) {
            throw new IllegalArgumentException("Batch ID cannot be null");
        }
        if (authToken == null || authToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Auth token cannot be null or empty");
        }
    }
}