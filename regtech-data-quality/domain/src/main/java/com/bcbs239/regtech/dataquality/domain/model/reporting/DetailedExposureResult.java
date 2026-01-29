package com.bcbs239.regtech.dataquality.domain.model.reporting;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Domain model for detailed exposure validation results.
 * Contains the specific rule violations for each exposure.
 */
public record DetailedExposureResult(
    @JsonProperty("isValid") boolean isValid,
    @JsonProperty("exposureId") String exposureId,
    @JsonProperty("errorCount") int errorCount,
    @JsonProperty("errors") List<DetailedError> errors
) {

    public record DetailedError(
        @JsonProperty("ruleCode") String ruleCode,
        @JsonProperty("severity") String severity,
        @JsonProperty("fieldName") String fieldName,
        @JsonProperty("message") String message,
        @JsonProperty("dimension") String dimension
    ) {}
}
