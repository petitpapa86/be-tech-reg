package com.bcbs239.regtech.modules.dataquality.presentation.web;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.modules.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.modules.dataquality.presentation.web.QualityRequestValidator.TrendsQueryParams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServerHttpRequest;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for QualityRequestValidator.
 */
class QualityRequestValidatorTest {
    
    private QualityRequestValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new QualityRequestValidator();
    }
    
    @Test
    void shouldValidateValidBatchId() {
        // Given
        String validBatchId = "batch_20241103_120000_uuid";
        
        // When
        Result<BatchId> result = validator.validateBatchId(validBatchId);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().orElseThrow().value()).isEqualTo(validBatchId);
    }
    
    @Test
    void shouldRejectNullBatchId() {
        // When
        Result<BatchId> result = validator.validateBatchId(null);
        
        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().orElseThrow().hasFieldErrors()).isTrue();
    }
    
    @Test
    void shouldRejectEmptyBatchId() {
        // When
        Result<BatchId> result = validator.validateBatchId("");
        
        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().orElseThrow().hasFieldErrors()).isTrue();
    }
    
    @Test
    void shouldRejectInvalidBatchIdFormat() {
        // When
        Result<BatchId> result = validator.validateBatchId("invalid-format");
        
        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().orElseThrow().hasFieldErrors()).isTrue();
    }
    
    @Test
    void shouldParseValidTrendsQueryParams() {
        // Given
        MockServerRequest request = MockServerRequest.builder()
            .uri("http://localhost/api/v1/data-quality/trends?days=7&limit=50")
            .build();
        
        // When
        Result<TrendsQueryParams> result = validator.parseTrendsQueryParams(request);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        TrendsQueryParams params = result.getValue().orElseThrow();
        assertThat(params.limit()).isEqualTo(50);
    }
    
    @Test
    void shouldUseDefaultsForMissingQueryParams() {
        // Given
        MockServerRequest request = MockServerRequest.builder()
            .uri("http://localhost/api/v1/data-quality/trends")
            .build();
        
        // When
        Result<TrendsQueryParams> result = validator.parseTrendsQueryParams(request);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        TrendsQueryParams params = result.getValue().orElseThrow();
        assertThat(params.limit()).isEqualTo(100); // default
    }
    
    @Test
    void shouldRejectInvalidDaysParameter() {
        // Given
        MockServerRequest request = MockServerRequest.builder()
            .uri("http://localhost/api/v1/data-quality/trends?days=-1")
            .build();
        
        // When
        Result<TrendsQueryParams> result = validator.parseTrendsQueryParams(request);
        
        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().orElseThrow().hasFieldErrors()).isTrue();
    }
    
    @Test
    void shouldRejectExcessiveDaysParameter() {
        // Given
        MockServerRequest request = MockServerRequest.builder()
            .uri("http://localhost/api/v1/data-quality/trends?days=400")
            .build();
        
        // When
        Result<TrendsQueryParams> result = validator.parseTrendsQueryParams(request);
        
        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().orElseThrow().hasFieldErrors()).isTrue();
    }
}