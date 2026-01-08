package com.bcbs239.regtech.core.infrastructure.storage;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.storage.*;
import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Service;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Unified storage service adapter that handles both S3 and local filesystem operations.
 * 
 * <p>This adapter routes storage operations to the appropriate backend based on the
 * {@link StorageUri} type, eliminating duplicate storage code across modules.
 */
@Service
public class StorageServiceAdapter implements IStorageService {
    
    private static final Logger log = LoggerFactory.getLogger(StorageServiceAdapter.class);
    
    private final CoreS3Service s3Service;
    private final JsonStorageHelper jsonHelper;
    
    public StorageServiceAdapter(CoreS3Service s3Service, JsonStorageHelper jsonHelper) {
        this.s3Service = s3Service;
        this.jsonHelper = jsonHelper;
    }
    
    @Override
    public Result<StorageResult> upload(
            @NonNull String content,
            @NonNull StorageUri uri,
            @NonNull Map<String, String> metadata) throws java.io.IOException, com.fasterxml.jackson.core.JsonProcessingException {
        
        log.debug("Uploading content to {}", uri);
        
        return switch (uri.getType()) {
            case S3 -> uploadToS3(content, uri, metadata);
            case LOCAL_ABSOLUTE, LOCAL_RELATIVE -> uploadToLocal(content, uri, metadata);
            case MEMORY -> Result.failure(
                ErrorDetail.of("MEMORY_STORAGE_NOT_IMPLEMENTED", ErrorType.SYSTEM_ERROR, 
                    "Memory storage not yet implemented", "storage.memory_not_implemented"));
        };
    }
    
    @Override
    public Result<StorageResult> uploadBytes(
            byte[] content,
            @NonNull StorageUri uri,
            @NonNull String contentType,
            @NonNull Map<String, String> metadata) throws java.io.IOException {
        
        log.debug("Uploading {} bytes to {}", content.length, uri);
        
        return switch (uri.getType()) {
            case S3 -> uploadBytesToS3(content, uri, contentType, metadata);
            case LOCAL_ABSOLUTE, LOCAL_RELATIVE -> uploadBytesToLocal(content, uri, metadata);
            case MEMORY -> Result.failure(
                ErrorDetail.of("MEMORY_STORAGE_NOT_IMPLEMENTED", ErrorType.SYSTEM_ERROR, 
                    "Memory storage not yet implemented", "storage.memory_not_implemented"));
        };
    }
    
    @Override
    public Result<String> download(@NonNull StorageUri uri) throws java.io.IOException {
        log.debug("Downloading from {}", uri);
        
        return switch (uri.getType()) {
            case S3 -> downloadFromS3(uri);
            case LOCAL_ABSOLUTE, LOCAL_RELATIVE -> downloadFromLocal(uri);
            case MEMORY -> Result.failure(
                ErrorDetail.of("MEMORY_STORAGE_NOT_IMPLEMENTED", ErrorType.SYSTEM_ERROR, 
                    "Memory storage not yet implemented", "storage.memory_not_implemented"));
        };
    }
    
    @Override
    public Result<byte[]> downloadBytes(@NonNull StorageUri uri) throws java.io.IOException {
        log.debug("Downloading bytes from {}", uri);
        
        return switch (uri.getType()) {
            case S3 -> downloadBytesFromS3(uri);
            case LOCAL_ABSOLUTE, LOCAL_RELATIVE -> downloadBytesFromLocal(uri);
            case MEMORY -> Result.failure(
                ErrorDetail.of("MEMORY_STORAGE_NOT_IMPLEMENTED", ErrorType.SYSTEM_ERROR, 
                    "Memory storage not yet implemented", "storage.memory_not_implemented"));
        };
    }
    
    @Override
    public Result<String> downloadJson(@NonNull StorageUri uri) throws java.io.IOException, com.fasterxml.jackson.core.JsonProcessingException {
        log.debug("Downloading JSON from {}", uri);
        
        Result<String> content = download(uri);
        if (content.isFailure()) {
            return content;
        }
        
        // Validate JSON structure
        return jsonHelper.validateJson(content.getValueOrThrow());
    }
    
