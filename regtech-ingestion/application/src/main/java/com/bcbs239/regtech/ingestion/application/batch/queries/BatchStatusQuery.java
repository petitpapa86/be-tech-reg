package com.bcbs239.regtech.ingestion.application.batch.queries;

import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;

/**
 * Query for retrieving batch status information.
 */
public record BatchStatusQuery(
        BatchId batchId,
        String authToken
) { }

