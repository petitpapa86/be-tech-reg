package com.bcbs239.regtech.riskcalculation.domain.persistence;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankInfo;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Repository interface for batch metadata persistence.
 * Defines operations for creating and managing batch records.
 */
public interface BatchRepository {
    
    /**
     * Creates a new batch record.
     * 
     * @param batchId the batch identifier
     * @param bankInfo bank information
     * @param reportDate the report date
     * @param totalExposures total number of exposures
     * @param ingestedAt when the batch was ingested
     */
    void createBatch(String batchId, BankInfo bankInfo, LocalDate reportDate, 
                     int totalExposures, Instant ingestedAt);
    
    /**
     * Checks if a batch exists.
     * 
     * @param batchId the batch identifier
     * @return true if batch exists, false otherwise
     */
    boolean exists(String batchId);
    
    /**
     * Updates batch status.
     * 
     * @param batchId the batch identifier
     * @param status the new status
     */
    void updateStatus(String batchId, String status);
    
    /**
     * Marks batch as processed.
     * 
     * @param batchId the batch identifier
     * @param processedAt when processing completed
     */
    void markAsProcessed(String batchId, Instant processedAt);
    
    /**
     * Updates the calculation results URI for a batch.
     * 
     * @param batchId the batch identifier
     * @param uri the calculation results URI (S3 URI or filesystem path)
     */
    void updateCalculationResultsUri(String batchId, String uri);
    
    /**
     * Retrieves the calculation results URI for a batch.
     * 
     * @param batchId the batch identifier
     * @return Optional containing the URI if present, empty otherwise
     */
    java.util.Optional<String> getCalculationResultsUri(String batchId);
    
    /**
     * Marks batch as completed with results URI.
     * Updates status to COMPLETED, sets processed timestamp, and stores results URI.
     * 
     * @param batchId the batch identifier
     * @param resultsUri the calculation results URI
     * @param processedAt when processing completed
     */
    void markAsCompleted(String batchId, String resultsUri, Instant processedAt);
}
