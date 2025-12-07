package com.bcbs239.regtech.riskcalculation.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.persistence.BatchRepository;
import com.bcbs239.regtech.riskcalculation.domain.services.IFileStorageService;
import com.bcbs239.regtech.riskcalculation.domain.storage.CalculationResultsImmutabilityException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for JSON file immutability enforcement in CalculationResultsStorageServiceImpl.
 * 
 * Validates Requirements:
 * - 8.1: Ensure JSON files are immutable (write-once)
 * - 8.4: Do not modify or overwrite existing JSON files
 */
@ExtendWith(MockitoExtension.class)
class CalculationResultsImmutabilityTest {
    
    @Mock
    private IFileStorageService fileStorageService;
    
    @Mock
    private BatchRepository batchRepository;
    
    private CalculationResultsStorageServiceImpl storageService;
    
    private static final String BATCH_ID = "batch_20241207_123456";
    private static final String EXISTING_URI = "s3://bucket/risk_calc_batch_20241207_123456_20241207_120000.json";
    private static final String JSON_CONTENT = "{\"format_version\":\"1.0\",\"batch_id\":\"" + BATCH_ID + "\"}";
    
    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        storageService = new CalculationResultsStorageServiceImpl(
            fileStorageService,
            batchRepository,
            objectMapper
        );
    }
    
    @Test
    void shouldThrowExceptionWhenAttemptingToOverwriteExistingResults() {
        // Given: Batch already has calculation results stored
        when(batchRepository.getCalculationResultsUri(BATCH_ID))
            .thenReturn(Optional.of(EXISTING_URI));
        
        // When/Then: Attempting to store results again should throw immutability exception
        assertThatThrownBy(() -> storageService.storeCalculationResults(JSON_CONTENT, BATCH_ID))
            .isInstanceOf(CalculationResultsImmutabilityException.class)
            .hasMessageContaining("Calculation results already exist for batch " + BATCH_ID)
            .hasMessageContaining("immutable")
            .hasMessageContaining(EXISTING_URI);
        
        // Verify: File storage service was never called
        verify(fileStorageService, never()).storeFile(anyString(), anyString());
    }
    
    @Test
    void shouldStoreResultsWhenNoExistingResultsFound() {
        // Given: Batch has no existing calculation results
        when(batchRepository.getCalculationResultsUri(BATCH_ID))
            .thenReturn(Optional.empty());
        
        String newUri = "s3://bucket/risk_calc_batch_20241207_123456_20241207_130000.json";
        when(fileStorageService.storeFile(anyString(), anyString()))
            .thenReturn(Result.success(newUri));
        
        // When: Storing calculation results
        Result<String> result = storageService.storeCalculationResults(JSON_CONTENT, BATCH_ID);
        
        // Then: Storage should succeed
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isEqualTo(newUri);
        
        // Verify: File storage service was called
        verify(fileStorageService).storeFile(anyString(), anyString());
    }
    
    @Test
    void immutabilityExceptionShouldContainBatchIdAndExistingUri() {
        // Given: Batch already has calculation results stored
        when(batchRepository.getCalculationResultsUri(BATCH_ID))
            .thenReturn(Optional.of(EXISTING_URI));
        
        // When/Then: Exception should contain batch ID and existing URI
        assertThatThrownBy(() -> storageService.storeCalculationResults(JSON_CONTENT, BATCH_ID))
            .isInstanceOf(CalculationResultsImmutabilityException.class)
            .satisfies(exception -> {
                CalculationResultsImmutabilityException immutabilityException = 
                    (CalculationResultsImmutabilityException) exception;
                assertThat(immutabilityException.getBatchId()).isEqualTo(BATCH_ID);
                assertThat(immutabilityException.getExistingUri()).isEqualTo(EXISTING_URI);
            });
    }
    
    @Test
    void shouldAllowStorageForDifferentBatchIds() {
        // Given: First batch has no existing results
        String batchId1 = "batch_20241207_123456";
        String batchId2 = "batch_20241207_234567";
        
        when(batchRepository.getCalculationResultsUri(batchId1))
            .thenReturn(Optional.empty());
        when(batchRepository.getCalculationResultsUri(batchId2))
            .thenReturn(Optional.empty());
        
        String uri1 = "s3://bucket/risk_calc_batch_20241207_123456_20241207_130000.json";
        String uri2 = "s3://bucket/risk_calc_batch_20241207_234567_20241207_140000.json";
        
        when(fileStorageService.storeFile(anyString(), anyString()))
            .thenReturn(Result.success(uri1))
            .thenReturn(Result.success(uri2));
        
        // When: Storing results for both batches
        Result<String> result1 = storageService.storeCalculationResults(JSON_CONTENT, batchId1);
        Result<String> result2 = storageService.storeCalculationResults(JSON_CONTENT, batchId2);
        
        // Then: Both should succeed
        assertThat(result1.isSuccess()).isTrue();
        assertThat(result2.isSuccess()).isTrue();
    }
}
