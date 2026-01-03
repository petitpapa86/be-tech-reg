package com.bcbs239.regtech.metrics.domain;

import java.util.List;
import java.util.OptionalDouble;

/**
 * Domain aggregate for dashboard metrics.
 * Business behavior lives here; persistence concerns live in infrastructure entities.
 */
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

    protected DashboardMetrics() {
    }

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
            this.dataQualityScore = dataQualityScore;
        }
        if (completenessScore != null) {
            this.completenessScore = completenessScore;
        }
        if (totalExposures != null) {
            this.totalExposures = totalExposures;
        }
        if (validExposures != null) {
            this.validExposures = validExposures;
        }
        if (totalErrors != null) {
            this.totalErrors = totalErrors;
        }
    }

    // ASK WHAT METRICS CAN DO: Update from file validation
    public void onFileValidated(String filename, double scorePercentage, int violations) {
        this.totalFilesProcessed++;
        this.totalViolations += violations;

        // Recalculate weighted average
        // Simplified - real version would be more sophisticated
        double weight = 1.0 / this.totalFilesProcessed;
        this.overallScore = this.overallScore * (1 - weight) + scorePercentage * weight;

        // Update sub-scores (simplified)
        this.dataQualityScore = this.overallScore + 5.0;
        this.bcbsRulesScore = this.overallScore - 2.0;
        this.completenessScore = this.overallScore + 7.0;
    }

    // ASK WHAT METRICS CAN DO: Increment reports
    public void onReportGenerated() {
        this.totalReportsGenerated++;
    }

    // ASK WHAT METRICS CAN DO: Recalculate month-to-date
    public void recalculateMonthToDate(List<ComplianceFile> files) {
        if (files == null) {
            return;
        }

        this.totalFilesProcessed = files.size();

        OptionalDouble avg = files.stream()
                .filter(f -> f.getScore() != null)
                .mapToDouble(ComplianceFile::getScore)
                .average();
        this.overallScore = avg.orElse(0.0);

        this.totalViolations = (int) files.stream().filter(f -> !f.isCompliant()).count();

        // Keep sub-scores as simplified derivations for now.
        this.dataQualityScore = this.overallScore + 5.0;
        this.bcbsRulesScore = this.overallScore - 2.0;
        this.completenessScore = this.overallScore + 7.0;
    }

    public BankId getBankId() {
        return bankId;
    }

    public java.time.LocalDate getPeriodStart() {
        return periodStart;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public Double getDataQualityScore() {
        return dataQualityScore;
    }

    public Double getBcbsRulesScore() {
        return bcbsRulesScore;
    }

    public Double getCompletenessScore() {
        return completenessScore;
    }

    public Integer getTotalFilesProcessed() {
        return totalFilesProcessed;
    }

    public Integer getTotalViolations() {
        return totalViolations;
    }

    public Integer getTotalReportsGenerated() {
        return totalReportsGenerated;
    }

    public Integer getTotalExposures() {
        return totalExposures;
    }

    public Integer getValidExposures() {
        return validExposures;
    }

    public Integer getTotalErrors() {
        return totalErrors;
    }

    public Long getVersion() {
        return version;
    }
}
