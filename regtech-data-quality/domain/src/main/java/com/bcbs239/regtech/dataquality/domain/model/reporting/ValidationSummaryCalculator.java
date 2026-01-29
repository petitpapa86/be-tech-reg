package com.bcbs239.regtech.dataquality.domain.model.reporting;

import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationSummary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ValidationSummaryCalculator {

    static ValidationSummary calculate(
            int storedTotalExposures,
            int storedValidExposures,
            int storedTotalErrors,
            List<DetailedExposureResult> exposureResults,
            List<DetailedExposureResult.DetailedError> batchErrors,
            ValidationSummary fallback
    ) {
        int finalTotalExposures = storedTotalExposures > 0
                ? storedTotalExposures
                : (fallback != null ? fallback.totalExposures() : 0);

        int finalValidExposures = storedValidExposures > 0
                ? storedValidExposures
                : (int) exposureResults.stream().filter(DetailedExposureResult::isValid).count();

        int finalInvalidExposures = finalTotalExposures > 0 ? Math.max(0, finalTotalExposures - finalValidExposures) : 0;

        Map<String, Integer> errorsByCode = new HashMap<>();
        Map<QualityDimension, Integer> errorsByDimension = new HashMap<>();
        Map<ValidationError.ErrorSeverity, Integer> errorsBySeverity = new HashMap<>();

        // Exposure-level errors
        exposureResults.forEach(exposure -> {
            if (exposure.errors() == null) {
                return;
            }
            exposure.errors().forEach(err -> accumulateError(err, errorsByCode, errorsByDimension, errorsBySeverity));
        });

        // Batch-level errors
        batchErrors.forEach(err -> accumulateError(err, errorsByCode, errorsByDimension, errorsBySeverity));

        int finalTotalErrors = storedTotalErrors > 0
                ? storedTotalErrors
                : errorsByCode.values().stream().mapToInt(Integer::intValue).sum();

        double overallValidationRate = finalTotalExposures > 0
                ? ((double) finalValidExposures / (double) finalTotalExposures)
                : 0.0;

        return new ValidationSummary(
                finalTotalExposures,
                finalValidExposures,
                finalInvalidExposures,
                finalTotalErrors,
                errorsByDimension,
                errorsBySeverity,
                errorsByCode,
                overallValidationRate
        );
    }

    private static void accumulateError(
            DetailedExposureResult.DetailedError err,
            Map<String, Integer> errorsByCode,
            Map<QualityDimension, Integer> errorsByDimension,
            Map<ValidationError.ErrorSeverity, Integer> errorsBySeverity
    ) {
        if (err == null) {
            return;
        }

        if (err.ruleCode() != null && !err.ruleCode().isBlank()) {
            errorsByCode.merge(err.ruleCode(), 1, Integer::sum);
        }

        if (err.dimension() != null && !err.dimension().isBlank()) {
            try {
                QualityDimension dim = QualityDimension.valueOf(err.dimension().trim().toUpperCase());
                errorsByDimension.merge(dim, 1, Integer::sum);
            } catch (IllegalArgumentException ignored) {}
        }

        if (err.severity() != null && !err.severity().isBlank()) {
            try {
                ValidationError.ErrorSeverity sev = ValidationError.ErrorSeverity.valueOf(err.severity().trim().toUpperCase());
                errorsBySeverity.merge(sev, 1, Integer::sum);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
