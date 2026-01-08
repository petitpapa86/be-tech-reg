package com.bcbs239.regtech.core.infrastructure.storage;

import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.storage.*;
import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StorageServiceAdapter.
 * Tests all storage operations with mocked CoreS3Service.
 * 
 * Requirements: Phase 0A - Shared Storage Infrastructure
 * Coverage: All 16 public methods of StorageServiceAdapter
 */
@ExtendWith(MockitoExtension.class)
class StorageServiceAdapterTest {

    @Mock
    private CoreS3Service coreS3Service;

    @Mock
    private JsonStorageHelper jsonStorageHelper;

    private StorageServiceAdapter storageService;

    private static final String TEST_BUCKET = "test-bucket";
    private static final String TEST_KEY = "test-key.json";
    private static final String TEST_CONTENT = "{\"test\": \"data\"}";
    private static final byte[] TEST_BYTES = TEST_CONTENT.getBytes();

    @BeforeEach
    void setUp() {
        storageService = new StorageServiceAdapter(coreS3Service, jsonStorageHelper);
    }

    // ========================================================================
    // UPLOAD OPERATIONS
    // ========================================================================

    @Test
    void shouldUploadStringToS3Successfully() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/test-key.json");
        Map<String, String> metadata = Map.of("batchId", "batch-123");

