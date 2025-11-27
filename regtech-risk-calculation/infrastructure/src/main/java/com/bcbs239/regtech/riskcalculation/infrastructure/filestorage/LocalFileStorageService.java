package com.bcbs239.regtech.riskcalculation.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.services.IFileStorageService;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Local filesystem implementation of file storage service.
 * Used in development profile for storing calculation results.
 */
@Service("riskCalculationLocalFileStorageService")
@ConditionalOnProperty(name = "risk-calculation.storage.type", havingValue = "local")
@RequiredArgsConstructor
@Slf4j
public class LocalFileStorageService implements IFileStorageService {
    
    private final ObjectMapper objectMapper;
    
    @Value("${risk-calculation.storage.local.base-path:./data/risk-calculations}")
    private String basePath;
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    @Override
    public Result<String> downloadFileContent(FileStorageUri uri) {
        try {
            // Extract file path from URI (handle both file:// and absolute paths)
            String filePath = parseFileUri(uri.uri());
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                log.error("File not found in local storage [uri:{}]", uri.uri());
                return Result.failure(ErrorDetail.of(
                    "LOCAL_FILE_NOT_FOUND",
                    ErrorType.SYSTEM_ERROR,
                    "File not found in local filesystem: " + uri.uri(),
                    "file.storage.local.not.found"
                ));
            }
            
            // Read file content
            String content = Files.readString(path);
            
            log.info("Downloaded file from local storage [uri:{},size:{}bytes]", 
                uri.uri(), content.length());
            
            return Result.success(content);
            
        } catch (IOException e) {
            log.error("Failed to download file from local storage [uri:{},error:{}]", 
                uri.uri(), e.getMessage(), e);
            
            return Result.failure(ErrorDetail.of(
                "LOCAL_DOWNLOAD_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to download file from local filesystem: " + e.getMessage(),
                "file.storage.local.download.error"
            ));
        } catch (Exception e) {
            log.error("Unexpected error downloading from local storage [uri:{},error:{}]", 
                uri.uri(), e.getMessage(), e);
            
            return Result.failure(ErrorDetail.of(
                "LOCAL_UNEXPECTED_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error downloading from local filesystem: " + e.getMessage(),
                "file.storage.local.unexpected.error"
            ));
        }
    }
    
    @Override
    public Result<FileStorageUri> storeCalculationResults(BatchId batchId, String content) {
        try {
            // Create directory if it doesn't exist
            Path directoryPath = Paths.get(basePath);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
                log.info("Created base directory [path:{}]", directoryPath.toAbsolutePath());
            }
            
            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String fileName = String.format("%s_results_%s.json", batchId.value(), timestamp);
            Path filePath = directoryPath.resolve(fileName);
            
            // Write content to file
            Files.writeString(filePath, content);
            
            // Generate URI
            String uri = String.format("file://%s", filePath.toAbsolutePath());
            
            log.info("Stored calculation results locally [batchId:{},path:{},size:{}bytes]", 
                batchId.value(), filePath.toAbsolutePath(), content.length());
            
            return Result.success(new FileStorageUri(uri));
            
        } catch (IOException e) {
            log.error("Failed to store results locally [batchId:{},error:{}]", 
                batchId.value(), e.getMessage(), e);
            
            return Result.failure(ErrorDetail.of(
                "LOCAL_STORAGE_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to store results to local filesystem: " + e.getMessage(),
                "file.storage.local.storage.error"
            ));
        } catch (Exception e) {
            log.error("Unexpected error storing results locally [batchId:{},error:{}]", 
                batchId.value(), e.getMessage(), e);
            
            return Result.failure(ErrorDetail.of(
                "LOCAL_UNEXPECTED_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error storing results to local filesystem: " + e.getMessage(),
                "file.storage.local.unexpected.error"
            ));
        }
    }
    
    @Override
    public Result<Boolean> checkServiceHealth() {
        try {
            // Check if base directory exists or can be created
            Path directoryPath = Paths.get(basePath);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }
            
            // Verify we can write to the directory
            if (!Files.isWritable(directoryPath)) {
                return Result.failure(ErrorDetail.of(
                    "LOCAL_STORAGE_NOT_WRITABLE",
                    ErrorType.SYSTEM_ERROR,
                    "Local storage directory is not writable: " + basePath,
                    "file.storage.local.not.writable"
                ));
            }
            
            // Try to create a test file to verify write permissions
            Path testFile = directoryPath.resolve(".health_check");
            Files.writeString(testFile, "health check");
            Files.deleteIfExists(testFile);
            
            log.debug("Local file storage health check passed [basePath:{}]", basePath);
            return Result.success(true);
            
        } catch (IOException e) {
            log.warn("Local file storage health check failed [error:{}]", e.getMessage());
            return Result.failure(ErrorDetail.of(
                "LOCAL_STORAGE_HEALTH_CHECK_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Local storage health check failed: " + e.getMessage(),
                "file.storage.local.health.error"
            ));
        } catch (Exception e) {
            log.warn("Unexpected error during local storage health check [error:{}]", e.getMessage());
            return Result.failure(ErrorDetail.of(
                "LOCAL_STORAGE_HEALTH_CHECK_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error during local storage health check: " + e.getMessage(),
                "file.storage.local.health.unexpected.error"
            ));
        }
    }
    
    /**
     * Parses a file URI to extract the file path, handling both Unix and Windows formats.
     * Also handles URL-encoded characters (e.g., %20 for spaces).
     * 
     * @param uri The file URI (e.g., "file:///C:/path" or "file:///path" or just "/path")
     * @return The file path suitable for Paths.get()
     */
    private String parseFileUri(String uri) {
        String path = uri;
        
        if (uri.startsWith("file://")) {
            // Remove file:// prefix
            path = uri.substring(7);
            
            // On Windows, file URIs look like file:///C:/path
            // After removing file://, we get /C:/path which needs to become C:/path
            if (path.startsWith("/") && path.length() > 2 && path.charAt(2) == ':') {
                // Windows path: /C:/path -> C:/path
                path = path.substring(1);
            }
        }
        
        // Decode URL-encoded characters (e.g., %20 -> space)
        try {
            path = java.net.URLDecoder.decode(path, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to URL decode path, using as-is [path:{},error:{}]", path, e.getMessage());
        }
        
        return path;
    }
}
