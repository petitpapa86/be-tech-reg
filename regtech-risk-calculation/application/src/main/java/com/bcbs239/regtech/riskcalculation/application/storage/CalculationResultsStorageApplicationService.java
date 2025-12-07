package com.bcbs239.regtech.riskcalculation.application.storage;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.calculation.CalculationResultsDeserializationException;
import com.bcbs239.regtech.riskcalculation.application.calculation.CalculationResultsJsonSerializer;
import com.bcbs239.regtech.riskcalculation.application.calculation.CalculationResultsSerializationException;
import com.bcbs239.regtech.riskcalculation.application.calculation.RiskCalculationResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service that combines serialization with storage.
 * This service acts as a facade that orchestrates:
 * 1. Serialization of RiskCalculationResult to JSON (application concern)
 * 2. Storage of JSON to files (domain concern via domain service)
 * 
 * Requirements: 1.3, 1.4, 4.3, 8.3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalculationResultsStorageApplicationService implements ICalculationResultsStorageService {
    
    private final CalculationResultsJsonSerializer jsonSerializer;
    private final com.bcbs239.regtech.riskcalculation.domain.storage.ICalculationResultsStorageService domainStorageService;
    
    /**
     * Stores complete calculation results to file storage.
     * Serializes the result to JSON and stores it using the domain storage service.
     * 
     * Requirement 1.3: Use File_Storage_Service to store the JSON file
     * Requirement 1.4: Return Calculation_Results_URI (S3 URI or filesystem path)
     * 
     * @param result The risk calculation result to store
     * @return Result containing the storage URI or error
     */
    @Override
    public Result<String> storeCalculationResults(RiskCalculationResult result) {
        String batchId = result.batchId();
        
        try {
            log.debug("Serializing calculation results [batchId:{}]", batchId);
            
            // Serialize to JSON (application layer responsibility)
            String jsonContent;
            try {
                jsonContent = jsonSerializer.serialize(result);
            } catch (CalculationResultsSerializationException e) {
                log.error("Failed to serialize calculation results [batchId:{}]", batchId, e);
                return Result.failure(ErrorDetail.of(
                    "SERIALIZATION_ERROR",
                    ErrorType.SYSTEM_ERROR,
                    "Failed to serialize calculation results: " + e.getMessage(),
                    "calculation.results.serialization.error"
                ));
            }
            
            // Store JSON (domain layer responsibility)
            return domainStorageService.storeCalculationResults(jsonContent, batchId);
            
        } catch (Exception e) {
            log.error("Unexpected error in application storage service [batchId:{}]", batchId, e);
            return Result.failure(ErrorDetail.of(
                "STORAGE_UNEXPECTED_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error storing calculation results: " + e.getMessage(),
                "calculation.results.storage.unexpected.error"
            ));
        }
    }
    
    /**
     * Retrieves calculation results by batch ID.
     * Downloads the JSON file and deserializes it to RiskCalculationResult.
     * 
     * Requirement 4.3: Download and return complete calculation results
     * Requirement 8.3: Retrieve JSON files by batch_id for historical data access
     * 
     * @param batchId The batch identifier
     * @return Result containing the deserialized calculation result or error
     */
    @Override
    public Result<RiskCalculationResult> retrieveCalculationResults(String batchId) {
        try {
            log.debug("Retrieving calculation results [batchId:{}]", batchId);
            
            // Retrieve JSON (domain layer responsibility)
            Result<String> jsonResult = domainStorageService.retrieveCalculationResultsJson(batchId);
            
            if (jsonResult.isFailure()) {
                return Result.failure(jsonResult.errors());
            }
            
            String jsonContent = jsonResult.value();
            
            // Deserialize JSON (application layer responsibility)
            try {
                RiskCalculationResult result = jsonSerializer.deserialize(jsonContent);
                return Result.success(result);
                
            } catch (CalculationResultsDeserializationException e) {
                log.error("Failed to deserialize calculation results [batchId:{}]", batchId, e);
                return Result.failure(ErrorDetail.of(
                    "DESERIALIZATION_ERROR",
                    ErrorType.SYSTEM_ERROR,
                    "Failed to deserialize calculation results: " + e.getMessage(),
                    "calculation.results.deserialization.error"
                ));
            }
            
        } catch (Exception e) {
            log.error("Unexpected error retrieving calculation results [batchId:{}]", batchId, e);
            return Result.failure(ErrorDetail.of(
                "RETRIEVAL_UNEXPECTED_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error retrieving calculation results: " + e.getMessage(),
                "calculation.results.retrieval.unexpected.error"
            ));
        }
    }
    
    /**
     * Retrieves raw calculation results JSON by batch ID.
     * Delegates directly to domain storage service.
     * 
     * Requirement 4.3: Download complete calculation results
     * Requirement 8.3: Retrieve JSON files by batch_id for historical data access
     * 
     * @param batchId The batch identifier
     * @return Result containing the raw JSON node or error
     */
    @Override
    public Result<JsonNode> retrieveCalculationResultsRaw(String batchId) {
        return domainStorageService.retrieveCalculationResultsRaw(batchId);
    }
}
