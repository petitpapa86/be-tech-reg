package com.bcbs239.regtech.riskcalculation.application.storage;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.calculation.RiskCalculationResult;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Application service interface for storing and retrieving calculation results.
 * This interface orchestrates JSON serialization and file storage operations.
 * 
 * Requirements: 1.3, 1.4, 4.3, 8.3
 */
public interface ICalculationResultsStorage {
    
    /**
     * Stores complete calculation results to file storage.
     * Serializes the result to JSON and stores it using the file storage service.
     * 
     * Requirement 1.3: Use File_Storage_Service to store the JSON file
     * Requirement 1.4: Return Calculation_Results_URI (S3 URI or filesystem path)
     * 
     * @param result The risk calculation result to store
     * @return Result containing the storage URI or error
     */
    Result<String> storeCalculationResults(RiskCalculationResult result);
    
    /**
     * Retrieves calculation results by batch ID.
     * Looks up the storage URI from the database and downloads/deserializes the JSON file.
     * 
     * Requirement 4.3: Download and return complete calculation results
     * Requirement 8.3: Retrieve JSON files by batch_id for historical data access
     * 
     * @param batchId The batch identifier
     * @return Result containing the deserialized calculation result or error
     */
    Result<RiskCalculationResult> retrieveCalculationResults(String batchId);
    
    /**
     * Retrieves raw calculation results JSON by batch ID.
     * Looks up the storage URI from the database and downloads the JSON file without deserialization.
     * Useful for downstream modules that want to parse the JSON themselves.
     * 
     * Requirement 4.3: Download complete calculation results
     * Requirement 8.3: Retrieve JSON files by batch_id for historical data access
     * 
     * @param batchId The batch identifier
     * @return Result containing the raw JSON node or error
     */
    Result<JsonNode> retrieveCalculationResultsRaw(String batchId);
}
