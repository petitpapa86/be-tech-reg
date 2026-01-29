package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.dataquality.domain.model.reporting.StoredValidationResults;

import java.util.Optional;

/**
 * Application-facing reader for the stored detailed validation results file.
 *
 * <p>Implementation belongs to infrastructure (local filesystem, S3, etc.).</p>
 */
public interface StoredValidationResultsReader {

    Optional<StoredValidationResults> load(String detailsUri);
}
