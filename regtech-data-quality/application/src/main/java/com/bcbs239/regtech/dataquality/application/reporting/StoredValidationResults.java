package com.bcbs239.regtech.dataquality.application.reporting;

import java.util.List;

/**
 * Snapshot of the stored detailed validation results file.
 *
 * <p>This is an application DTO used for presentation/reporting workflows.
 * It is loaded from infrastructure storage (local filesystem / S3).</p>
 */
public record StoredValidationResults(
    int totalExposures,
    int validExposures,
    int totalErrors,
    List<DetailedExposureResult> exposureResults,
    List<DetailedExposureResult.DetailedError> batchErrors
) {

    public StoredValidationResults {
        if (exposureResults == null) {
            exposureResults = List.of();
        }
        if (batchErrors == null) {
            batchErrors = List.of();
        }
    }
}
