package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;

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

