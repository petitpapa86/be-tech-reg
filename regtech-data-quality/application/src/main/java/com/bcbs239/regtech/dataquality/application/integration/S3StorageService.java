package com.bcbs239.regtech.dataquality.application.integration;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;

import java.util.List;

/**
 * Service interface for S3 operations related to quality validation.
 * Handles downloading exposure data and storing detailed validation results.
 */
public interface S3StorageService {
    
    /**
     * Downloads and parses exposure data from S3.
     * Uses streaming JSON parsing to handle large files efficiently.
     * 
     * @param s3Uri The S3 URI containing the exposure data
     * @return List of parsed exposure records
     */
    Result<List<ExposureRecord>> downloadExposures(String s3Uri);
    
    /**
     * Downloads and parses exposure data with expected count validation.
     * 
     * @param s3Uri The S3 URI containing the exposure data
     * @param expectedCount Expected number of exposures for validation
     * @return List of parsed exposure records
     */
    Result<List<ExposureRecord>> downloadExposures(String s3Uri, int expectedCount);
    
    /**
     * Stores detailed validation results in S3 with encryption.
     * 
     * @param batchId The batch ID for naming the results file
     * @param validationResult The detailed validation results to store
     * @return S3Reference pointing to the stored results
     */
    Result<S3Reference> storeDetailedResults(BatchId batchId, ValidationResult validationResult);
    
    /**
     * Stores detailed validation results with custom metadata.
     * 
     * @param batchId The batch ID for naming the results file
     * @param validationResult The detailed validation results to store
     * @param metadata Additional metadata to include with the stored object
     * @return S3Reference pointing to the stored results
     */
    Result<S3Reference> storeDetailedResults(
        BatchId batchId, 
        ValidationResult validationResult, 
        java.util.Map<String, String> metadata
    );
    
    /**
     * Checks if an S3 object exists at the given URI.
     * 
     * @param s3Uri The S3 URI to check
     * @return true if the object exists, false otherwise
     */
    Result<Boolean> objectExists(String s3Uri);
    
    /**
     * Gets the size of an S3 object in bytes.
     * 
     * @param s3Uri The S3 URI to check
     * @return Size in bytes
     */
    Result<Long> getObjectSize(String s3Uri);
}

