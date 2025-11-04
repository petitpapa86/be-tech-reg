package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.dataquality.application.scoring.QualityScoresDto;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;

import java.time.Instant;

/**
 * DTO for quality report summary (lightweight version for lists and trends).
 * Contains essential information without detailed validation results.
 */
public record QualityReportSummaryDto(
    String reportId,
    String batchId,
    String bankId,
    String status,
    QualityScoresDto scores,
    int totalExposures,
    int validExposures,
    int totalErrors,
    double validationRate,
    Instant createdAt,
    Instant updatedAt
) {
    
    /**
     * Creates a summary DTO from domain QualityReport.
     */
    public static QualityReportSummaryDto fromDomain(QualityReport report) {
        if (report == null) {
            return null;
        }
        
        int totalExposures = report.getValidationSummary() != null 
            ? report.getValidationSummary().totalExposures() : 0;
        int validExposures = report.getValidationSummary() != null 
            ? report.getValidationSummary().validExposures() : 0;
        int totalErrors = report.getValidationSummary() != null 
            ? report.getValidationSummary().totalErrors() : 0;
        
        double validationRate = totalExposures > 0 
            ? (double) validExposures / totalExposures * 100.0 : 0.0;
        
        return new QualityReportSummaryDto(
            report.getReportId().value(),
            report.getBatchId().value(),
            report.getBankId().value(),
            report.getStatus().name(),
            QualityScoresDto.fromDomain(report.getScores()),
            totalExposures,
            validExposures,
            totalErrors,
            validationRate,
            report.getCreatedAt(),
            report.getUpdatedAt()
        );
    }
    
    /**
     * Checks if the report is completed.
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
    
    /**
     * Checks if the report is failed.
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }
    
    /**
     * Checks if the quality meets compliance standards.
     */
    public boolean isCompliant() {
        return scores != null && scores.isCompliant();
    }
    
    /**
     * Gets the overall quality score.
     */
    public double getOverallScore() {
        return scores != null ? scores.overallScore() : 0.0;
    }
    
    /**
     * Gets the quality grade.
     */
    public String getGrade() {
        return scores != null ? scores.grade() : "UNKNOWN";
    }
    
    /**
     * Gets the validation rate as a percentage string.
     */
    public String getValidationRatePercentage() {
        return String.format("%.1f%%", validationRate);
    }
    
    /**
     * Gets the processing duration in milliseconds.
     */
    public long getProcessingDurationMs() {
        if (createdAt == null || updatedAt == null) {
            return 0;
        }
        return updatedAt.toEpochMilli() - createdAt.toEpochMilli();
    }
    
    /**
     * Gets a brief summary of the report.
     */
    public String getBriefSummary() {
        if (isFailed()) {
            return "Failed";
        }
        
        if (!isCompleted()) {
            return status.replace("_", " ");
        }
        
        if (scores == null) {
            return "Completed";
        }
        
        return String.format("%s (%.1f%%)", scores.getGradeDisplay(), scores.overallScore());
    }
}