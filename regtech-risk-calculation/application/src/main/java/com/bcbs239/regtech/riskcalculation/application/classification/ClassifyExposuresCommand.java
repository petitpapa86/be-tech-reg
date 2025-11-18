package com.bcbs239.regtech.riskcalculation.application.classification;

import com.bcbs239.regtech.riskcalculation.domain.calculation.CalculatedExposure;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankId;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Command to classify exposures by geographic region and sector category.
 * Contains the batch of exposures to be classified and the bank context.
 */
public record ClassifyExposuresCommand(
    @NotNull(message = "Batch ID is required")
    BatchId batchId,
    
    @NotNull(message = "Exposures list is required")
    @NotEmpty(message = "Exposures list cannot be empty")
    List<CalculatedExposure> exposures,
    
    @NotNull(message = "Bank ID is required")
    BankId bankId
) {
    
    /**
     * Get the number of exposures to be classified
     */
    public int getExposureCount() {
        return exposures != null ? exposures.size() : 0;
    }
}