    @Override
    public Result<Boolean> exists(@NonNull StorageUri uri) throws java.io.IOException {
        return switch (uri.getType()) {
            case S3 -> existsInS3(uri);
            case LOCAL_ABSOLUTE, LOCAL_RELATIVE -> existsInLocal(uri);
            case MEMORY -> Result.failure(
                ErrorDetail.of("MEMORY_STORAGE_NOT_IMPLEMENTED", ErrorType.SYSTEM_ERROR, 
                    "Memory storage not yet implemented", "storage.memory_not_implemented"));
        };
    }
    
    @Override
    public Result<Void> delete(@NonNull StorageUri uri) throws java.io.IOException {
        log.debug("Deleting {}", uri);
        
        return switch (uri.getType()) {
            case S3 -> deleteFromS3(uri);
            case LOCAL_ABSOLUTE, LOCAL_RELATIVE -> deleteFromLocal(uri);
            case MEMORY -> Result.failure(
                ErrorDetail.of("MEMORY_STORAGE_NOT_IMPLEMENTED", ErrorType.SYSTEM_ERROR, 
                    "Memory storage not yet implemented", "storage.memory_not_implemented"));
        };
    }
    
    @Override
    public Result<String> generatePresignedUrl(
            @NonNull StorageUri uri,
            @NonNull Duration expiration) {
        
        if (uri.getType() != StorageType.S3) {
            return Result.failure(
                ErrorDetail.of("PRESIGNED_URL_NOT_SUPPORTED", ErrorType.VALIDATION_ERROR, 
                    "Presigned URLs only supported for S3 URIs", "storage.presigned_url_s3_only"));
        }
        
        return generateS3PresignedUrl(uri, expiration);
    }
    
    @Override
    public Result<StorageResult> getMetadata(@NonNull StorageUri uri) throws java.io.IOException {
        return switch (uri.getType()) {
            case S3 -> getS3Metadata(uri);
            case LOCAL_ABSOLUTE, LOCAL_RELATIVE -> getLocalMetadata(uri);
            case MEMORY -> Result.failure(
                ErrorDetail.of("MEMORY_STORAGE_NOT_IMPLEMENTED", ErrorType.SYSTEM_ERROR, 
                    "Memory storage not yet implemented", "storage.memory_not_implemented"));
        };
    }
    
    // ========================================================================
    // S3 Operations
    // ========================================================================
    
    private Result<StorageResult> uploadToS3(String content, StorageUri uri, Map<String, String> metadata) {
        String bucket = uri.getBucket();
        String key = uri.getKey();
        
        if (bucket == null || key == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_S3_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid S3 URI: " + uri, "storage.invalid_s3_uri"));
        }
        
        // Let exceptions propagate to GlobalExceptionHandler
        s3Service.putString(bucket, key, content, "text/plain", metadata, null);
        
        StorageResult result = StorageResult.builder()
            .uri(uri)
            .contentType("text/plain")
            .sizeBytes(content.getBytes(StandardCharsets.UTF_8).length)
            .metadata(metadata)
            .uploadedAt(Instant.now())
            .build();
        
        return Result.success(result);
    }
    
    private Result<StorageResult> uploadBytesToS3(
            byte[] content,
            StorageUri uri,
            String contentType,
            Map<String, String> metadata) {
        
        String bucket = uri.getBucket();
        String key = uri.getKey();
        
        if (bucket == null || key == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_S3_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid S3 URI: " + uri, "storage.invalid_s3_uri"));
        }
        
        // Let exceptions propagate to GlobalExceptionHandler
        s3Service.putBytes(bucket, key, content, contentType, metadata, null);
        
        StorageResult result = StorageResult.builder()
            .uri(uri)
            .contentType(contentType)
            .sizeBytes(content.length)
            .metadata(metadata)
            .uploadedAt(Instant.now())
            .build();
        
        return Result.success(result);
    }
    
