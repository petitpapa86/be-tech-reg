package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.application.integration.S3StorageService;
import com.bcbs239.regtech.dataquality.application.integration.events.BatchQualityCompletedEvent;
import com.bcbs239.regtech.dataquality.application.integration.events.BatchQualityFailedEvent;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidateBatchQualityCommandHandler Unit Tests")
class ValidateBatchQualityCommandHandlerTest {

    @Mock
    private IQualityReportRepository qualityReportRepository;
    
    @Mock
    private S3StorageService s3StorageService;
    
    @Mock
    private IIntegrationEventBus eventBus;
    
    private ValidateBatchQualityCommandHandler handler;
    
    private BatchId testBatchId;
    private BankId testBankId;
    private String testS3Uri;
    
    @BeforeEach
    void setUp() {
        handler = new ValidateBatchQualityCommandHandler(
            qualityReportRepository,
            s3StorageService,
            eventBus
        );
        
        testBatchId = BatchId.of("batch_batch-123");
        testBankId = BankId.of("bank-456");
        testS3Uri = "s3://bucket/path/data.json";
    }
    
    @Test
    @DisplayName("Should successfully validate batch with valid exposures")
    void shouldSuccessfullyValidateBatchWithValidExposures() {
        // Arrange
        ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
            testBatchId,
            testBankId,
            testS3Uri,
            10
        );
        
        List<ExposureRecord> exposures = createTestExposures(10);
        S3Reference s3Reference = S3Reference.of("bucket", "results/report.json", "v1");
        
        when(qualityReportRepository.existsByBatchId(testBatchId)).thenReturn(false);
        when(qualityReportRepository.save(any(QualityReport.class)))
            .thenAnswer(invocation -> Result.success(invocation.getArgument(0)));
        when(s3StorageService.downloadExposures(eq(testS3Uri), eq(10)))
            .thenReturn(Result.success(exposures));
        when(s3StorageService.storeDetailedResults(eq(testBatchId), any(ValidationResult.class), anyMap()))
            .thenReturn(Result.success(s3Reference));
        
        // Act
        Result<Void> result = handler.handle(command);
        
