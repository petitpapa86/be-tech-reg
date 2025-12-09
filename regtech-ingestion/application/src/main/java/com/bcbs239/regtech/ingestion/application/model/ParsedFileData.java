package com.bcbs239.regtech.ingestion.application.model;

import com.bcbs239.regtech.ingestion.domain.model.CreditRiskMitigation;
import com.bcbs239.regtech.ingestion.domain.model.LoanExposure;

import java.util.List;
import java.util.Map;

/**
 * Top-level DTO used across the application layer to carry parsed file data.
 */
public record ParsedFileData(
    List<LoanExposure> exposures,
    List<CreditRiskMitigation> creditRiskMitigation,
    Map<String, Object> metadata
) {
    public int totalExposures() {
        return exposures == null ? 0 : exposures.size();
    }
}


