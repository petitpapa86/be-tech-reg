package com.bcbs239.regtech.riskcalculation.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.calculation.CalculationResultsDeserializationException;
import com.bcbs239.regtech.riskcalculation.application.calculation.CalculationResultsJsonSerializer;
import com.bcbs239.regtech.riskcalculation.application.calculation.CalculationResultsSerializationException;
import com.bcbs239.regtech.riskcalculation.application.calculation.RiskCalculationResult;
import com.bcbs239.regtech.riskcalculation.application.storage.ICalculationResultsStorageService;
import com.bcbs239.regtech.riskcalculation.domain.persistence.BatchRepository;
import com.bcbs239.regtech.riskcalculation.domain.services.IFileStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Implementation of calculation results storage service.
 * Orchestrates JSON serialization, file storage, and URI management.
 * 
 * Requirements: 1.3, 1.4, 4.3, 8.3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalculationResultsStorageServiceImpl implements ICalculationResultsStorageService {
    
    private final CalculationResultsJsonSerializer jsonSerializer;
    private final IFileStorageService fileStorageService;
    private final BatchRepository batchRepository;
    private final ObjectMapper objectMapper;
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * Stores complete calculation results to file storage.
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
            log.info("Storing calculation results [batchId:{}]", batchId);
            
            // Generate file name: risk_calc_{batchId}_{timestamp}.json
            String fileName = generateFileName(batchId, result.ingestedAt());
            
            // Serialize to JSON
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
            
            // Store file using file storage service
            Result<String> storageResult = fileStorageService.storeFile(fileName, jsonContent);
            
            if (storageResult.isFailure()) {
                log.error("Failed to store calculation results file [batchId:{},fileName:{}]", 
                    batchId, fileName);
                return storageResult;
            }
            
            String storageUri = storageResult.value();
            
            log.info("Successfully stored calculation results [batchId:{},fileName:{},uri:{}]", 
                batchId, fileName, storageUri);
            
            return Result.success(storageUri);
            
        } catch (Exception e) {
            log.error("Unexpected error storing calculation results [batchId:{}]", batchId, e);
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
            log.info("Retrieving calculation results [batchId:{}]", batchId);
            
            // Look up storage URI from database
            Optional<String> uriOptional = batchRepository.getCalculationResultsUri(batchId);
            
            if (uriOptional.isEmpty()) {
                log.error("No calculation results URI found for batch [batchId:{}]", batchId);
                return Result.failure(ErrorDetail.of(
                    "URI_NOT_FOUND",
                    ErrorType.NOT_FOUND,
                    "No calculation results URI found for batch: " + batchId,
                    "calculation.results.uri.not.found"
                ));
            }
            
            String storageUri = uriOptional.get();
            
            // Download file from storage
            Result<String> downloadResult = fileStorageService.retrieveFile(storageUri);
            
            if (downloadResult.isFailure()) {
                log.error("Failed to download calculation results file [batchId:{},uri:{}]", 
                    batchId, storageUri);
                return Result.failure(downloadResult.errors());
            }
            
            String jsonContent = downloadResult.value();
            
            // Deserialize JSON to RiskCalculationResult
            try {
                RiskCalculationResult result = jsonSerializer.deserialize(jsonContent);
                
                log.info("Successfully retrieved calculation results [batchId:{}]", batchId);
                
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
     * 
     * Requirement 4.3: Download complete calculation results
     * Requirement 8.3: Retrieve JSON files by batch_id for historical data access
     * 
     * @param batchId The batch identifier
     * @return Result containing the raw JSON node or error
     */
    @Override
    public Result<JsonNode> retrieveCalculationResultsRaw(String batchId) {
        try {
            log.info("Retrieving raw calculation results JSON [batchId:{}]", batchId);
            
            // Look up storage URI from database
            Optional<String> uriOptional = batchRepository.getCalculationResultsUri(batchId);
            
            if (uriOptional.isEmpty()) {
                log.error("No calculation results URI found for batch [batchId:{}]", batchId);
                return Result.failure(ErrorDetail.of(
                    "URI_NOT_FOUND",
                    ErrorType.NOT_FOUND,
                    "No calculation results URI found for batch: " + batchId,
                    "calculation.results.uri.not.found"
                ));
            }
            
            String storageUri = uriOptional.get();
            
            // Download file from storage
            Result<String> downloadResult = fileStorageService.retrieveFile(storageUri);
            
            if (downloadResult.isFailure()) {
                log.error("Failed to download calculation results file [batchId:{},uri:{}]", 
                    batchId, storageUri);
                return Result.failure(downloadResult.errors());
            }
            
            String jsonContent = downloadResult.value();
            
            // Parse JSON to JsonNode
            try {
                JsonNode jsonNode = objectMapper.readTree(jsonContent);
                
                log.info("Successfully retrieved raw calculation results JSON [batchId:{}]", batchId);
                
                return Result.success(jsonNode);
                
            } catch (Exception e) {
                log.error("Failed to parse calculation results JSON [batchId:{}]", batchId, e);
                return Result.failure(ErrorDetail.of(
                    "JSON_PARSE_ERROR",
                    ErrorType.SYSTEM_ERROR,
                    "Failed to parse calculation results JSON: " + e.getMessage(),
                    "calculation.results.json.parse.error"
                ));
            }
            
        } catch (Exception e) {
            log.error("Unexpected error retrieving raw calculation results [batchId:{}]", batchId, e);
            return Result.failure(ErrorDetail.of(
                "RETRIEVAL_UNEXPECTED_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error retrieving raw calculation results: " + e.getMessage(),
                "calculation.results.retrieval.unexpected.error"
            ));
        }
    }
    
    /**
     * Generates a file name for calculation results.
     * Format: risk_calc_{batchId}_{timestamp}.json
     * 
     * @param batchId The batch identifier
     * @param timestamp The timestamp to use in the file name
     * @return The generated file name
     */
    private String generateFileName(String batchId, Instant timestamp) {
        String formattedTimestamp = TIMESTAMP_FORMATTER.format(timestamp.atZone(java.time.ZoneId.systemDefault()));
        return String.format("risk_calc_%s_%s.json", batchId, formattedTimestamp);
    }
}
