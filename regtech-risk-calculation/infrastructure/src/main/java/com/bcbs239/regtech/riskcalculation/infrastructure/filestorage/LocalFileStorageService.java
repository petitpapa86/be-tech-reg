package com.bcbs239.regtech.riskcalculation.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.services.IFileStorageService;
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
    public Result<String> retrieveFile(String storageUri) {
        try {
            // Extract file path from URI (handle both file:// and absolute paths)
            String filePath = parseFileUri(storageUri);
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                log.error("File not found in local storage [uri:{}]", storageUri);
                return Result.failure(ErrorDetail.of(
                    "LOCAL_FILE_NOT_FOUND",
                    ErrorType.SYSTEM_ERROR,
                    "File not found in local filesystem: " + storageUri,
                    "file.storage.local.not.found"
                ));
            }
            
            // Read file content
            String content = Files.readString(path);
            
            log.info("Downloaded file from local storage [uri:{},size:{}bytes]", 
                storageUri, content.length());
            
            return Result.success(content);
            
        } catch (IOException e) {
            log.error("Failed to download file from local storage [uri:{},error:{}]", 
                storageUri, e.getMessage(), e);
            
            return Result.failure(ErrorDetail.of(
                "LOCAL_DOWNLOAD_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to download file from local filesystem: " + e.getMessage(),
                "file.storage.local.download.error"
            ));
        } catch (Exception e) {
            log.error("Unexpected error downloading from local storage [uri:{},error:{}]", 
                storageUri, e.getMessage(), e);
            
            return Result.failure(ErrorDetail.of(
                "LOCAL_UNEXPECTED_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error downloading from local filesystem: " + e.getMessage(),
                "file.storage.local.unexpected.error"
            ));
        }
    }
    
    @Override
    public Result<String> storeFile(String fileName, String content) {
        try {
            // Create directory if it doesn't exist
            Path directoryPath = Paths.get(basePath);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
                log.info("Created base directory [path:{}]", directoryPath.toAbsolutePath());
            }
            
            // Resolve file path
            Path filePath = directoryPath.resolve(fileName);
            
            // Write content to file
            Files.writeString(filePath, content);
            
            // Generate URI
            String uri = String.format("file://%s", filePath.toAbsolutePath());
            
            log.info("Stored file locally [fileName:{},path:{},size:{}bytes]", 
                fileName, filePath.toAbsolutePath(), content.length());
            
            return Result.success(uri);
            
        } catch (IOException e) {
            log.error("Failed to store file locally [fileName:{},error:{}]", 
                fileName, e.getMessage(), e);
            
            return Result.failure(ErrorDetail.of(
                "LOCAL_STORAGE_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to store file to local filesystem: " + e.getMessage(),
                "file.storage.local.storage.error"
            ));
        } catch (Exception e) {
            log.error("Unexpected error storing file locally [fileName:{},error:{}]", 
                fileName, e.getMessage(), e);
            
            return Result.failure(ErrorDetail.of(
                "LOCAL_UNEXPECTED_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error storing file to local filesystem: " + e.getMessage(),
                "file.storage.local.unexpected.error"
            ));
        }
    }
    
    @Override
    public Result<Void> deleteFile(String storageUri) {
        try {
            // Extract file path from URI
            String filePath = parseFileUri(storageUri);
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                log.warn("File not found for deletion [uri:{}]", storageUri);
                return Result.failure(ErrorDetail.of(
                    "LOCAL_FILE_NOT_FOUND",
                    ErrorType.SYSTEM_ERROR,
                    "File not found for deletion: " + storageUri,
                    "file.storage.local.not.found"
                ));
            }
            
            // Delete the file
            Files.delete(path);
            
            log.info("Deleted file from local storage [uri:{}]", storageUri);
            
            return Result.success();
            
        } catch (IOException e) {
            log.error("Failed to delete file from local storage [uri:{},error:{}]", 
                storageUri, e.getMessage(), e);
            
            return Result.failure(ErrorDetail.of(
                "LOCAL_DELETE_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to delete file from local filesystem: " + e.getMessage(),
                "file.storage.local.delete.error"
            ));
        } catch (Exception e) {
            log.error("Unexpected error deleting file from local storage [uri:{},error:{}]", 
                storageUri, e.getMessage(), e);
            
            return Result.failure(ErrorDetail.of(
                "LOCAL_UNEXPECTED_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error deleting file from local filesystem: " + e.getMessage(),
                "file.storage.local.unexpected.error"
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
