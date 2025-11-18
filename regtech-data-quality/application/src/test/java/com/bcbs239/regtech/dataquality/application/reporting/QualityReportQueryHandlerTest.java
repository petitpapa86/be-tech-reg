package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QualityReportQueryHandler Unit Tests")
class QualityReportQueryHandlerTest {

    @Mock
    private IQualityReportRepository qualityReportRepository;
    
    private QualityReportQueryHandler handler;
    
    @BeforeEach
    void setUp() {
        handler = new QualityReportQueryHandler(qualityReportRepository);
    }
    
    @Test
    @DisplayName("Should successfully retrieve quality report")
    void shouldSuccessfullyRetrieveQualityReport() {
        // Arrange
        BatchId batchId = BatchId.of("batch_batch-123");
        GetQualityReportQuery query = new GetQualityReportQuery(batchId);
        QualityReport report = QualityReport.createForBatch(batchId, com.bcbs239.regtech.dataquality.domain.shared.BankId.of("bank-1"));
        
        when(qualityReportRepository.findByBatchId(batchId)).thenReturn(Optional.of(report));
        
        // Act
        Result<QualityReportDto> result = handler.handle(query);
        
        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getValueOrThrow());
        verify(qualityReportRepository).findByBatchId(batchId);
    }
    
    @Test
    @DisplayName("Should handle report not found")
    void shouldHandleReportNotFound() {
        // Arrange
        BatchId batchId = BatchId.of("batch_batch-456");
        GetQualityReportQuery query = new GetQualityReportQuery(batchId);
        
        when(qualityReportRepository.findByBatchId(batchId)).thenReturn(Optional.empty());
        
        // Act
        Result<QualityReportDto> result = handler.handle(query);
        
        // Assert
        assertTrue(result.isFailure());
        assertEquals("QUALITY_REPORT_NOT_FOUND", result.getError().map(e -> e.getCode()).orElse(null));
    }
    
    @Test
    @DisplayName("Should check if report exists")
    void shouldCheckIfReportExists() {
        // Arrange
        BatchId batchId = BatchId.of("batch_batch-789");
        GetQualityReportQuery query = new GetQualityReportQuery(batchId);
        
        when(qualityReportRepository.existsByBatchId(batchId)).thenReturn(true);
        
        // Act
        Result<Boolean> result = handler.exists(query);
        
        // Assert
        assertTrue(result.isSuccess());
        assertTrue(result.getValueOrThrow());
        verify(qualityReportRepository).existsByBatchId(batchId);
    }
}
