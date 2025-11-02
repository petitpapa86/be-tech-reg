package com.bcbs239.regtech.ingestion.infrastructure.performance;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.infrastructure.service.ParsedFileData;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * High-performance streaming JSON processor optimized for large files.
 * Uses memory-efficient streaming parsing with configurable batch processing.
 */
@Component
@Slf4j
public class StreamingJsonProcessor {

    private final int batchSize;
    private final int maxMemoryThreshold;
    private final JsonFactory jsonFactory;

    public StreamingJsonProcessor(
            @Value("${ingestion.streaming.batch-size:1000}") int batchSize,
            @Value("${ingestion.streaming.memory-threshold:50000}") int maxMemoryThreshold) {
        this.batchSize = batchSize;
        this.maxMemoryThreshold = maxMemoryThreshold;
        this.jsonFactory = new JsonFactory();
        
        // Configure JsonFactory for optimal streaming performance
        this.jsonFactory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        this.jsonFactory.configure(JsonParser.Feature.USE_FAST_DOUBLE_PARSER, true);
        
        log.info("Initialized StreamingJsonProcessor with batch size: {}, memory threshold: {}", 
            batchSize, maxMemoryThreshold);
    }

    /**
     * Process large JSON files with streaming and batch processing.
     */
    public Result<ParsedFileData> processLargeJsonFile(
            InputStream fileStream, 
            String fileName,
            Consumer<List<ParsedFileData.ExposureRecord>> batchProcessor) {
        
        log.info("Starting streaming JSON processing for large file: {}", fileName);
        
        try (JsonParser parser = jsonFactory.createParser(fileStream)) {
            
            // Expect array of objects
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                return Result.failure(ErrorDetail.of("INVALID_JSON_FORMAT", 
                    "JSON file must contain an array of exposure objects"));
            }
            
            List<ParsedFileData.ExposureRecord> currentBatch = new ArrayList<>();
            Set<String> seenExposureIds = new HashSet<>();
            int totalProcessed = 0;
            int lineNumber = 1;
            long startTime = System.currentTimeMillis();
            
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                lineNumber++;
                
                Result<ParsedFileData.ExposureRecord> recordResult = 
                    parseStreamingExposureRecord(parser, lineNumber);
                
                if (recordResult.isFailure()) {
                    return Result.failure(recordResult.getError().orElse(
                        ErrorDetail.of("PARSING_ERROR", "Failed to parse exposure record")));
                }
                
                ParsedFileData.ExposureRecord record = recordResult.getValue().orElseThrow();
                
                // Check for duplicate exposure_id
                if (seenExposureIds.contains(record.getExposureId())) {
                    return Result.failure(ErrorDetail.of("DUPLICATE_EXPOSURE_ID", 
                        String.format("Duplicate exposure_id '%s' found at line %d", 
                            record.getExposureId(), lineNumber)));
                }
                
                seenExposureIds.add(record.getExposureId());
                currentBatch.add(record);
                totalProcessed++;
                
                // Process batch when it reaches the configured size
                if (currentBatch.size() >= batchSize) {
                    processBatch(currentBatch, batchProcessor, totalProcessed);
                    currentBatch.clear();
                    
                    // Memory optimization: clear seen IDs periodically for very large files
                    if (seenExposureIds.size() > maxMemoryThreshold) {
                        log.info("Clearing duplicate check cache at {} records to optimize memory", 
                            totalProcessed);
                        seenExposureIds.clear();
                    }
                    
                    // Log progress for large files
                    if (totalProcessed % 10000 == 0) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        double rate = totalProcessed / (elapsed / 1000.0);
                        log.info("Processed {} records in {}ms (rate: {:.1f} records/sec)", 
                            totalProcessed, elapsed, rate);
                    }
                }
            }
            
            // Process remaining records in the final batch
            if (!currentBatch.isEmpty()) {
                processBatch(currentBatch, batchProcessor, totalProcessed);
            }
            
            if (totalProcessed == 0) {
                return Result.failure(ErrorDetail.of("EMPTY_FILE", 
                    "File contains no exposure records"));
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            double rate = totalProcessed / (totalTime / 1000.0);
            
            log.info("Successfully processed {} records from {} in {}ms (rate: {:.1f} records/sec)", 
                totalProcessed, fileName, totalTime, rate);
            
            // Return summary data (individual records processed via callback)
            ParsedFileData result = ParsedFileData.builder()
                .exposures(new ArrayList<>()) // Empty list for memory efficiency
                .totalCount(totalProcessed)
                .fileName(fileName)
                .contentType("application/json")
                .build();
                
            return Result.success(result);
            
        } catch (IOException e) {
            log.error("IO error during streaming JSON processing: {}", fileName, e);
            return Result.failure(ErrorDetail.of("JSON_PARSING_ERROR", 
                "Failed to parse JSON file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during streaming JSON processing: {}", fileName, e);
            return Result.failure(ErrorDetail.of("PARSING_ERROR", 
                "Unexpected error parsing file: " + e.getMessage()));
        }
    }

    /**
     * Process JSON files with memory usage monitoring.
     */
    public Result<ParsedFileData> processWithMemoryMonitoring(
            InputStream fileStream, 
            String fileName) {
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        log.debug("Starting JSON processing with memory monitoring. Initial memory: {}MB", 
            initialMemory / 1024 / 1024);
        
        List<ParsedFileData.ExposureRecord> allRecords = new ArrayList<>();
        
        Result<ParsedFileData> result = processLargeJsonFile(fileStream, fileName, batch -> {
            allRecords.addAll(batch);
            
            // Monitor memory usage during processing
            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = currentMemory - initialMemory;
            
            if (memoryIncrease > 100 * 1024 * 1024) { // 100MB increase
                log.warn("High memory usage detected during processing: {}MB increase", 
                    memoryIncrease / 1024 / 1024);
                
                // Suggest garbage collection
                System.gc();
            }
        });
        
        if (result.isSuccess()) {
            // Update the result with all collected records
            ParsedFileData data = result.getValue().orElseThrow();
            ParsedFileData updatedData = ParsedFileData.builder()
                .exposures(allRecords)
                .totalCount(data.getTotalCount())
                .fileName(data.getFileName())
                .contentType(data.getContentType())
                .build();
            
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            log.debug("Completed JSON processing. Final memory: {}MB, increase: {}MB", 
                finalMemory / 1024 / 1024, (finalMemory - initialMemory) / 1024 / 1024);
            
            return Result.success(updatedData);
        }
        
        return result;
    }

    private void processBatch(List<ParsedFileData.ExposureRecord> batch, 
                            Consumer<List<ParsedFileData.ExposureRecord>> processor,
                            int totalProcessed) {
        try {
            processor.accept(new ArrayList<>(batch));
            log.debug("Processed batch of {} records (total: {})", batch.size(), totalProcessed);
        } catch (Exception e) {
            log.error("Error processing batch at record {}: {}", totalProcessed, e.getMessage(), e);
            throw new RuntimeException("Batch processing failed", e);
        }
    }

    private Result<ParsedFileData.ExposureRecord> parseStreamingExposureRecord(
            JsonParser parser, int lineNumber) throws IOException {
        
        String exposureId = null;
        BigDecimal amount = null;
        String currency = null;
        String country = null;
        String sector = null;
        
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            parser.nextToken();
            
            switch (fieldName) {
                case "exposure_id":
                    exposureId = parser.getValueAsString();
                    break;
                case "amount":
                    try {
                        String amountStr = parser.getValueAsString();
                        amount = new BigDecimal(amountStr);
                        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                            return Result.failure(ErrorDetail.of("INVALID_AMOUNT", 
                                String.format("Amount must be positive at line %d", lineNumber)));
                        }
                    } catch (NumberFormatException e) {
                        return Result.failure(ErrorDetail.of("INVALID_AMOUNT_FORMAT", 
                            String.format("Invalid amount format at line %d", lineNumber)));
                    }
                    break;
                case "currency":
                    currency = parser.getValueAsString();
                    break;
                case "country":
                    country = parser.getValueAsString();
                    break;
                case "sector":
                    sector = parser.getValueAsString();
                    break;
                default:
                    // Skip unknown fields efficiently
                    parser.skipChildren();
                    break;
            }
        }
        
        // Validate required fields
        if (exposureId == null || exposureId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("MISSING_EXPOSURE_ID", 
                String.format("Missing or empty exposure_id at line %d", lineNumber)));
        }
        if (amount == null) {
            return Result.failure(ErrorDetail.of("MISSING_AMOUNT", 
                String.format("Missing amount at line %d", lineNumber)));
        }
        if (currency == null || currency.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("MISSING_CURRENCY", 
                String.format("Missing currency at line %d", lineNumber)));
        }
        if (country == null || country.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("MISSING_COUNTRY", 
                String.format("Missing country at line %d", lineNumber)));
        }
        if (sector == null || sector.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("MISSING_SECTOR", 
                String.format("Missing sector at line %d", lineNumber)));
        }
        
        return Result.success(ParsedFileData.ExposureRecord.builder()
            .exposureId(exposureId.trim())
            .amount(amount)
            .currency(currency.trim().toUpperCase())
            .country(country.trim().toUpperCase())
            .sector(sector.trim().toUpperCase())
            .lineNumber(lineNumber)
            .build());
    }
}