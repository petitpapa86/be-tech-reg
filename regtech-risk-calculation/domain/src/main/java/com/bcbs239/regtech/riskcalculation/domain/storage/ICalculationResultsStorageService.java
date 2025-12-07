package com.bcbs239.regtech.riskcalculation.domain.storage;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Domain service interface for storing and retrieving calculation results.
 * This interface defines the contract for file-based storage of calculation results.
 * 
 * Requirements: 1.3, 1.4, 4.3, 8.3
 */
public interface ICalculationResultsStorageService {
    
    /**
     * Stores complete calculation results to file storage.
     * Serializes the result to JSON and stores it using the file storage service.
     * 
     * Requirement 1.3: Use File_Storage_Service to store the JSON file
     * Requirement 1.4: Return Calculation_Results_URI (S3 URI or filesystem path)
     * 
     * @param jsonContent The serialized JSON content to store
     * @param batchId The batch identifier for file naming
     * @return Result containing the storage URI or error
     */
    Result<String> storeCalculationResults(String jsonContent, String batchId);
    
    /**
     * Retrieves calculation results JSON by batch ID.
     * Looks up the storage URI from the database and downloads the JSON file.
     * 
     * Requirement 4.3: Download and return complete calculation results
     * Requirement 8.3: Retrieve JSON files by batch_id for historical data access
     * 
     * @param batchId The batch identifier
     * @return Result containing the JSON content or error
     */
    Result<String> retrieveCalculationResultsJson(String batchId);
    
    /**
     * Retrieves raw calculation results JSON as JsonNode by batch ID.
     * Looks up the storage URI from the database and downloads/parses the JSON file.
     * Useful for downstream modules that want to work with JsonNode.
     * 
     * Requirement 4.3: Download complete calculation results
     * Requirement 8.3: Retrieve JSON files by batch_id for historical data access
     * 
     * @param batchId The batch identifier
     * @return Result containing the raw JSON node or error
     */
    Result<JsonNode> retrieveCalculationResultsRaw(String batchId);
}
