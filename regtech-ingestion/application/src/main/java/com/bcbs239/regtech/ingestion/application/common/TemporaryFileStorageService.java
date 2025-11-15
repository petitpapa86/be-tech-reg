package com.bcbs239.regtech.ingestion.application.common;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary file storage service for storing uploaded files in memory
 * during the upload+process operation. Files are stored with a reference key
 * and automatically cleaned up after processing.
 */
@Service
@Slf4j
public class TemporaryFileStorageService {

    // In-memory storage for temporary files (key -> file data)
    private final ConcurrentHashMap<String, FileData> temporaryFiles = new ConcurrentHashMap<>();

    /**
     * Store a file temporarily and return a reference key.
     */
    public Result<String> storeFile(InputStream fileStream, String fileName, String contentType, long fileSize) {
        try {
            // Read the entire file into memory
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;
            long maxFileSize = 500L * 1024 * 1024; // 500MB max safety limit

            while ((bytesRead = fileStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
                totalBytesRead += bytesRead;

                // Safety check to prevent memory exhaustion (using absolute max limit)
                if (totalBytesRead > maxFileSize) {
                    return Result.failure(ErrorDetail.of("FILE_TOO_LARGE", ErrorType.VALIDATION_ERROR,
                        "File size exceeds maximum allowed size (500MB)", "file.storage.size.exceeded"));
                }
            }

            // Use actual bytes read as the file size (since fileSize param might be 0 or incorrect)
            long actualFileSize = totalBytesRead;
            
            // Log warning if declared size doesn't match actual size
            if (fileSize > 0 && fileSize != actualFileSize) {
                log.warn("Declared file size ({}) doesn't match actual size ({}). Using actual size.", 
                    fileSize, actualFileSize);
            }

            byte[] fileBytes = buffer.toByteArray();
            String referenceKey = UUID.randomUUID().toString();

            FileData fileData = new FileData(fileBytes, fileName, contentType, actualFileSize);
            temporaryFiles.put(referenceKey, fileData);

            log.info("Stored temporary file with key: {}, fileName: {}, contentType: {}, size: {} bytes, arrayLength: {}", 
                referenceKey, fileName, contentType, actualFileSize, fileBytes.length);

            return Result.success(referenceKey);

        } catch (IOException e) {
            log.error("Failed to store temporary file: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of("FILE_STORAGE_ERROR", ErrorType.SYSTEM_ERROR,
                "Failed to store file temporarily: " + e.getMessage(), "file.storage.error"));
        }
    }

    /**
     * Retrieve a temporarily stored file by reference key.
     */
    public Result<FileData> retrieveFile(String referenceKey) {
        FileData fileData = temporaryFiles.get(referenceKey);
        if (fileData == null) {
            log.warn("Temporary file not found for key: {}. Available keys: {}", referenceKey, temporaryFiles.keySet());
            return Result.failure(ErrorDetail.of("FILE_NOT_FOUND", ErrorType.NOT_FOUND_ERROR,
                "Temporary file not found or expired", "file.storage.not.found"));
        }

        log.info("Retrieved temporary file for key: {}, fileName: {}, dataLength: {}", 
            referenceKey, fileData.fileName(), fileData.data() != null ? fileData.data().length : 0);
        return Result.success(fileData);
    }

    /**
     * Remove a temporarily stored file (cleanup after processing).
     */
    public void removeFile(String referenceKey) {
        FileData removed = temporaryFiles.remove(referenceKey);
        if (removed != null) {
            log.debug("Removed temporary file with key: {}", referenceKey);
        }
    }

    /**
     * Data structure for stored file information.
     */
    public record FileData(byte[] data, String fileName, String contentType, long fileSize) {

        public InputStream getInputStream() {
            return new ByteArrayInputStream(data);
        }
    }
}
