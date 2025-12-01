package com.bcbs239.regtech.riskcalculation.domain.services;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;

/**
 * Domain service interface for storing detailed calculation results.
 * Stores complete calculation data (summary + all calculated exposures) as JSON in S3/filesystem.
 */
public interface ICalculationResultsStorageService {
    
    /**
     * Store detailed calculation results as JSON and return the storage URI.
     * 
     * @param jsonContent The complete calculation results as JSON string
     * @param batchId The batch identifier
     * @param bankId The bank identifier
     * @return Result containing the FileStorageUri where the results were stored
     */
    Result<FileStorageUri> storeCalculationResults(String jsonContent, String batchId, String bankId);
    
    /**
     * Retrieve detailed calculation results from storage.
     * 
     * @param fileUri The URI where the results are stored
     * @return Result containing the JSON content
     */
    Result<String> retrieveCalculationResults(FileStorageUri fileUri);
    
    /**
     * Check if the storage service is healthy and accessible.
     * 
     * @return Result indicating service health status
     */
    Result<Boolean> checkServiceHealth();
}
