package com.bcbs239.regtech.dataquality.application.reporting;

/**
 * Application-facing reader for the stored detailed validation results file.
 *
 * <p>Implementation belongs to infrastructure (local filesystem, S3, etc.).</p>
 */
public interface StoredValidationResultsReader {

    StoredValidationResults load(String detailsUri);
}
