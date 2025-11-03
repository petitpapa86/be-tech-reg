package com.bcbs239.regtech.ingestion.domain.batch;

/**
 * Enumeration representing the status of an ingestion batch.
 */
public enum BatchStatus {
    /**
     * File has been uploaded and initial validation passed.
     */
    UPLOADED,
    
    /**
     * File is being parsed and validated.
     */
    PARSING,
    
    /**
     * File has been successfully parsed and validated.
     */
    VALIDATED,
    
    /**
     * File is being stored in S3.
     */
    STORING,
    
    /**
     * Batch processing has completed successfully.
     */
    COMPLETED,
    
    /**
     * Batch processing has failed.
     */
    FAILED
}