package com.bcbs239.regtech.reportgeneration.application.generation;

import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.storage.IStorageService;
import com.bcbs239.regtech.core.domain.storage.StorageUri;
import com.bcbs239.regtech.reportgeneration.application.coordination.CalculationEventData;
import com.bcbs239.regtech.reportgeneration.domain.generation.CalculationResults;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComprehensiveReportDataAggregatorTest {

    @Mock
    private IStorageService storageService;

    @Mock
    private ObjectMapper objectMapper;

    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private ComprehensiveReportDataAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new ComprehensiveReportDataAggregator(storageService, objectMapper, meterRegistry);
    }

    @Test
    void fetchCalculationData_ReturnsFailure_WhenUriIsInvalid() {
        // Arrange
        CalculationEventData event = mock(CalculationEventData.class);
        when(event.getBatchId()).thenReturn("batch-123");
        // Use an unsupported scheme to trigger IllegalArgumentException in StorageUri.parse
        when(event.getResultFileUri()).thenReturn("http://invalid-uri");

        // Act
        Result<CalculationResults> result = aggregator.fetchCalculationData(event);

        // Assert
        assertTrue(result.isFailure());
        assertEquals("INVALID_STORAGE_URI", result.getError().get().getCode());
        assertEquals(ErrorType.VALIDATION_ERROR, result.getError().get().getErrorType());
    }

    @Test
    void fetchCalculationData_ReturnsFailure_WhenDownloadFails() throws IOException {
        // Arrange
        CalculationEventData event = mock(CalculationEventData.class);
        when(event.getBatchId()).thenReturn("batch-123");
        when(event.getResultFileUri()).thenReturn("s3://bucket/key");
        
        // Mock storage service to throw IOException (simulating the try-catch block in aggregator)
        // Note: The aggregator catches IOException. But storageService.download returns Result<String>.
        // If storageService.download() throws IOException, the catch block works.
        // If storageService.download() returns Result.failure, we need to handle that.
        // Looking at the code, aggregator calls: storageService.download(uri).flatMap(...)
        // And catches IOException.
        // So we simulate IOException from storageService.download(uri).
        when(storageService.download(any(StorageUri.class))).thenThrow(new IOException("Download failed"));

        // Act
        Result<CalculationResults> result = aggregator.fetchCalculationData(event);

        // Assert
        assertTrue(result.isFailure());
        assertEquals("STORAGE_IO_ERROR", result.getError().get().getCode());
        assertEquals(ErrorType.SYSTEM_ERROR, result.getError().get().getErrorType());
    }

    @Test
    void fetchCalculationData_ReturnsFailure_WhenJsonIsInvalid() throws IOException {
        // Arrange
        CalculationEventData event = mock(CalculationEventData.class);
        when(event.getBatchId()).thenReturn("batch-123");
        when(event.getResultFileUri()).thenReturn("s3://bucket/key");
        
        // Mock successful download
        when(storageService.download(any(StorageUri.class))).thenReturn(Result.success("invalid json"));
        
        // Mock ObjectMapper to throw exception on invalid JSON
        when(objectMapper.readTree("invalid json")).thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "Invalid JSON"));

        // Act
        Result<CalculationResults> result = aggregator.fetchCalculationData(event);

        // Assert
        assertTrue(result.isFailure());
        assertEquals("JSON_PARSE_ERROR", result.getError().get().getCode());
        assertEquals(ErrorType.SYSTEM_ERROR, result.getError().get().getErrorType());
    }
}
