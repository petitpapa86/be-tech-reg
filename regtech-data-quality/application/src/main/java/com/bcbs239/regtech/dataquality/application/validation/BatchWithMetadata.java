package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

/**
 * Wrapper containing batch exposures and their associated metadata.
 * Used when downloading batch data that includes both exposures and bank information.
 */
public record BatchWithMetadata(
    List<ExposureRecord> exposures,
    @Nullable Integer declaredCount,
    @Nullable LocalDate reportDate
) {
    /**
     * Creates a batch with metadata.
     * 
     * @param exposures List of exposure records
     * @param declaredCount Total exposures declared by the bank in metadata
     * @param reportDate Reporting date from bank metadata
     */
    public BatchWithMetadata {
        if (exposures == null) {
            throw new IllegalArgumentException("Exposures list cannot be null");
        }
    }
    
    /**
     * Creates a batch with only exposures (no metadata).
     */
    public static BatchWithMetadata withoutMetadata(List<ExposureRecord> exposures) {
        return new BatchWithMetadata(exposures, null, null);
    }
}
