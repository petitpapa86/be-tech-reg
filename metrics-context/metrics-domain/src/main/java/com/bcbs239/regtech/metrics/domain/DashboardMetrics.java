package com.bcbs239.regtech.metrics.domain;

import lombok.Getter;

import java.util.List;
import java.util.OptionalDouble;

/**
 * Domain aggregate for dashboard metrics.
 * Business behavior lives here; persistence concerns live in infrastructure entities.
 */
@Getter
public class DashboardMetrics {

    private BankId bankId;

    /**
     * Month scope start date (first day of month).
     */
    private java.time.LocalDate periodStart;

    private Double overallScore;
    private Double dataQualityScore;
    private Double bcbsRulesScore;
    private Double completenessScore;

    private Integer totalFilesProcessed;
    private Integer totalViolations;
    private Integer totalReportsGenerated;

    // Data-quality counts (from dataquality.quality_reports)
    private Integer totalExposures;
    private Integer validExposures;
    private Integer totalErrors;

    private Long version;

    public DashboardMetrics(
            BankId bankId,
            java.time.LocalDate periodStart,
            Double overallScore,
            Double dataQualityScore,
            Double bcbsRulesScore,
            Double completenessScore,
            Integer totalFilesProcessed,
            Integer totalViolations,
            Integer totalReportsGenerated,
            Integer totalExposures,
            Integer validExposures,
            Integer totalErrors,
            Long version
    ) {
        this.bankId = bankId == null ? BankId.unknown() : bankId;
        this.periodStart = periodStart;
        this.overallScore = overallScore == null ? 0.0 : overallScore;
        this.dataQualityScore = dataQualityScore == null ? 0.0 : dataQualityScore;
        this.bcbsRulesScore = bcbsRulesScore == null ? 0.0 : bcbsRulesScore;
        this.completenessScore = completenessScore == null ? 0.0 : completenessScore;
        this.totalFilesProcessed = totalFilesProcessed == null ? 0 : totalFilesProcessed;
        this.totalViolations = totalViolations == null ? 0 : totalViolations;
        this.totalReportsGenerated = totalReportsGenerated == null ? 0 : totalReportsGenerated;
        this.totalExposures = totalExposures == null ? 0 : totalExposures;
        this.validExposures = validExposures == null ? 0 : validExposures;
        this.totalErrors = totalErrors == null ? 0 : totalErrors;
        this.version = version;
    }

    public static DashboardMetrics initialize(BankId bankId, java.time.LocalDate periodStart) {
        return new DashboardMetrics(bankId, periodStart, 0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0, 0, 0, null);
    }

    public void onDataQualityCompleted(Double dataQualityScore, Double completenessScore, Integer totalExposures, Integer validExposures, Integer totalErrors) {
        if (dataQualityScore != null) {
            this.dataQualityScore += dataQualityScore;
        }
        if (completenessScore != null) {
            this.completenessScore += completenessScore;
        }
        if (totalExposures != null) {
            this.totalExposures += totalExposures;
        }
        if (validExposures != null) {
            this.validExposures += validExposures;
        }
        if (totalErrors != null) {
            this.totalErrors += totalErrors;
        }
    }


    public void recalculateMonthToDate(List<ComplianceFile> files) {

        this.totalFilesProcessed += files.size();

        OptionalDouble avg = files.stream()
                .filter(f -> f.getScore() != null)
                .mapToDouble(ComplianceFile::getScore)
                .average();
        this.overallScore = avg.orElse(0.0);

        this.totalViolations += (int) files.stream().filter(f -> !f.isCompliant()).count();

        // Keep sub-scores as simplified derivations for now.
        //this.bcbsRulesScore = this.overallScore - 2.0;
    }

}