        // Act
        Result<StorageResult> result = storageService.upload(TEST_CONTENT, uri, metadata);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getUri().uri()).isEqualTo(uri.uri());
        assertThat(result.getValue().getSizeInBytes()).isEqualTo(TEST_CONTENT.length());

        verify(coreS3Service).putString(TEST_BUCKET, TEST_KEY, TEST_CONTENT, "application/json", metadata, null);
    }

    @Test
    void shouldUploadBytesToS3Successfully() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/report.pdf");
        Map<String, String> metadata = Map.of("reportId", "report-456");

        // Act
        Result<StorageResult> result = storageService.uploadBytes(TEST_BYTES, uri, metadata);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getUri().uri()).isEqualTo(uri.uri());
        assertThat(result.getValue().getSizeInBytes()).isEqualTo(TEST_BYTES.length);

        verify(coreS3Service).putBytes(TEST_BUCKET, "report.pdf", TEST_BYTES, "application/octet-stream", metadata, null);
    }

    @Test
    void shouldHandleS3UploadFailure() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/test-key.json");
        Map<String, String> metadata = Map.of();
        
        doThrow(new IOException("S3 connection timeout"))
            .when(coreS3Service).putString(anyString(), anyString(), anyString(), anyString(), anyMap(), any());

        // Act & Assert
        assertThatThrownBy(() -> storageService.upload(TEST_CONTENT, uri, metadata))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("S3 connection timeout");

        verify(coreS3Service).putString(TEST_BUCKET, TEST_KEY, TEST_CONTENT, "application/json", metadata, null);
    }

    @Test
    void shouldRejectInvalidS3Uri() {
        // Arrange
        StorageUri invalidUri = StorageUri.parse("s3://"); // Missing bucket
        Map<String, String> metadata = Map.of();

        // Act
        Result<StorageResult> result = storageService.upload(TEST_CONTENT, invalidUri, metadata);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isPresent();
        assertThat(result.getError().get().getErrorType()).isEqualTo(ErrorType.VALIDATION_ERROR);
        assertThat(result.getError().get().getMessage()).contains("Invalid S3 URI");

        verify(coreS3Service, never()).putString(anyString(), anyString(), anyString(), anyString(), anyMap(), any());
    }

    // ========================================================================
    // DOWNLOAD OPERATIONS
    // ========================================================================

    @Test
    void shouldDownloadStringFromS3Successfully() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/test-key.json");
        when(coreS3Service.getString(TEST_BUCKET, TEST_KEY)).thenReturn(TEST_CONTENT);

        // Act
        Result<String> result = storageService.download(uri);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(TEST_CONTENT);

        verify(coreS3Service).getString(TEST_BUCKET, TEST_KEY);
    }

    @Test
    void shouldDownloadBytesFromS3Successfully() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/report.pdf");
        when(coreS3Service.getBytes(TEST_BUCKET, "report.pdf")).thenReturn(TEST_BYTES);

        // Act
        Result<byte[]> result = storageService.downloadBytes(uri);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(TEST_BYTES);

        verify(coreS3Service).getBytes(TEST_BUCKET, "report.pdf");
    }

    @Test
    void shouldHandleS3DownloadFailure() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/missing.json");
        when(coreS3Service.getString(TEST_BUCKET, "missing.json"))
            .thenThrow(new IOException("Object not found"));

        // Act & Assert
        assertThatThrownBy(() -> storageService.download(uri))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Object not found");

        verify(coreS3Service).getString(TEST_BUCKET, "missing.json");
    }

    // ========================================================================
    // JSON OPERATIONS
    // ========================================================================

    @Test
    void shouldUploadJsonToS3Successfully() throws IOException, JsonProcessingException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/data.json");
        Map<String, Object> jsonData = Map.of("key", "value", "count", 42);
        String jsonString = "{\"key\":\"value\",\"count\":42}";
        Map<String, String> metadata = Map.of("type", "test-data");

        when(jsonStorageHelper.serializeToJson(jsonData)).thenReturn(jsonString);

        // Act
        Result<StorageResult> result = storageService.uploadJson(jsonData, uri, metadata);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getUri().uri()).isEqualTo(uri.uri());

        verify(jsonStorageHelper).serializeToJson(jsonData);
        verify(coreS3Service).putString(TEST_BUCKET, "data.json", jsonString, "application/json", metadata, null);
    }

    @Test
    void shouldDownloadJsonFromS3Successfully() throws IOException, JsonProcessingException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/data.json");
        Map<String, Object> expectedData = Map.of("key", "value");

        when(coreS3Service.getString(TEST_BUCKET, "data.json")).thenReturn(TEST_CONTENT);
        when(jsonStorageHelper.deserializeFromJson(TEST_CONTENT)).thenReturn(expectedData);

        // Act
        Result<Map<String, Object>> result = storageService.downloadJson(uri);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(expectedData);

        verify(coreS3Service).getString(TEST_BUCKET, "data.json");
        verify(jsonStorageHelper).deserializeFromJson(TEST_CONTENT);
    }

    @Test
    void shouldHandleJsonSerializationFailure() throws JsonProcessingException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/data.json");
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("circular", invalidData); // Circular reference

        when(jsonStorageHelper.serializeToJson(invalidData))
            .thenThrow(new JsonProcessingException("Circular reference") {});

        // Act & Assert
        assertThatThrownBy(() -> storageService.uploadJson(invalidData, uri, Map.of()))
            .isInstanceOf(JsonProcessingException.class)
            .hasMessageContaining("Circular reference");

        verify(jsonStorageHelper).serializeToJson(invalidData);
        verify(coreS3Service, never()).putString(anyString(), anyString(), anyString(), anyString(), anyMap(), any());
    }

    @Test
    void shouldHandleJsonDeserializationFailure() throws IOException, JsonProcessingException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/invalid.json");
        String malformedJson = "{invalid json}";

        when(coreS3Service.getString(TEST_BUCKET, "invalid.json")).thenReturn(malformedJson);
        when(jsonStorageHelper.deserializeFromJson(malformedJson))
            .thenThrow(new JsonProcessingException("Malformed JSON") {});

        // Act & Assert
        assertThatThrownBy(() -> storageService.downloadJson(uri))
            .isInstanceOf(JsonProcessingException.class)
            .hasMessageContaining("Malformed JSON");

        verify(coreS3Service).getString(TEST_BUCKET, "invalid.json");
        verify(jsonStorageHelper).deserializeFromJson(malformedJson);
    }

    // ========================================================================
    // FILE EXISTENCE CHECKS
    // ========================================================================

    @Test
    void shouldCheckS3ObjectExists() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/existing.json");
        when(coreS3Service.exists(TEST_BUCKET, "existing.json")).thenReturn(true);

        // Act
        Result<Boolean> result = storageService.exists(uri);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isTrue();

        verify(coreS3Service).exists(TEST_BUCKET, "existing.json");
    }

    @Test
    void shouldCheckS3ObjectDoesNotExist() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/missing.json");
        when(coreS3Service.exists(TEST_BUCKET, "missing.json")).thenReturn(false);

        // Act
        Result<Boolean> result = storageService.exists(uri);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isFalse();

        verify(coreS3Service).exists(TEST_BUCKET, "missing.json");
    }

    @Test
    void shouldHandleExistsCheckFailure() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/test.json");
        when(coreS3Service.exists(TEST_BUCKET, TEST_KEY))
            .thenThrow(new IOException("S3 service unavailable"));

        // Act & Assert
        assertThatThrownBy(() -> storageService.exists(uri))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("S3 service unavailable");

        verify(coreS3Service).exists(TEST_BUCKET, TEST_KEY);
    }

    // ========================================================================
    // DELETE OPERATIONS
    // ========================================================================

    @Test
    void shouldDeleteFromS3Successfully() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/to-delete.json");

        // Act
        Result<Void> result = storageService.delete(uri);

        // Assert
        assertThat(result.isSuccess()).isTrue();

        verify(coreS3Service).delete(TEST_BUCKET, "to-delete.json");
    }

    @Test
    void shouldHandleDeleteFailure() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/protected.json");
        doThrow(new IOException("Access denied"))
            .when(coreS3Service).delete(TEST_BUCKET, "protected.json");

        // Act & Assert
        assertThatThrownBy(() -> storageService.delete(uri))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Access denied");

        verify(coreS3Service).delete(TEST_BUCKET, "protected.json");
    }

    // ========================================================================
    // PRESIGNED URL OPERATIONS
    // ========================================================================

    @Test
    void shouldGeneratePresignedUrlSuccessfully() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/report.pdf");
        Duration expiration = Duration.ofHours(1);
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/report.pdf?X-Amz-Signature=...";

        when(coreS3Service.generatePresignedUrl(TEST_BUCKET, "report.pdf", expiration))
            .thenReturn(expectedUrl);

        // Act
        Result<PresignedUrl> result = storageService.generatePresignedUrl(uri, expiration);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().url()).isEqualTo(expectedUrl);
        assertThat(result.getValue().expiresAt()).isAfter(java.time.Instant.now());

        verify(coreS3Service).generatePresignedUrl(TEST_BUCKET, "report.pdf", expiration);
    }

    @Test
    void shouldHandlePresignedUrlGenerationFailure() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/report.pdf");
        Duration expiration = Duration.ofHours(1);

        when(coreS3Service.generatePresignedUrl(TEST_BUCKET, "report.pdf", expiration))
            .thenThrow(new IOException("S3 service error"));

        // Act & Assert
        assertThatThrownBy(() -> storageService.generatePresignedUrl(uri, expiration))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("S3 service error");

        verify(coreS3Service).generatePresignedUrl(TEST_BUCKET, "report.pdf", expiration);
    }

    // ========================================================================
    // METADATA OPERATIONS
    // ========================================================================

    @Test
    void shouldRetrieveMetadataFromS3Successfully() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/file-with-metadata.json");
        Map<String, String> expectedMetadata = Map.of(
            "batchId", "batch-789",
            "uploadedBy", "user-123",
            "contentType", "application/json"
        );

        when(coreS3Service.getMetadata(TEST_BUCKET, "file-with-metadata.json"))
            .thenReturn(expectedMetadata);

        // Act
        Result<Map<String, String>> result = storageService.getMetadata(uri);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(expectedMetadata);

        verify(coreS3Service).getMetadata(TEST_BUCKET, "file-with-metadata.json");
    }

    @Test
    void shouldHandleMetadataRetrievalFailure() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/no-metadata.json");
        when(coreS3Service.getMetadata(TEST_BUCKET, "no-metadata.json"))
            .thenThrow(new IOException("Metadata not available"));

        // Act & Assert
        assertThatThrownBy(() -> storageService.getMetadata(uri))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Metadata not available");

        verify(coreS3Service).getMetadata(TEST_BUCKET, "no-metadata.json");
    }

    // ========================================================================
    // EDGE CASES & VALIDATION
    // ========================================================================

    @Test
    void shouldRejectNullContent() {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/test.json");

        // Act
        Result<StorageResult> result = storageService.upload(null, uri, Map.of());

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isPresent();
        assertThat(result.getError().get().getErrorType()).isEqualTo(ErrorType.VALIDATION_ERROR);
        assertThat(result.getError().get().getMessage()).contains("Content cannot be null");

        verify(coreS3Service, never()).putString(anyString(), anyString(), anyString(), anyString(), anyMap(), any());
    }

    @Test
    void shouldRejectEmptyContent() {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/test.json");

        // Act
        Result<StorageResult> result = storageService.upload("", uri, Map.of());

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isPresent();
        assertThat(result.getError().get().getErrorType()).isEqualTo(ErrorType.VALIDATION_ERROR);
        assertThat(result.getError().get().getMessage()).contains("Content cannot be empty");

        verify(coreS3Service, never()).putString(anyString(), anyString(), anyString(), anyString(), anyMap(), any());
    }

    @Test
    void shouldHandleNullMetadata() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/test.json");

        // Act
        Result<StorageResult> result = storageService.upload(TEST_CONTENT, uri, null);

        // Assert - should use empty map as fallback
        assertThat(result.isSuccess()).isTrue();

        verify(coreS3Service).putString(eq(TEST_BUCKET), eq(TEST_KEY), eq(TEST_CONTENT), 
            eq("application/json"), eq(Map.of()), isNull());
    }

    @Test
    void shouldHandleLargeContent() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/large-file.json");
        String largeContent = "x".repeat(1_000_000); // 1 MB
        Map<String, String> metadata = Map.of();

        // Act
        Result<StorageResult> result = storageService.upload(largeContent, uri, metadata);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getSizeInBytes()).isEqualTo(1_000_000);

        verify(coreS3Service).putString(TEST_BUCKET, "large-file.json", largeContent, 
            "application/json", metadata, null);
    }

    @Test
    void shouldHandleSpecialCharactersInKey() throws IOException {
        // Arrange
        StorageUri uri = StorageUri.parse("s3://test-bucket/reports/2024/batch%20123/result.json");
        Map<String, String> metadata = Map.of();

        // Act
        Result<StorageResult> result = storageService.upload(TEST_CONTENT, uri, metadata);

        // Assert
        assertThat(result.isSuccess()).isTrue();

        verify(coreS3Service).putString(eq(TEST_BUCKET), eq("reports/2024/batch%20123/result.json"), 
            eq(TEST_CONTENT), eq("application/json"), eq(metadata), isNull());
    }
}
