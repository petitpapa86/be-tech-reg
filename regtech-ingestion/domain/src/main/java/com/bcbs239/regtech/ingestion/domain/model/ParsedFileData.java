package com.bcbs239.regtech.ingestion.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Domain model representing parsed file data.
 * Contains the structured data extracted from uploaded files.
 */
public record ParsedFileData(
    List<LoanExposure> exposures,
    List<CreditRiskMitigation> creditRiskMitigation,
    Map<String, Object> metadata
) {
    /**
     * Get the total number of exposures in this parsed data.
     */
    public int totalExposures() {
        return exposures == null ? 0 : exposures.size();
    }
}
