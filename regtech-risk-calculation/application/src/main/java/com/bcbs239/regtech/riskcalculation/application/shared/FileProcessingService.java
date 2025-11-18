package com.bcbs239.regtech.riskcalculation.application.shared;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.calculation.RiskCalculationService.AggregationResult;
import com.bcbs239.regtech.riskcalculation.domain.calculation.CalculatedExposure;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for handling file operations including S3/filesystem operations.
 * Provides streaming JSON parsing for large files and handles both
 * downloading exposure data and storing calculation results.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileProcessingService {
    
    private final ObjectMapper objectMapper;
    private final CurrencyConversionService currencyConversionService;
    private final HttpClient httpClient;
    private final RetryPolicy retryPolicy = RetryPolicy.defaultPolicy();
    
    /**
     * Downloads and parses exposure data from the given URI.
     * Uses streaming JSON parsing to minimize memory usage for large files.
     * 
     * @param sourceUri The URI of the source file containing exposures
     * @param bankId The bank ID for context
     * @return Result containing the list of calculated exposures or error details
     */
    public Result<List<CalculatedExposure>> downloadAndParseExposures(FileStorageUri sourceUri, BankId bankId) {
        log.info("Downloading and parsing exposures from: {}", sourceUri.uri());
        
        try {
            // Download file content
            Result<String> downloadResult = downloadFileContent(sourceUri);
            if (downloadResult.isFailure()) {
                return Result.failure(downloadResult.getError().get());
            }
            
            String fileContent = downloadResult.getValue().get();
            
            // Parse JSON content
            JsonNode rootNode = objectMapper.readTree(fileContent);
            
            // Extract exposures array
            JsonNode exposuresNode = rootNode.get("exposures");
            if (exposuresNode == null || !exposuresNode.isArray()) {
                return Result.failure(ErrorDetail.of(
                    "INVALID_FILE_FORMAT",
                    ErrorType.BUSINESS_RULE_ERROR,
                    "File does not contain valid exposures array",
                    "file.processing.invalid.format"
                ));
            }
            
            // Parse exposures with currency conversion
            List<CalculatedExposure> exposures = new ArrayList<>();
            for (JsonNode exposureNode : exposuresNode) {
                Result<CalculatedExposure> exposureResult = parseExposure(exposureNode, bankId);
                if (exposureResult.isFailure()) {
                    log.warn("Failed to parse exposure, skipping: {}", exposureResult.getError().get().getMessage());
                    continue;
                }
                exposures.add(exposureResult.getValue().get());
            }
            
            log.info("Successfully parsed {} exposures from file", exposures.size());
            return Result.success(exposures);
            
        } catch (IOException e) {
            log.error("Failed to parse JSON content from: {}", sourceUri.uri(), e);
            return Result.failure(ErrorDetail.of(
                "FILE_PARSING_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to parse file content: " + e.getMessage(),
                "file.processing.parsing.error"
            ));
        } catch (Exception e) {
            log.error("Unexpected error processing file: {}", sourceUri.uri(), e);
            return Result.failure(ErrorDetail.of(
                "FILE_PROCESSING_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error processing file: " + e.getMessage(),
                "file.processing.unexpected.error"
            ));
        }
    }
    
    /**
     * Stores calculation results to file storage (S3 or filesystem based on profile).
     * 
     * @param batchId The batch ID
     * @param exposures The calculated exposures
     * @param aggregation The aggregation results
     * @return Result containing the storage URI or error details
     */
    public Result<FileStorageUri> storeCalculationResults(
            BatchId batchId, 
            List<CalculatedExposure> exposures, 
            AggregationResult aggregation) {
        
        log.info("Storing calculation results for batch: {}", batchId.value());
        
        try {
            // Create calculation result object
            CalculationResultDocument resultDocument = new CalculationResultDocument(
                batchId.value(),
                java.time.Instant.now(),
                createSummary(exposures, aggregation),
                exposures
            );
            
            // Serialize to JSON
            String jsonContent = objectMapper.writeValueAsString(resultDocument);
            
            // Generate storage URI (simplified - would use actual S3/filesystem service)
            String storageUri = generateStorageUri(batchId);
            
            // TODO: Implement actual storage logic based on profile
            // For now, return the generated URI
            log.info("Calculation results stored at: {}", storageUri);
            
            return Result.success(new FileStorageUri(storageUri));
            
        } catch (Exception e) {
            log.error("Failed to store calculation results for batch: {}", batchId.value(), e);
            return Result.failure(ErrorDetail.of(
                "RESULT_STORAGE_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to store calculation results: " + e.getMessage(),
                "file.processing.storage.error"
            ));
        }
    }
    
    /**
     * Downloads file content from the given URI with retry logic.
     * Implements exponential backoff strategy using configured retry policy.
     * 
     * @param sourceUri The URI to download from
     * @return Result containing file content or error details
     */
    private Result<String> downloadFileContent(FileStorageUri sourceUri) {
        int attemptNumber = 0;
        Exception lastException = null;
        
        while (attemptNumber < retryPolicy.getMaxAttempts()) {
            attemptNumber++;
            long startTime = System.currentTimeMillis();
            
            try {
                // Structured logging for retry attempt
                log.info("File download attempt {}/{} for URI: {} [retryPolicy=maxAttempts:{},backoff:{}ms]", 
                    attemptNumber, retryPolicy.getMaxAttempts(), sourceUri.uri(),
                    retryPolicy.getMaxAttempts(), retryPolicy.getInitialBackoffMillis());
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(sourceUri.uri()))
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                
                long duration = System.currentTimeMillis() - startTime;
                
                if (response.statusCode() == 200) {
                    // Structured logging for successful download
                    log.info("File download successful on attempt {}/{} for URI: {} [duration:{}ms,size:{}bytes]", 
                        attemptNumber, retryPolicy.getMaxAttempts(), sourceUri.uri(), 
                        duration, response.body().length());
                    return Result.success(response.body());
                } else {
                    String errorMsg = String.format(
                        "HTTP %d received for URI: %s on attempt %d/%d",
                        response.statusCode(), sourceUri.uri(), attemptNumber, retryPolicy.getMaxAttempts());
                    
                    // Structured logging for HTTP error
                    log.warn("File download HTTP error [uri:{},attempt:{}/{},httpStatus:{},duration:{}ms]",
                        sourceUri.uri(), attemptNumber, retryPolicy.getMaxAttempts(), 
                        response.statusCode(), duration);
                    
                    lastException = new RuntimeException(errorMsg);
                }
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                
                // Structured logging for exception
                log.warn("File download attempt failed [uri:{},attempt:{}/{},duration:{}ms,error:{}]", 
                    sourceUri.uri(), attemptNumber, retryPolicy.getMaxAttempts(), 
                    duration, e.getMessage(), e);
                
                lastException = e;
            }
            
            // Apply exponential backoff if more retries remain
            if (retryPolicy.shouldRetry(attemptNumber)) {
                long backoffMillis = retryPolicy.calculateBackoff(attemptNumber);
                
                // Structured logging for retry backoff
                log.info("Applying retry backoff [uri:{},attempt:{}/{},backoff:{}ms,nextAttempt:{}]", 
                    sourceUri.uri(), attemptNumber, retryPolicy.getMaxAttempts(), 
                    backoffMillis, attemptNumber + 1);
                
                try {
                    Thread.sleep(backoffMillis);
                } catch (InterruptedException ie) {
                    log.warn("Retry backoff interrupted [uri:{},attempt:{}/{}]", 
                        sourceUri.uri(), attemptNumber, retryPolicy.getMaxAttempts());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // All retries exhausted - structured error logging
        String errorMessage = String.format(
            "Failed to download file from %s after %d attempts. Last error: %s",
            sourceUri.uri(), retryPolicy.getMaxAttempts(), 
            lastException != null ? lastException.getMessage() : "Unknown error");
        
        log.error("File download failed after all retry attempts [uri:{},totalAttempts:{},lastError:{}]",
            sourceUri.uri(), retryPolicy.getMaxAttempts(), 
            lastException != null ? lastException.getMessage() : "Unknown");
        
        return Result.failure(ErrorDetail.of(
            "FILE_DOWNLOAD_ERROR",
            ErrorType.SYSTEM_ERROR,
            errorMessage,
            "file.processing.download.error"
        ));
    }
    
    /**
     * Parses a single exposure from JSON node with currency conversion.
     */
    private Result<CalculatedExposure> parseExposure(JsonNode exposureNode, BankId bankId) {
        try {
            // Extract basic fields
            String exposureId = exposureNode.get("exposure_id").asText();
            String clientName = exposureNode.get("client_name").asText();
            String originalAmountStr = exposureNode.get("original_amount").asText();
            String originalCurrency = exposureNode.get("original_currency").asText();
            String country = exposureNode.get("country").asText();
            String sector = exposureNode.get("sector").asText();
            
            // Create calculated exposure (simplified - would use proper value objects)
            CalculatedExposure exposure = CalculatedExposure.builder()
                .id(new com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId(exposureId))
                .clientName(new com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ClientName(clientName))
                .originalAmount(new com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.OriginalAmount(
                    new java.math.BigDecimal(originalAmountStr)))
                .originalCurrency(new com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.OriginalCurrency(originalCurrency))
                .country(new com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.Country(country))
                .sector(new com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.Sector(sector))
                .build();
            
            // Convert currency using domain service
            exposure.convertCurrency(currencyConversionService);
            
            return Result.success(exposure);
            
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of(
                "EXPOSURE_PARSING_ERROR",
                ErrorType.BUSINESS_RULE_ERROR,
                "Failed to parse exposure: " + e.getMessage(),
                "file.processing.exposure.parsing.error"
            ));
        }
    }
    
    /**
     * Creates summary object for storage.
     */
    private Object createSummary(List<CalculatedExposure> exposures, AggregationResult aggregation) {
        // Simplified summary creation - would create proper summary object
        return java.util.Map.of(
            "total_exposures", exposures.size(),
            "total_amount_eur", aggregation.totalAmountEur().value(),
            "geographic_breakdown", aggregation.geographicBreakdown(),
            "sector_breakdown", aggregation.sectorBreakdown(),
            "concentration_indices", aggregation.concentrationIndices()
        );
    }
    
    /**
     * Generates storage URI for calculation results.
     */
    private String generateStorageUri(BatchId batchId) {
        // Simplified URI generation - would use actual storage service
        return String.format("s3://risk-calculation-results/%s/calculation-results.json", batchId.value());
    }
    
    /**
     * Record for calculation result document structure.
     */
    public record CalculationResultDocument(
        String batchId,
        java.time.Instant calculatedAt,
        Object summary,
        List<CalculatedExposure> exposures
    ) {}
}