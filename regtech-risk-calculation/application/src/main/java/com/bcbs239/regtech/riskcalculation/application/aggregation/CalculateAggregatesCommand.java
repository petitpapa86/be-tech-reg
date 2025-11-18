package com.bcbs239.regtech.riskcalculation.application.aggregation;

import com.bcbs239.regtech.riskcalculation.domain.calculation.CalculatedExposure;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Command to calculate aggregates and concentration indices for classified exposures.
 * Contains the batch of classified exposures ready for aggregation.
 */
public record CalculateAggregatesCommand(
    @NotNull(message = "Batch ID is required")
    BatchId batchId,
    
    @NotNull(message = "Classified exposures list is required")
    @NotEmpty(message = "Classified exposures list cannot be empty")
    List<CalculatedExposure> classifiedExposures
) {
    
    /**
     * Get the number of exposures to be aggregated
     */
    public int getExposureCount() {
        return classifiedExposures != null ? classifiedExposures.size() : 0;
    }
}