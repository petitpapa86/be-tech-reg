package com.bcbs239.regtech.modules.dataquality.application.queries;

import com.bcbs239.regtech.modules.dataquality.domain.shared.BatchId;

/**
 * Query to retrieve a quality report by batch ID.
 */
public record GetQualityReportQuery(
    BatchId batchId
) {
    
    /**
     * Creates a query for the specified batch ID.
     */
    public static GetQualityReportQuery forBatch(BatchId batchId) {
        return new GetQualityReportQuery(batchId);
    }
    
    /**
     * Validates the query parameters.
     */
    public void validate() {
        if (batchId == null) {
            throw new IllegalArgumentException("Batch ID cannot be null");
        }
    }
}