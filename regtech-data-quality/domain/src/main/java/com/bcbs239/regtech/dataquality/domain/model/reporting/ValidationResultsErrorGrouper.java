package com.bcbs239.regtech.dataquality.domain.model.reporting;

import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation.DimensionDetailPresentation;
import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation.GroupedErrorPresentation;
import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class ValidationResultsErrorGrouper {

    static List<DimensionDetailPresentation> groupErrors(
            List<DetailedExposureResult> exposureResults,
            List<DetailedExposureResult.DetailedError> batchErrors
    ) {
        Map<QualityDimension, Map<String, ErrorAggregator>> groupedErrors = new EnumMap<>(QualityDimension.class);

        // Process exposure-level errors
        if (exposureResults != null) {
            for (DetailedExposureResult exposure : exposureResults) {
                if (exposure.errors() != null) {
                    for (DetailedExposureResult.DetailedError error : exposure.errors()) {
                        processError(error, exposure.exposureId(), groupedErrors);
                    }
                }
            }
        }

        // Process batch-level errors
        if (batchErrors != null) {
            for (DetailedExposureResult.DetailedError error : batchErrors) {
                processError(error, null, groupedErrors);
            }
        }

        List<DimensionDetailPresentation> result = new ArrayList<>();
        for (Map.Entry<QualityDimension, Map<String, ErrorAggregator>> entry : groupedErrors.entrySet()) {
            QualityDimension dim = entry.getKey();
            List<GroupedErrorPresentation> errors = entry.getValue().values().stream()
                    .map(agg -> agg.toPresentation(ValidationResultsErrorGrouper::mapSeverityToColor))
                    .collect(Collectors.toList());

            result.add(new DimensionDetailPresentation(
                    dim.name(),
                    dim.getItalianName(),
                    errors
            ));
        }

        return result;
    }

    private static void processError(
            DetailedExposureResult.DetailedError error,
            String exposureId,
            Map<QualityDimension, Map<String, ErrorAggregator>> groupedErrors
    ) {
        if (error.dimension() == null || error.dimension().isBlank()) {
            return;
        }

        QualityDimension dim;
        try {
            dim = QualityDimension.valueOf(error.dimension().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return; // Skip unknown dimensions
        }

        groupedErrors.putIfAbsent(dim, new HashMap<>());
        Map<String, ErrorAggregator> dimensionMap = groupedErrors.get(dim);

        String ruleCode = error.ruleCode() != null ? error.ruleCode() : "UNKNOWN";

        ErrorAggregator aggregator = dimensionMap.computeIfAbsent(ruleCode, k -> new ErrorAggregator(error));
        aggregator.addInstance(exposureId);
    }

    private static String mapSeverityToColor(String severity) {
        if (severity == null) return "gray";
        return switch (severity.trim().toUpperCase()) {
            case "CRITICAL" -> "red";
            case "HIGH" -> "orange";
            case "MEDIUM" -> "yellow";
            case "LOW" -> "blue";
            case "SUCCESS" -> "green";
            default -> "gray";
        };
    }

    private static class ErrorAggregator {
        private final String code;
        private final String title;
        private final String message;
        private final String severity;
        private int count = 0;
        private final List<String> affectedRecords = new ArrayList<>();

        ErrorAggregator(DetailedExposureResult.DetailedError error) {
            this.code = error.ruleCode() != null ? error.ruleCode() : "UNKNOWN";
            this.message = error.message();
            // Use message as title if available, otherwise code
            this.title = message != null && !message.isBlank() ? message : code;
            this.severity = error.severity();
        }

        void addInstance(String exposureId) {
            this.count++;
            if (exposureId != null) {
                this.affectedRecords.add(exposureId);
            }
        }

        GroupedErrorPresentation toPresentation(Function<String, String> colorMapper) {
            return new GroupedErrorPresentation(
                    code,
                    title,
                    message,
                    severity,
                    colorMapper.apply(severity),
                    count,
                    affectedRecords
            );
        }
    }
}
