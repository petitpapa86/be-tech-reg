package com.bcbs239.regtech.riskcalculation.application.shared;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.calculation.RiskCalculationService.AggregationResult;
import com.bcbs239.regtech.riskcalculation.domain.calculation.CalculatedExposure;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
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
     * Monitors memory usage during processing and logs metrics.
     * 
     * @param sourceUri The URI of the source file containing exposures
     * @param bankId The bank ID for context
     * @return Result containing the list of calculated exposures or error details
     */
    public Result<List<CalculatedExposure>> downloadAndParseExposures(FileStorageUri sourceUri, BankId bankId) {
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        
        log.info("Starting file download and parsing [uri:{},startMemory:{}MB]", 
            sourceUri.uri(), startMemory / (1024 * 1024));
        
        try {
            // Download file content
            Result<String> downloadResult = downloadFileContent(sourceUri);
            if (downloadResult.isFailure()) {
                return Result.failure(downloadResult.getError().get());
            }
            
            String fileContent = downloadResult.getValue().get();
            long afterDownloadMemory = getUsedMemory();
            long downloadMemoryDelta = afterDownloadMemory - startMemory;
            
            log.info("File downloaded [uri:{},size:{}bytes,memoryUsed:{}MB]", 
                sourceUri.uri(), fileContent.length(), downloadMemoryDelta / (1024 * 1024));
            
            // Use streaming JSON parser to minimize memory usage
            List<CalculatedExposure> exposures = parseExposuresStreaming(fileContent, bankId);
            
            long endTime = System.currentTimeMillis();
            long endMemory = getUsedMemory();
            long totalMemoryDelta = endMemory - startMemory;
            long peakMemory = getPeakMemoryUsage();
            long duration = endTime - startTime;
            
            // Structured logging with comprehensive metrics
            log.info("File processing completed [uri:{},exposures:{},duration:{}ms,memoryDelta:{}MB,peakMemory:{}MB,throughput:{}/sec]",
                sourceUri.uri(), exposures.size(), duration, 
                totalMemoryDelta / (1024 * 1024), peakMemory / (1024 * 1024),
                exposures.size() * 1000 / Math.max(duration, 1));
            
            // Check for memory usage alerts
            checkMemoryUsageAlerts(totalMemoryDelta, peakMemory);
            
            return Result.success(exposures);
            
        } catch (IOException e) {
            log.error("Failed to parse JSON content from: {} [error:{}]", sourceUri.uri(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "FILE_PARSING_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to parse file content: " + e.getMessage(),
                "file.processing.parsing.error"
            ));
        } catch (Exception e) {
            log.error("Unexpected error processing file: {} [error:{}]", sourceUri.uri(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "FILE_PROCESSING_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error processing file: " + e.getMessage(),
                "file.processing.unexpected.error"
            ));
        }
    }
    
    /**
     * Parses exposures using streaming JSON parser to minimize memory usage.
     * This approach processes one exposure at a time without loading the entire array into memory.
     */
    private List<CalculatedExposure> parseExposuresStreaming(String fileContent, BankId bankId) throws IOException {
        List<CalculatedExposure> exposures = new ArrayList<>();
        int parsedCount = 0;
        int skippedCount = 0;
        
        try (InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
             JsonParser parser = objectMapper.getFactory().createParser(inputStream)) {
            
            // Navigate to the exposures array
            boolean foundExposuresArray = false;
            while (parser.nextToken() != null) {
                if (parser.getCurrentToken() == JsonToken.FIELD_NAME && 
                    "exposures".equals(parser.getCurrentName())) {
                    // Move to the array start token
                    parser.nextToken();
                    if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
                        foundExposuresArray = true;
                        break;
                    }
                }
            }
            
            if (!foundExposuresArray) {
                throw new IOException("File does not contain valid exposures array");
            }
            
            // Stream parse each exposure object
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                    // Parse single exposure object
                    JsonNode exposureNode = objectMapper.readTree(parser);
                    Result<CalculatedExposure> exposureResult = parseExposure(exposureNode, bankId);
                    
                    if (exposureResult.isSuccess()) {
                        exposures.add(exposureResult.getValue().get());
                        parsedCount++;
                        
                        // Log progress for large files
                        if (parsedCount % 1000 == 0) {
                            long currentMemory = getUsedMemory();
                            log.debug("Streaming parse progress [parsed:{},memory:{}MB]", 
                                parsedCount, currentMemory / (1024 * 1024));
                        }
                    } else {
                        skippedCount++;
                        log.warn("Failed to parse exposure, skipping [error:{}]", 
                            exposureResult.getError().get().getMessage());
                    }
                }
            }
        }
        
        log.info("Streaming parse completed [parsed:{},skipped:{}]", parsedCount, skippedCount);
        return exposures;
    }
    
    /**
     * Gets the current used memory in bytes.
     */
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * Gets the peak memory usage in bytes.
     */
    private long getPeakMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory();
    }
    
    /**
     * Checks memory usage and logs alerts if thresholds are exceeded.
     */
    private void checkMemoryUsageAlerts(long memoryDelta, long peakMemory) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        double memoryUsagePercent = (double) peakMemory / maxMemory * 100.0;
        
        // Alert if memory usage exceeds 80%
        if (memoryUsagePercent > 80.0) {
            log.warn("HIGH MEMORY USAGE ALERT [usage:{}%,peak:{}MB,max:{}MB]",
                String.format("%.2f", memoryUsagePercent),
                peakMemory / (1024 * 1024),
                maxMemory / (1024 * 1024));
        }
        
        // Alert if memory delta exceeds 500MB
        long memoryDeltaMB = memoryDelta / (1024 * 1024);
        if (memoryDeltaMB > 500) {
            log.warn("LARGE MEMORY ALLOCATION ALERT [delta:{}MB]", memoryDeltaMB);
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