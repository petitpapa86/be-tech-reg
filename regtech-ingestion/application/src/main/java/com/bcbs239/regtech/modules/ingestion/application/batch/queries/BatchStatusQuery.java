package com.bcbs239.regtech.modules.ingestion.application.batch.queries;

import com.bcbs239.regtech.modules.ingestion.domain.batch.BatchId;

/**
 * Query for retrieving batch status information.
 */
public record BatchStatusQuery(
    BatchId batchId,
    String authToken
) {
}