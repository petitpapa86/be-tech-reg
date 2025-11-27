package com.bcbs239.regtech.riskcalculation.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileStorageServiceTest {
    
    @TempDir
    Path tempDir;
    
    private LocalFileStorageService service;
    
    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new LocalFileStorageService(objectMapper);
        // Use reflection to set the basePath for testing
        try {
            var field = LocalFileStorageService.class.getDeclaredField("basePath");
            field.setAccessible(true);
            field.set(service, tempDir.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set basePath", e);
        }
    }
    
    @Test
    void shouldStoreAndRetrieveCalculationResults() {
        // Given
        BatchId batchId = new BatchId("BATCH001");
        String content = "{\"test\":\"data\"}";
        
        // When - Store
        Result<FileStorageUri> storeResult = service.storeCalculationResults(batchId, content);
        
        // Then - Store successful
        assertThat(storeResult.isSuccess()).isTrue();
        FileStorageUri uri = storeResult.getValue().get();
        assertThat(uri.uri()).contains("BATCH001");
        assertThat(uri.uri()).startsWith("file://");
        
        // When - Download
        Result<String> downloadResult = service.downloadFileContent(uri);
        
        // Then - Download successful
        assertThat(downloadResult.isSuccess()).isTrue();
        assertThat(downloadResult.getValue().get()).isEqualTo(content);
    }
    
    @Test
    void shouldReturnErrorWhenFileNotFound() {
        // Given
        FileStorageUri nonExistentUri = new FileStorageUri("file://" + tempDir.resolve("nonexistent.json"));
        
        // When
        Result<String> result = service.downloadFileContent(nonExistentUri);
        
        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().get().getCode()).isEqualTo("LOCAL_FILE_NOT_FOUND");
    }
    
    @Test
    void shouldPassHealthCheck() {
        // When
        Result<Boolean> result = service.checkServiceHealth();
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().get()).isTrue();
    }
}
