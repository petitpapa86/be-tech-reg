package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.dataquality.application.scoring.QualityScoresDto;
import com.bcbs239.regtech.dataquality.application.validation.ValidationSummaryDto;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.report.QualityStatus;

import java.time.Instant;

/**
 * DTO for complete quality report with score breakdown.
 * Provides a serializable representation of quality reports for API responses.
 */
public record QualityReportDto(
    String reportId,
    String batchId,
    String bankId,
    String status,
    QualityScoresDto scores,
    ValidationSummaryDto validationSummary,
    String detailsUri,
    String errorMessage,
    Instant createdAt,
    Instant updatedAt
) {
    
    /**
     * Creates a DTO from domain QualityReport.
     */
    public static QualityReportDto fromDomain(QualityReport report) {
        if (report == null) {
            return null;
        }
        
        return new QualityReportDto(
            report.getReportId().value(),
            report.getBatchId().value(),
            report.getBankId().value(),
            report.getStatus().name(),
            QualityScoresDto.fromDomain(report.getScores()),
            ValidationSummaryDto.fromDomain(report.getValidationSummary()),
            report.getDetailsReference() != null ? report.getDetailsReference().uri() : null,
            report.getErrorMessage(),
            report.getCreatedAt(),
            report.getUpdatedAt()
        );
    }
    
    /**
     * Gets the status as an enum.
     */
    public QualityStatus getStatusEnum() {
        return QualityStatus.valueOf(status);
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
     * Checks if the report is in progress.
     */
    public boolean isInProgress() {
        return "IN_PROGRESS".equals(status);
    }
    
    /**
     * Checks if the report is pending.
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }
    
    /**
     * Checks if the report is in a terminal state.
     */
    public boolean isTerminal() {
        return isCompleted() || isFailed();
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
     * Gets the processing duration in seconds.
     */
    public double getProcessingDurationSeconds() {
        return getProcessingDurationMs() / 1000.0;
    }
    
    /**
     * Checks if the quality meets compliance standards.
     */
    public boolean isCompliant() {
        return scores != null && scores.isCompliant();
    }
    
    /**
     * Checks if the quality requires immediate attention.
     */
    public boolean requiresAttention() {
        return scores != null && scores.requiresAttention();
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
     * Gets the validation rate percentage.
     */
    public double getValidationRate() {
        return validationSummary != null ? validationSummary.validationRate() : 0.0;
    }
    
    /**
     * Gets the total number of exposures processed.
     */
    public int getTotalExposures() {
        return validationSummary != null ? validationSummary.totalExposures() : 0;
    }
    
    /**
     * Gets the number of valid exposures.
     */
    public int getValidExposures() {
        return validationSummary != null ? validationSummary.validExposures() : 0;
    }
    
    /**
     * Gets the total number of validation errors.
     */
    public int getTotalErrors() {
        return validationSummary != null ? validationSummary.totalErrors() : 0;
    }
    
    /**
     * Checks if detailed results are available in S3.
     */
    public boolean hasDetailedResults() {
        return detailsUri != null && !detailsUri.trim().isEmpty();
    }
    
    /**
     * Gets a human-readable status description.
     */
    public String getStatusDescription() {
        return switch (status) {
            case "PENDING" -> "Validation is queued and waiting to start";
            case "IN_PROGRESS" -> "Validation is currently running";
            case "COMPLETED" -> "Validation completed successfully";
            case "FAILED" -> "Validation failed: " + (errorMessage != null ? errorMessage : "Unknown error");
            default -> "Unknown status: " + status;
        };
    }
    
    /**
     * Gets a summary of the quality report.
     */
    public String getSummary() {
        if (isFailed()) {
            return "Validation failed";
        }
        
        if (!isCompleted()) {
            return "Validation " + status.toLowerCase().replace("_", " ");
        }
        
        if (scores == null) {
            return "Validation completed (no scores available)";
        }
        
        return String.format("Grade %s (%.1f%%) - %s exposures processed",
            scores.getGradeDisplay(),
            scores.overallScore(),
            getTotalExposures()
        );
    }
}

