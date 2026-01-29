package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.dataquality.application.scoring.QualityScoresDto;
import com.bcbs239.regtech.dataquality.application.validation.ValidationSummaryDto;
import com.bcbs239.regtech.dataquality.domain.model.reporting.DetailedExposureResult;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * Enhanced DTO for quality report with detailed exposure results.
 * Provides complete validation details for debugging and fixing issues.
 */
public record DetailedQualityReportDto(
    @JsonProperty("reportId") String reportId,
    @JsonProperty("batchId") String batchId,
    @JsonProperty("bankId") String bankId,
    @JsonProperty("status") String status,
    @JsonProperty("scores") QualityScoresDto scores,
    @JsonProperty("validationSummary") ValidationSummaryDto validationSummary,
    @JsonProperty("exposureResults") List<DetailedExposureResult> exposureResults,
    @JsonProperty("detailsUri") String detailsUri,
    @JsonProperty("errorMessage") String errorMessage,
    @JsonProperty("createdAt") Instant createdAt,
    @JsonProperty("updatedAt") Instant updatedAt
) {

    /**
     * Creates a detailed DTO from the basic QualityReportDto and exposure results.
     */
    public static DetailedQualityReportDto fromDto(
        QualityReportDto basicReport,
        List<DetailedExposureResult> exposureResults
    ) {
        return new DetailedQualityReportDto(
            basicReport.reportId(),
            basicReport.batchId(),
            basicReport.bankId(),
            basicReport.status(),
            basicReport.scores(),
            basicReport.validationSummary(),
            exposureResults,
            basicReport.detailsUri(),
            basicReport.errorMessage(),
            basicReport.createdAt(),
            basicReport.updatedAt()
        );
    }
}