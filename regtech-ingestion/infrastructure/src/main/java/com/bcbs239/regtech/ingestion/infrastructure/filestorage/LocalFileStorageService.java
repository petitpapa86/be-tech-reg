package com.bcbs239.regtech.ingestion.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.batch.S3Reference;
import com.bcbs239.regtech.ingestion.domain.services.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Local filesystem implementation of FileStorageService for development mode.
 * Stores files in /data/raw/ directory structure instead of S3.
 * 
 * Directory structure:
 * /data/raw/
 *   ├── batch_20240331_001.json
 *   ├── batch_20240331_002.json
 *   └── ...
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = false)
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);
    private static final String BASE_PATH = "data/raw";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public Result<S3Reference> storeFile(InputStream fileStream,
                                       FileMetadata fileMetadata,
                                       String batchId,
                                       String bankId,
                                       int exposureCount) {

        log.debug("Storing file to local filesystem for batchId: {}, bankId: {}, exposureCount: {}",
            batchId, bankId, exposureCount);

        if (fileStream == null) {
            return Result.failure(ErrorDetail.of("NULL_FILE_STREAM", ErrorType.SYSTEM_ERROR, 
                "File stream cannot be null", "generic.error"));
        }

        if (fileMetadata == null) {
            return Result.failure(ErrorDetail.of("NULL_FILE_METADATA", ErrorType.SYSTEM_ERROR, 
                "File metadata cannot be null", "generic.error"));
        }

        if (batchId == null || batchId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_BATCH_ID", ErrorType.SYSTEM_ERROR, 
                "Batch ID cannot be null or empty", "generic.error"));
        }

        if (bankId == null || bankId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_BANK_ID", ErrorType.SYSTEM_ERROR, 
                "Bank ID cannot be null or empty", "generic.error"));
        }

        if (exposureCount < 0) {
            return Result.failure(ErrorDetail.of("INVALID_EXPOSURE_COUNT", ErrorType.SYSTEM_ERROR, 
                "Exposure count cannot be negative", "generic.error"));
        }

        try {
            // Create base directory if it doesn't exist
            Path baseDir = Paths.get(BASE_PATH);
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
                log.info("Created base directory: {}", baseDir.toAbsolutePath());
            }

            // Generate filename: batch_20240331_001.json
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String fileName = String.format("batch_%s_%s.json", timestamp, batchId);
            Path filePath = baseDir.resolve(fileName);

            // Write file to filesystem
            Files.copy(fileStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            // Create S3Reference with local file information
            // Using file:// protocol for local filesystem
            String bucket = "local-storage";
            String key = fileName;
            String versionId = String.valueOf(Files.getLastModifiedTime(filePath).toMillis());
            String uri = filePath.toAbsolutePath().toUri().toString();

            S3Reference s3Reference = new S3Reference(
                bucket,
                key,
                versionId,
                uri
            );

            log.info("Successfully stored processed file for batch {} with {} exposures to local path: {}",
                batchId, exposureCount, filePath.toAbsolutePath());

            return Result.success(s3Reference);

        } catch (IOException e) {
            log.error("Failed to store file to local filesystem for batchId: {}, bankId: {}", 
                batchId, bankId, e);
            return Result.failure(ErrorDetail.of("LOCAL_STORAGE_ERROR", ErrorType.SYSTEM_ERROR,
                String.format("Failed to store file to local filesystem: %s", e.getMessage()), 
                "storage.local.write.error"));
        } catch (Exception e) {
            log.error("Unexpected error storing file to local filesystem for batchId: {}, bankId: {}", 
                batchId, bankId, e);
            return Result.failure(ErrorDetail.of("STORAGE_ERROR", ErrorType.SYSTEM_ERROR,
                String.format("Unexpected storage error: %s", e.getMessage()), 
                "storage.unexpected.error"));
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
            
            log.debug("Local filesystem storage health check passed");
            return Result.success(true);
            
        } catch (IOException e) {
            log.warn("Local filesystem storage health check failed: {}", e.getMessage());
            return Result.failure(ErrorDetail.of("LOCAL_STORAGE_HEALTH_CHECK_FAILED", ErrorType.SYSTEM_ERROR,
                "Local filesystem storage is not available: " + e.getMessage(), 
                "storage.local.health.check.failed"));
        } catch (Exception e) {
            log.warn("Unexpected error during local storage health check: {}", e.getMessage());
            return Result.failure(ErrorDetail.of("LOCAL_STORAGE_HEALTH_CHECK_ERROR", ErrorType.SYSTEM_ERROR,
                "Unexpected error during local storage health check: " + e.getMessage(), 
                "storage.local.health.check.error"));
        }
    }
}
