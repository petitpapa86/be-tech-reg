package com.bcbs239.regtech.reportgeneration.application.generation;

import com.bcbs239.regtech.reportgeneration.domain.generation.CalculationResults;
import com.bcbs239.regtech.reportgeneration.domain.generation.QualityResults;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.*;
import com.bcbs239.regtech.reportgeneration.domain.generation.CalculatedExposure;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Comprehensive report data value object (flattened)
 *
 * Contains a small set of value objects for use by report generators so callers
 * don't need to drill into nested structures.
 */
@Getter
public class ComprehensiveReportData {

    // All value objects (no primitives at this level)
    private final BatchId batchId;
    private final BankId bankId;
    private final BankName bankName;
    private final ReportingDate reportingDate;

    // Flattened quality data
    private final QualityScore overallScore;
    private final ComplianceStatus complianceStatus;
    private final List<com.bcbs239.regtech.core.domain.recommendations.QualityInsight> recommendations;

    // Flattened calculation data
    private final AmountEur tierOneCapital;
    private final int totalExposures;
    private final List<CalculatedExposure> largeExposures;

    // Keep originals for complex queries
    private final QualityResults qualityResults;
    private final CalculationResults calculationResults;

    @Builder
    private ComprehensiveReportData(QualityResults qualityResults, CalculationResults calculationResults) {
        validate(qualityResults, calculationResults);

        this.qualityResults = qualityResults;
        this.calculationResults = calculationResults;

        // Extract with consistent types
        this.batchId = calculationResults.batchId();
        this.bankId = calculationResults.bankId();
        this.bankName = calculationResults.bankName();
        this.reportingDate = calculationResults.reportingDate();

        // Flatten quality (BigDecimal -> QualityScore)
        this.overallScore = QualityScore.of(qualityResults.getOverallScore());
        this.complianceStatus = qualityResults.getComplianceStatus();
        this.recommendations = qualityResults.getRecommendations();

        // Flatten calculation
        this.tierOneCapital = calculationResults.tierOneCapital();
        this.totalExposures = calculationResults.totalExposures();
        this.largeExposures = calculationResults.getLargeExposures();
    }

    private void validate(QualityResults quality, CalculationResults calc) {
        if (quality == null || calc == null) {
            throw new IllegalArgumentException("Both results required");
        }
        if (!quality.getBatchId().value().equals(calc.batchId().value())) {
            throw new IllegalStateException("Batch ID mismatch");
        }
        if (!quality.getBankId().value().equals(calc.bankId().value())) {
            throw new IllegalStateException("Bank ID mismatch");
        }
    }

    /**
     * Convenience accessor returning the overall quality score as BigDecimal.
     * Some legacy callers expect a numeric score; use this to obtain it.
     */
    public java.math.BigDecimal getOverallScoreAsBigDecimal() {
        return this.qualityResults.getOverallScore();
    }
}
