package com.bcbs239.regtech.dataquality.domain.model.presentation;

import java.util.List;
import java.util.Map;

/**
 * Frontend presentation model for Quality Report
 *
 * <p>This is NOT a domain entity - it's a read-only presentation model
 * created by {@code QualityReport.toFrontendPresentation(...)}.</p>
 *
 * <p>Package: domain/model/presentation (stays in domain layer)
 * Purpose: Presentation concern separated from domain state</p>
 */
public record QualityReportPresentation(
    // Basic info
    String fileName,
    String fileSize,
    int totalRecords,
    double complianceScore,
    double dataQuality,
    int criticalViolations,
    int largeExposures,

    // Arrays
    List<ViolationPresentation> violations,
    List<ExposurePresentation> topExposures,
    List<ActionPresentation> recommendedActions
) {

    public record ViolationPresentation(
        String title,
        String severity,
        String description,
        Map<String, String> details
    ) {}

    public record ExposurePresentation(
        String counterparty,
        String exposure,
        String percent,
        String status
    ) {}

    public record ActionPresentation(
        String title,
        String description,
        String deadline,
        String priority,
        String color
    ) {}
}
