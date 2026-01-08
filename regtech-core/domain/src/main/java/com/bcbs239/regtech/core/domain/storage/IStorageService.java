package com.bcbs239.regtech.core.domain.storage;

import com.bcbs239.regtech.core.domain.shared.Result;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Generic storage service interface for S3 and local filesystem operations.
 * 
 * <p>This interface provides a unified abstraction over different storage backends
 * (S3, local filesystem) to avoid code duplication across modules.
 * 
 * <p><strong>Exception Handling:</strong> Methods may throw {@link IOException} for infrastructure
 * errors (network failures, S3 errors, file I/O errors). These exceptions should propagate
 * to the boundary (controller) where {@link com.bcbs239.regtech.app.config.GlobalExceptionHandler}
 * will handle them. Methods return {@code Result.failure()} for expected errors like validation
 * failures or missing files.
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Upload to S3
 * StorageUri s3Uri = StorageUri.parse("s3://my-bucket/data/batch123.json");
 * Result<StorageResult> result = storageService.upload(
 *     jsonContent, 
 *     s3Uri, 
 *     Map.of("batchId", "batch123")
 * );
 * 
 * // Download and parse JSON
 * Result<String> json = storageService.downloadJson(s3Uri);
 * }</pre>
 */
public interface IStorageService {
    
    /**
     * Uploads content to storage
     * 
     * @param content The content to upload (String, JSON, etc.)
     * @param uri The destination storage URI
     * @param metadata Additional metadata to store with the content
     * @return Result containing StorageResult with upload details, or error
     * @throws IOException if S3 or file I/O operation fails
     * @throws com.fasterxml.jackson.core.JsonProcessingException if JSON validation fails
     */
    Result<StorageResult> upload(
        @NonNull String content,
        @NonNull StorageUri uri,
        @NonNull Map<String, String> metadata
    ) throws IOException, com.fasterxml.jackson.core.JsonProcessingException;
    
    /**
     * Uploads binary content to storage
     * 
     * @param content The binary content to upload
     * @param uri The destination storage URI
     * @param contentType The MIME type of the content
     * @param metadata Additional metadata to store with the content
     * @return Result containing StorageResult with upload details, or error
     * @throws IOException if S3 or file I/O operation fails
     */
    Result<StorageResult> uploadBytes(
        byte[] content,
        @NonNull StorageUri uri,
        @NonNull String contentType,
        @NonNull Map<String, String> metadata
    ) throws IOException;
    
    /**
     * Downloads content from storage as a string
     * 
     * @param uri The source storage URI
     * @return Result containing the downloaded content, or error
     * @throws IOException if S3 or file I/O operation fails
     */
    Result<String> download(@NonNull StorageUri uri) throws IOException;
    
    /**
     * Downloads binary content from storage
     * 
     * @param uri The source storage URI
     * @return Result containing the downloaded binary content, or error
     * @throws IOException if S3 or file I/O operation fails
     */
    Result<byte[]> downloadBytes(@NonNull StorageUri uri) throws IOException;
    
    /**
     * Downloads and parses JSON content from storage
     * 
     * <p>This is a convenience method that validates the content is valid JSON.
     * 
     * @param uri The source storage URI
     * @return Result containing the JSON string, or error if not valid JSON
     * @throws IOException if S3 or file I/O operation fails
     * @throws com.fasterxml.jackson.core.JsonProcessingException if JSON validation fails
     */
    Result<String> downloadJson(@NonNull StorageUri uri) throws IOException, com.fasterxml.jackson.core.JsonProcessingException;
    
    /**
     * Checks if content exists at the given URI
     * 
     * @param uri The storage URI to check
     * @return Result containing true if exists, false otherwise, or error
     * @throws IOException if S3 or file I/O operation fails
     */
    Result<Boolean> exists(@NonNull StorageUri uri) throws IOException;
    
    /**
     * Deletes content at the given URI
     * 
     * @param uri The storage URI to delete
     * @return Result containing success/failure
     * @throws IOException if S3 or file I/O operation fails
     */
    Result<Void> delete(@NonNull StorageUri uri) throws IOException;
    
    /**
     * Generates a presigned URL for temporary access to the content
     * 
     * <p>Only supported for S3 URIs. Local filesystem URIs will return an error.
     * 
     * @param uri The storage URI
     * @param expiration The duration until the URL expires
     * @return Result containing the presigned URL, or error
     */
    Result<String> generatePresignedUrl(
        @NonNull StorageUri uri,
        @NonNull Duration expiration
    );
    
    /**
     * Gets metadata about stored content
     * 
     * @param uri The storage URI
     * @return Result containing StorageResult with metadata, or error
     * @throws IOException if S3 or file I/O operation fails
     */
    Result<StorageResult> getMetadata(@NonNull StorageUri uri) throws IOException;
}