    private Result<String> downloadFromS3(StorageUri uri) throws java.io.IOException {
        String bucket = uri.getBucket();
        String key = uri.getKey();
        
        if (bucket == null || key == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_S3_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid S3 URI: " + uri, "storage.invalid_s3_uri"));
        }
        
        // Let exceptions propagate to GlobalExceptionHandler
        var stream = s3Service.getObjectStream(bucket, key);
        String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        stream.close();
        return Result.success(content);
    }
    
    private Result<byte[]> downloadBytesFromS3(StorageUri uri) throws java.io.IOException {
        String bucket = uri.getBucket();
        String key = uri.getKey();
        
        if (bucket == null || key == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_S3_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid S3 URI: " + uri, "storage.invalid_s3_uri"));
        }
        
        // Let exceptions propagate to GlobalExceptionHandler
        var stream = s3Service.getObjectStream(bucket, key);
        byte[] content = stream.readAllBytes();
        stream.close();
        return Result.success(content);
    }
    
    private Result<Boolean> existsInS3(StorageUri uri) {
        String bucket = uri.getBucket();
        String key = uri.getKey();
        
        if (bucket == null || key == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_S3_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid S3 URI: " + uri, "storage.invalid_s3_uri"));
        }
        
        // For exists check, NoSuchKeyException is expected (means object doesn't exist)
        // This is business logic, not an error condition
        try {
            s3Service.headObject(bucket, key);
            return Result.success(true);
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            return Result.success(false);
        }
    }
    
    private Result<Void> deleteFromS3(StorageUri uri) {
        String bucket = uri.getBucket();
        String key = uri.getKey();
        
        if (bucket == null || key == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_S3_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid S3 URI: " + uri, "storage.invalid_s3_uri"));
        }
        
        // Let exceptions propagate to GlobalExceptionHandler
        s3Service.deleteObject(bucket, key);
        return Result.success(null);
    }
    
    private Result<String> generateS3PresignedUrl(StorageUri uri, Duration expiration) {
        String bucket = uri.getBucket();
        String key = uri.getKey();
        
        if (bucket == null || key == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_S3_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid S3 URI: " + uri, "storage.invalid_s3_uri"));
        }
        
        // Let exceptions propagate to GlobalExceptionHandler
        String[] urlHolder = new String[1];
        s3Service.generatePresignedUrl(bucket, key, expiration, url -> {
            urlHolder[0] = url;
            return null;
        });
        
        if (urlHolder[0] != null) {
            return Result.success(urlHolder[0]);
        } else {
            return Result.failure(
                ErrorDetail.of("PRESIGNED_URL_GENERATION_FAILED", ErrorType.SYSTEM_ERROR, 
                    "Failed to generate presigned URL", "storage.presigned_url_failed"));
        }
    }
    
    private Result<StorageResult> getS3Metadata(StorageUri uri) {
        String bucket = uri.getBucket();
        String key = uri.getKey();
        
        if (bucket == null || key == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_S3_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid S3 URI: " + uri, "storage.invalid_s3_uri"));
        }
        
        // Let exceptions propagate to GlobalExceptionHandler
        var response = s3Service.headObject(bucket, key);
        
        StorageResult result = StorageResult.builder()
            .uri(uri)
            .sizeBytes(response.contentLength())
            .contentType(response.contentType())
            .metadata(response.metadata())
            .etag(response.eTag())
            .build();
        
        return Result.success(result);
    }
    
    // ========================================================================
    // Local Filesystem Operations
    // ========================================================================
    
    private Result<StorageResult> uploadToLocal(String content, StorageUri uri, Map<String, String> metadata) throws java.io.IOException {
        String filePath = uri.getFilePath();
        if (filePath == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_LOCAL_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid local URI: " + uri, "storage.invalid_local_uri"));
        }
        
        // Let exceptions propagate to GlobalExceptionHandler
        Path path = Paths.get(filePath);
        
        // Create parent directories if they don't exist
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        
        Files.writeString(path, content, StandardCharsets.UTF_8);
        
        StorageResult result = StorageResult.builder()
            .uri(uri)
            .contentType("text/plain")
            .sizeBytes(content.getBytes(StandardCharsets.UTF_8).length)
            .metadata(metadata)
            .uploadedAt(Instant.now())
            .build();
        
        return Result.success(result);
    }
    
