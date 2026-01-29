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
    
    // Grouped Stats
    ValidationStats validationStats,
    
    // Grouped Scores
    ScoreSummary scores,
    
    // Grouped Metrics
    KeyMetrics metrics,

    // Grouped Badges
    ReportBadges badges,

    // Arrays
    List<DimensionPresentation> dimensionScores,
    List<ViolationPresentation> violations,
    List<ExposurePresentation> topExposures,
    List<ActionPresentation> recommendedActions,
    List<DimensionDetailPresentation> dimensionDetails
) {

    public record ValidationStats(
        int totalRecords,
        Integer validRecords,
        Double validationRate
    ) {}

    public record ScoreSummary(
        double complianceScore,
        double dataQualityScore
    ) {}

    public record KeyMetrics(
        int criticalViolations,
        int largeExposuresCount
    ) {}

    public record ReportBadges(
        Badge overall,
        Badge compliance,
        Badge error,
        IconBadge validationRate
    ) {}

    public record Badge(
        String label,
        String color
    ) {}

    public record IconBadge(
        String label,
        String color,
        String icon
    ) {}

    public record DimensionPresentation(
        String name,
        String label,
        double score,
        String color,
        String badge,
        String description
    ) {}

    public record ViolationPresentation(
        String title,
        String severity,
        String description,
        Map<String, String> details,
        String color
    ) {}

    public record ExposurePresentation(
        String counterparty,
        String exposure,
        String percent,
        String status,
        String color,
        List<String> errors
    ) {}

    public record ActionPresentation(
        String title,
        String description,
        String deadline,
        String priority,
        String color
    ) {}

    public record DimensionDetailPresentation(
        String dimensionName,
        String label,
        List<GroupedErrorPresentation> errors
    ) {}

    public record GroupedErrorPresentation(
        String code,
        String title,
        String message,
        String severity,
        String color,
        int count,
        List<String> affectedRecords
    ) {}
}
