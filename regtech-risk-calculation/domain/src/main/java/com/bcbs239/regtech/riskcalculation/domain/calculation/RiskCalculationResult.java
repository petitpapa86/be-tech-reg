package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.protection.ProtectedExposure;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BankInfo;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Result value object for risk calculation
 * Contains all information about a completed risk calculation
 * Requirement: 6.1
 */
public record RiskCalculationResult(
        String batchId,
        BankInfo bankInfo,
        List<ProtectedExposure> calculatedExposures,
        PortfolioAnalysis analysis,
        Instant ingestedAt) {

    public RiskCalculationResult {
        Objects.requireNonNull(batchId, "Batch ID cannot be null");
        Objects.requireNonNull(bankInfo, "Bank info cannot be null");
        Objects.requireNonNull(calculatedExposures, "Calculated exposures cannot be null");
        Objects.requireNonNull(analysis, "Portfolio analysis cannot be null");
        Objects.requireNonNull(ingestedAt, "Ingested timestamp cannot be null");
        
        // Make defensive copy
        calculatedExposures = List.copyOf(calculatedExposures);
    }

    @Override
    public String toString() {
        return "RiskCalculationResult{" +
                "batchId='" + batchId + '\'' +
                ", bankInfo=" + bankInfo +
                ", totalExposures=" + calculatedExposures.size() +
                ", totalPortfolio=" + analysis.getTotalPortfolio() +
                ", ingestedAt=" + ingestedAt +
                '}';
    }
}