    private Result<StorageResult> uploadBytesToLocal(byte[] content, StorageUri uri, Map<String, String> metadata) throws java.io.IOException {
        String filePath = uri.getFilePath();
        if (filePath == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_LOCAL_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid local URI: " + uri, "storage.invalid_local_uri"));
        }
        
        // Let exceptions propagate to GlobalExceptionHandler
        Path path = Paths.get(filePath);
        
        // Create parent directories if they don't exist
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        
        Files.write(path, content);
        
        StorageResult result = StorageResult.builder()
            .uri(uri)
            .sizeBytes(content.length)
            .metadata(metadata)
            .uploadedAt(Instant.now())
            .build();
        
        return Result.success(result);
    }
    
    private Result<String> downloadFromLocal(StorageUri uri) throws java.io.IOException {
        String filePath = uri.getFilePath();
        if (filePath == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_LOCAL_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid local URI: " + uri, "storage.invalid_local_uri"));
        }
        
        // Let exceptions propagate to GlobalExceptionHandler
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            return Result.failure(
                ErrorDetail.of("FILE_NOT_FOUND", ErrorType.NOT_FOUND_ERROR, 
                    "File not found: " + filePath, "storage.file_not_found"));
        }
        
        String content = Files.readString(path, StandardCharsets.UTF_8);
        return Result.success(content);
    }
    
    private Result<byte[]> downloadBytesFromLocal(StorageUri uri) throws java.io.IOException {
        String filePath = uri.getFilePath();
        if (filePath == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_LOCAL_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid local URI: " + uri, "storage.invalid_local_uri"));
        }
        
        // Let exceptions propagate to GlobalExceptionHandler
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            return Result.failure(
                ErrorDetail.of("FILE_NOT_FOUND", ErrorType.NOT_FOUND_ERROR, 
                    "File not found: " + filePath, "storage.file_not_found"));
        }
        
        byte[] content = Files.readAllBytes(path);
        return Result.success(content);
    }
    
    private Result<Boolean> existsInLocal(StorageUri uri) {
        String filePath = uri.getFilePath();
        if (filePath == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_LOCAL_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid local URI: " + uri, "storage.invalid_local_uri"));
        }
        
        // Let exceptions propagate to GlobalExceptionHandler
        Path path = Paths.get(filePath);
        boolean exists = Files.exists(path);
        return Result.success(exists);
    }
    
    private Result<Void> deleteFromLocal(StorageUri uri) throws java.io.IOException {
        String filePath = uri.getFilePath();
        if (filePath == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_LOCAL_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid local URI: " + uri, "storage.invalid_local_uri"));
        }
        
        // Let exceptions propagate to GlobalExceptionHandler
        Path path = Paths.get(filePath);
        
        if (Files.exists(path)) {
            Files.delete(path);
        }
        
        return Result.success(null);
    }
    
    private Result<StorageResult> getLocalMetadata(StorageUri uri) throws java.io.IOException {
        String filePath = uri.getFilePath();
        if (filePath == null) {
            return Result.failure(
                ErrorDetail.of("INVALID_LOCAL_URI", ErrorType.VALIDATION_ERROR, 
                    "Invalid local URI: " + uri, "storage.invalid_local_uri"));
        }
        
        // Let exceptions propagate to GlobalExceptionHandler
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            return Result.failure(
                ErrorDetail.of("FILE_NOT_FOUND", ErrorType.NOT_FOUND_ERROR, 
                    "File not found: " + filePath, "storage.file_not_found"));
        }
        
        long size = Files.size(path);
        
        StorageResult result = StorageResult.builder()
            .uri(uri)
            .sizeBytes(size)
            .metadata(Map.of("path", filePath))
            .build();
        
        return Result.success(result);
    }
}
