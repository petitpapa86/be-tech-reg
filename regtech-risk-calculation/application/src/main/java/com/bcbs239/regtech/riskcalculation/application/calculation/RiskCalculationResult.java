package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankInfo;

import java.time.Instant;
import java.util.Objects;

/**
 * Result value object for risk calculation
 * Contains all information about a completed risk calculation
 * Requirement: 6.1
 */
public record RiskCalculationResult(String batchId, BankInfo bankInfo, int totalExposures, PortfolioAnalysis analysis,
                                    Instant ingestedAt) {

    public RiskCalculationResult(
            String batchId,
            BankInfo bankInfo,
            int totalExposures,
            PortfolioAnalysis analysis,
            Instant ingestedAt
    ) {
        this.batchId = Objects.requireNonNull(batchId, "Batch ID cannot be null");
        this.bankInfo = Objects.requireNonNull(bankInfo, "Bank info cannot be null");
        this.totalExposures = totalExposures;
        this.analysis = Objects.requireNonNull(analysis, "Portfolio analysis cannot be null");
        this.ingestedAt = Objects.requireNonNull(ingestedAt, "Ingested timestamp cannot be null");

        if (totalExposures < 0) {
            throw new IllegalArgumentException("Total exposures cannot be negative");
        }
    }

    @Override
    public String toString() {
        return "RiskCalculationResult{" +
                "batchId='" + batchId + '\'' +
                ", bankInfo=" + bankInfo +
                ", totalExposures=" + totalExposures +
                ", totalPortfolio=" + analysis.getTotalPortfolio() +
                ", ingestedAt=" + ingestedAt +
                '}';
    }
}
