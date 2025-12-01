package com.bcbs239.regtech.riskcalculation.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.services.ICalculationResultsStorageService;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Local filesystem implementation for storing calculation results.
 * Stores files in data/calculated/ directory structure.
 * 
 * Directory structure:
 * data/calculated/
 *   ├── calc_batch_20240331_001_20240331_143045.json
 *   ├── calc_batch_20240331_002_20240331_143145.json
 *   └── ...
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "risk-calculation.storage.type", havingValue = "local", matchIfMissing = false)
public class LocalCalculationResultsStorageService implements ICalculationResultsStorageService {

    private static final String BASE_PATH = "data/calculated";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public Result<FileStorageUri> storeCalculationResults(String jsonContent, String batchId, String bankId) {
        log.debug("Storing calculation results to local filesystem for batchId: {}, bankId: {}", batchId, bankId);

        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("NULL_JSON_CONTENT", ErrorType.VALIDATION_ERROR,
                "JSON content cannot be null or empty", "calculation.results.null.content"));
        }

        if (batchId == null || batchId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_BATCH_ID", ErrorType.VALIDATION_ERROR,
                "Batch ID cannot be null or empty", "calculation.results.invalid.batch.id"));
        }

        if (bankId == null || bankId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_BANK_ID", ErrorType.VALIDATION_ERROR,
                "Bank ID cannot be null or empty", "calculation.results.invalid.bank.id"));
        }

        try {
            // Create base directory if it doesn't exist
            Path baseDir = Paths.get(BASE_PATH);
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
                log.info("Created base directory: {}", baseDir.toAbsolutePath());
            }

            // Generate filename: calc_batch_20240331_001_20240331_143045.json
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String fileName = String.format("calc_%s_%s.json", batchId, timestamp);
            Path filePath = baseDir.resolve(fileName);

            // Write JSON content to file
            Files.writeString(filePath, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Create FileStorageUri with file:// protocol
            String uri = filePath.toAbsolutePath().toUri().toString();
            FileStorageUri fileStorageUri = FileStorageUri.of(uri);

            log.info("Successfully stored calculation results for batch {} to local path: {}", 
                batchId, filePath.toAbsolutePath());

            return Result.success(fileStorageUri);

        } catch (IOException e) {
            log.error("Failed to store calculation results to local filesystem for batchId: {}, bankId: {}", 
                batchId, bankId, e);
            return Result.failure(ErrorDetail.of("LOCAL_STORAGE_ERROR", ErrorType.SYSTEM_ERROR,
                String.format("Failed to store calculation results: %s", e.getMessage()),
                "calculation.results.storage.error"));
        } catch (Exception e) {
            log.error("Unexpected error storing calculation results for batchId: {}, bankId: {}", 
                batchId, bankId, e);
            return Result.failure(ErrorDetail.of("STORAGE_ERROR", ErrorType.SYSTEM_ERROR,
                String.format("Unexpected storage error: %s", e.getMessage()),
                "calculation.results.unexpected.error"));
        }
    }

    @Override
    public Result<String> retrieveCalculationResults(FileStorageUri fileUri) {
        log.debug("Retrieving calculation results from local filesystem: {}", fileUri.uri());

        if (fileUri == null || fileUri.uri() == null || fileUri.uri().trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_FILE_URI", ErrorType.VALIDATION_ERROR,
                "File URI cannot be null or empty", "calculation.results.invalid.uri"));
        }

        try {
            // Handle file:// protocol
            String uriString = fileUri.uri();
            if (uriString.startsWith("file://")) {
                uriString = uriString.substring(7); // Remove file:// prefix
            }

            Path filePath = Paths.get(uriString);

            if (!Files.exists(filePath)) {
                return Result.failure(ErrorDetail.of("FILE_NOT_FOUND", ErrorType.SYSTEM_ERROR,
                    "Calculation results file not found: " + filePath,
                    "calculation.results.not.found"));
            }

            String jsonContent = Files.readString(filePath);

            log.debug("Successfully retrieved calculation results from: {}", filePath.toAbsolutePath());

            return Result.success(jsonContent);

        } catch (IOException e) {
            log.error("Failed to retrieve calculation results from: {}", fileUri.uri(), e);
            return Result.failure(ErrorDetail.of("LOCAL_RETRIEVAL_ERROR", ErrorType.SYSTEM_ERROR,
                String.format("Failed to retrieve calculation results: %s", e.getMessage()),
                "calculation.results.retrieval.error"));
        } catch (Exception e) {
            log.error("Unexpected error retrieving calculation results from: {}", fileUri.uri(), e);
            return Result.failure(ErrorDetail.of("RETRIEVAL_ERROR", ErrorType.SYSTEM_ERROR,
                String.format("Unexpected retrieval error: %s", e.getMessage()),
                "calculation.results.unexpected.retrieval.error"));
        }
    }

    @Override
    public Result<Boolean> checkServiceHealth() {
        try {
            // Check if base directory exists and is writable
            Path baseDir = Paths.get(BASE_PATH);

            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }

            // Try to create a test file to verify write permissions
            Path testFile = baseDir.resolve(".health_check");
            Files.writeString(testFile, "health check");
            Files.deleteIfExists(testFile);

            log.debug("Local calculation results storage health check passed");
            return Result.success(true);

        } catch (IOException e) {
            log.warn("Local calculation results storage health check failed: {}", e.getMessage());
            return Result.failure(ErrorDetail.of("LOCAL_STORAGE_HEALTH_CHECK_FAILED", ErrorType.SYSTEM_ERROR,
                "Local filesystem storage is not available: " + e.getMessage(),
                "calculation.results.health.check.failed"));
        } catch (Exception e) {
            log.warn("Unexpected error during local storage health check: {}", e.getMessage());
            return Result.failure(ErrorDetail.of("LOCAL_STORAGE_HEALTH_CHECK_ERROR", ErrorType.SYSTEM_ERROR,
                "Unexpected error during health check: " + e.getMessage(),
                "calculation.results.health.check.error"));
        }
    }
}