        // Assert
        assertTrue(result.isSuccess());
        verify(qualityReportRepository, times(2)).save(any(QualityReport.class));
        verify(s3StorageService).downloadExposures(testS3Uri, 10);
        verify(s3StorageService).storeDetailedResults(eq(testBatchId), any(ValidationResult.class), anyMap());
        verify(eventBus).publish(any(BatchQualityCompletedEvent.class));
    }
    
    @Test
    @DisplayName("Should handle idempotency - skip if report already exists")
    void shouldHandleIdempotency() {
        // Arrange
        ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
            testBatchId,
            testBankId,
            testS3Uri,
            0
        );
        
        when(qualityReportRepository.existsByBatchId(testBatchId)).thenReturn(true);
        
        // Act
        Result<Void> result = handler.handle(command);
        
        // Assert
        assertTrue(result.isSuccess());
        verify(qualityReportRepository).existsByBatchId(testBatchId);
        verify(qualityReportRepository, never()).save(any());
        verify(s3StorageService, never()).downloadExposures(anyString());
        verify(eventBus, never()).publish(any());
    }
    
    @Test
    @DisplayName("Should handle S3 download failure")
    void shouldHandleS3DownloadFailure() {
        // Arrange
        ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
            testBatchId,
            testBankId,
            testS3Uri,
            0
        );
        
        when(qualityReportRepository.existsByBatchId(testBatchId)).thenReturn(false);
        when(qualityReportRepository.save(any(QualityReport.class)))
            .thenAnswer(invocation -> Result.success(invocation.getArgument(0)));
        when(s3StorageService.downloadExposures(testS3Uri))
            .thenReturn(Result.failure("S3_DOWNLOAD_ERROR", ErrorType.SYSTEM_ERROR, 
                "Failed to download from S3", "s3.download"));
        
        // Act
        Result<Void> result = handler.handle(command);
        
        // Assert
        assertTrue(result.isFailure());
        assertEquals("S3_DOWNLOAD_ERROR", result.getError().map(e -> e.getCode()).orElse(null));
        verify(s3StorageService).downloadExposures(testS3Uri);
        verify(eventBus).publish(any(BatchQualityFailedEvent.class));
    }
    
    @Test
    @DisplayName("Should handle S3 storage failure after validation")
    void shouldHandleS3StorageFailure() {
        // Arrange
        ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
            testBatchId,
            testBankId,
            testS3Uri,
            0
        );
        
        List<ExposureRecord> exposures = createTestExposures(5);
        
        when(qualityReportRepository.existsByBatchId(testBatchId)).thenReturn(false);
        when(qualityReportRepository.save(any(QualityReport.class)))
            .thenAnswer(invocation -> Result.success(invocation.getArgument(0)));
        when(s3StorageService.downloadExposures(testS3Uri))
            .thenReturn(Result.success(exposures));
        when(s3StorageService.storeDetailedResults(eq(testBatchId), any(ValidationResult.class), anyMap()))
            .thenReturn(Result.failure("S3_STORAGE_ERROR", ErrorType.SYSTEM_ERROR, 
                "Failed to store results", "s3.storage"));
        
        // Act
        Result<Void> result = handler.handle(command);
        
        // Assert
        assertTrue(result.isFailure());
        assertEquals("S3_STORAGE_ERROR", result.getError().map(e -> e.getCode()).orElse(null));
        verify(eventBus).publish(any(BatchQualityFailedEvent.class));
    }
    
    @Test
    @DisplayName("Should handle repository save failure")
    void shouldHandleRepositorySaveFailure() {
        // Arrange
        ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
            testBatchId,
            testBankId,
            testS3Uri,
            0
        );
        
        when(qualityReportRepository.existsByBatchId(testBatchId)).thenReturn(false);
        when(qualityReportRepository.save(any(QualityReport.class)))
            .thenReturn(Result.failure("SAVE_ERROR", ErrorType.SYSTEM_ERROR, 
                "Failed to save report", "repository.save"));
        
        // Act
        Result<Void> result = handler.handle(command);
        
        // Assert
        assertTrue(result.isFailure());
        assertEquals("SAVE_ERROR", result.getError().map(e -> e.getCode()).orElse(null));
    }
    
    @Test
    @DisplayName("Should validate command with expected exposure count")
    void shouldValidateCommandWithExpectedCount() {
        // Arrange
        ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
            testBatchId,
            testBankId,
            testS3Uri,
            20
        );
        
        List<ExposureRecord> exposures = createTestExposures(20);
        S3Reference s3Reference = S3Reference.of("bucket", "results/report.json", "v1");
        
        when(qualityReportRepository.existsByBatchId(testBatchId)).thenReturn(false);
        when(qualityReportRepository.save(any(QualityReport.class)))
            .thenAnswer(invocation -> Result.success(invocation.getArgument(0)));
        when(s3StorageService.downloadExposures(eq(testS3Uri), eq(20)))
            .thenReturn(Result.success(exposures));
        when(s3StorageService.storeDetailedResults(eq(testBatchId), any(ValidationResult.class), anyMap()))
            .thenReturn(Result.success(s3Reference));
        
        // Act
        Result<Void> result = handler.handle(command);
        
        // Assert
        assertTrue(result.isSuccess());
        verify(s3StorageService).downloadExposures(testS3Uri, 20);
    }
    
    @Test
    @DisplayName("Should validate command without expected exposure count")
    void shouldValidateCommandWithoutExpectedCount() {
        // Arrange
        ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
            testBatchId,
            testBankId,
            testS3Uri,
            0
        );
        
        List<ExposureRecord> exposures = createTestExposures(15);
        S3Reference s3Reference = S3Reference.of("bucket", "results/report.json", "v1");
        
        when(qualityReportRepository.existsByBatchId(testBatchId)).thenReturn(false);
        when(qualityReportRepository.save(any(QualityReport.class)))
            .thenAnswer(invocation -> Result.success(invocation.getArgument(0)));
        when(s3StorageService.downloadExposures(testS3Uri))
            .thenReturn(Result.success(exposures));
        when(s3StorageService.storeDetailedResults(eq(testBatchId), any(ValidationResult.class), anyMap()))
            .thenReturn(Result.success(s3Reference));
        
        // Act
        Result<Void> result = handler.handle(command);
        
        // Assert
        assertTrue(result.isSuccess());
        verify(s3StorageService).downloadExposures(testS3Uri);
        verify(s3StorageService, never()).downloadExposures(anyString(), anyInt());
    }
    
    @Test
    @DisplayName("Should publish BatchQualityCompletedEvent with correct data")
    void shouldPublishCompletedEventWithCorrectData() {
        // Arrange
        ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
            testBatchId,
            testBankId,
            testS3Uri,
            0
        );
        
        List<ExposureRecord> exposures = createTestExposures(10);
        S3Reference s3Reference = S3Reference.of("bucket", "results/report.json", "v1");
        
        when(qualityReportRepository.existsByBatchId(testBatchId)).thenReturn(false);
        when(qualityReportRepository.save(any(QualityReport.class)))
            .thenAnswer(invocation -> Result.success(invocation.getArgument(0)));
        when(s3StorageService.downloadExposures(testS3Uri))
            .thenReturn(Result.success(exposures));
        when(s3StorageService.storeDetailedResults(eq(testBatchId), any(ValidationResult.class), anyMap()))
            .thenReturn(Result.success(s3Reference));
        
        ArgumentCaptor<BatchQualityCompletedEvent> eventCaptor = 
            ArgumentCaptor.forClass(BatchQualityCompletedEvent.class);
        
        // Act
        Result<Void> result = handler.handle(command);
        
        // Assert
        assertTrue(result.isSuccess());
        verify(eventBus).publish(eventCaptor.capture());
        
        BatchQualityCompletedEvent event = eventCaptor.getValue();
        assertEquals(testBatchId, event.getBatchId());
        assertEquals(testBankId, event.getBankId());
        assertNotNull(event.getQualityScores());
    }
    
    @Test
    @DisplayName("Should publish BatchQualityFailedEvent on validation failure")
    void shouldPublishFailedEventOnValidationFailure() {
        // Arrange
        ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
            testBatchId,
            testBankId,
            testS3Uri,
            0
        );
        
        when(qualityReportRepository.existsByBatchId(testBatchId)).thenReturn(false);
        when(qualityReportRepository.save(any(QualityReport.class)))
            .thenAnswer(invocation -> Result.success(invocation.getArgument(0)));
        when(s3StorageService.downloadExposures(testS3Uri))
            .thenReturn(Result.failure("DOWNLOAD_ERROR", ErrorType.SYSTEM_ERROR, 
                "Download failed", "s3.download"));
        
        ArgumentCaptor<BatchQualityFailedEvent> eventCaptor = 
            ArgumentCaptor.forClass(BatchQualityFailedEvent.class);
        
        // Act
        Result<Void> result = handler.handle(command);
        
        // Assert
        assertTrue(result.isFailure());
        verify(eventBus).publish(eventCaptor.capture());
        
        BatchQualityFailedEvent event = eventCaptor.getValue();
        assertEquals(testBatchId, event.getBatchId());
        assertEquals(testBankId, event.getBankId());
        assertNotNull(event.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should handle empty exposure list")
    void shouldHandleEmptyExposureList() {
        // Arrange
        ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
            testBatchId,
            testBankId,
            testS3Uri,
            0
        );
        
        List<ExposureRecord> exposures = Collections.emptyList();
        
        when(qualityReportRepository.existsByBatchId(testBatchId)).thenReturn(false);
        when(qualityReportRepository.save(any(QualityReport.class)))
            .thenAnswer(invocation -> Result.success(invocation.getArgument(0)));
        when(s3StorageService.downloadExposures(testS3Uri))
            .thenReturn(Result.success(exposures));
        // Note: storeDetailedResults is not stubbed as the handler fails before reaching that point
        
        // Act
        Result<Void> result = handler.handle(command);
        
        // Assert
        assertTrue(result.isFailure());
        verify(eventBus).publish(any(BatchQualityFailedEvent.class));
    }
    
    // Helper methods
    
    private List<ExposureRecord> createTestExposures(int count) {
        List<ExposureRecord> exposures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            exposures.add(createTestExposure("exp-" + i));
        }
        return exposures;
    }
    
    private ExposureRecord createTestExposure(String exposureId) {
        return ExposureRecord.builder()
            .exposureId(exposureId)
            .counterpartyId("CP-123")
            .amount(BigDecimal.valueOf(1000000))
            .currency("USD")
            .country("USA")
            .sector("Financial")
            .counterpartyType("Bank")
            .productType("Loan")
            .leiCode("LEI123456789")
            .internalRating("A")
            .riskCategory("Low")
            .riskWeight(BigDecimal.valueOf(0.5))
            .reportingDate(LocalDate.now())
            .valuationDate(LocalDate.now())
            .maturityDate(LocalDate.now().plusYears(1))
            .referenceNumber("REF-" + exposureId)
            .build();
    }
}
