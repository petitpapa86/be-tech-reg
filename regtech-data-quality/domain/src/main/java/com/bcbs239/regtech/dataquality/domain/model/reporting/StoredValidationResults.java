package com.bcbs239.regtech.dataquality.domain.model.reporting;

import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation.ActionPresentation;
import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation.DimensionDetailPresentation;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationSummary;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Snapshot of the stored detailed validation results file.
 *
 * <p>This is a Domain Model used for presentation/reporting workflows.
 * It is loaded from infrastructure storage (local filesystem / S3).</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StoredValidationResults(
    int totalExposures,
    int validExposures,
    int totalErrors,
    List<DetailedExposureResult> exposureResults,
    List<DetailedExposureResult.DetailedError> batchErrors,
    List<StoredRecommendation> recommendations
) {

    @JsonCreator
    public StoredValidationResults(
            @JsonProperty("totalExposures") int totalExposures,
            @JsonProperty("validExposures") int validExposures,
            @JsonProperty("totalErrors") int totalErrors,
            @JsonProperty("exposureResults") List<DetailedExposureResult> exposureResults,
            @JsonProperty("batchErrors") List<DetailedExposureResult.DetailedError> batchErrors,
            @JsonProperty("recommendations") List<StoredRecommendation> recommendations
    ) {
        this.totalExposures = totalExposures;
        this.validExposures = validExposures;
        this.totalErrors = totalErrors;
        this.exposureResults = exposureResults != null ? exposureResults : List.of();
        this.batchErrors = batchErrors != null ? batchErrors : List.of();
        this.recommendations = recommendations != null ? recommendations : List.of();
    }

    public List<DimensionDetailPresentation> getDimensionDetails() {
        return ValidationResultsErrorGrouper.groupErrors(exposureResults, batchErrors);
    }

    public ValidationSummary calculateSummary(ValidationSummary fallback) {
        return ValidationSummaryCalculator.calculate(
                totalExposures,
                validExposures,
                totalErrors,
                exposureResults,
                batchErrors,
                fallback
        );
    }

    public List<ActionPresentation> getActions() {
        if (recommendations == null) {
            return List.of();
        }

        return recommendations.stream()
                .map(this::mapRecommendation)
                .collect(Collectors.toList());
    }

    private ActionPresentation mapRecommendation(StoredRecommendation rec) {
        String title = rec.message() != null && !rec.message().isBlank()
                ? rec.message()
                : (rec.ruleId() != null ? rec.ruleId() : "Raccomandazione");

        String description = rec.actionItems() != null
                ? String.join("\n", rec.actionItems())
                : "";

        String priority = mapSeverityToPriority(rec.severity());
        String color = rec.color() != null ? rec.color() : "blue";

        return new ActionPresentation(
                title,
                description,
                "", // deadline not available in stored results
                priority,
                color
        );
    }

    private String mapSeverityToPriority(String severity) {
        if (severity == null) return "Media";
        return switch (severity.trim().toUpperCase()) {
            case "CRITICAL" -> "Critica";
            case "HIGH" -> "Alta";
            case "LOW" -> "Bassa";
            case "SUCCESS" -> "Info";
            default -> "Media";
        };
    }
}
