package com.bcbs239.regtech.dataquality.domain.report;


import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.domain.quality.QualityGrade;
import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.dataquality.domain.report.events.*;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationSummary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QualityReportTest {
    
    private final BatchId batchId = BatchId.of("batch_123");
    private final BankId bankId = BankId.of("bank_456");
    private final String correlationId = "corr-789";
    
    @Test
    void createForBatch_ShouldCreateReportInPendingStatus() {
        // When
        QualityReport report = QualityReport.createForBatch(batchId, bankId);
        
        // Then
        assertNotNull(report.getReportId());
        assertEquals(batchId, report.getBatchId());
        assertEquals(bankId, report.getBankId());
        assertEquals(QualityStatus.PENDING, report.getStatus());
        assertNull(report.getScores());
        assertNotNull(report.getCreatedAt());
        assertNotNull(report.getUpdatedAt());
        assertTrue(report.canStartValidation());
    }
    
    @Test
    void startValidation_ShouldTransitionToInProgressAndRaiseEvent() {
        // Given
        QualityReport report = QualityReport.createForBatch(batchId, bankId);
        
        // When
        Result<Void> result = report.startValidation(correlationId);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(QualityStatus.IN_PROGRESS, report.getStatus());
        assertTrue(report.isInProgress());
        assertTrue(report.canRecordResults());
        
        // Verify domain event was raised
        assertEquals(1, report.getDomainEvents().size());
        assertTrue(report.getDomainEvents().get(0) instanceof QualityValidationStartedEvent);
        
        QualityValidationStartedEvent event = (QualityValidationStartedEvent) report.getDomainEvents().get(0);
        assertEquals(report.getReportId(), event.getReportId());
        assertEquals(batchId, event.getBatchId());
        assertEquals(bankId, event.getBankId());
    }
    
    @Test
    void startValidation_FromNonPendingStatus_ShouldFail() {
        // Given
        QualityReport report = QualityReport.createForBatch(batchId, bankId);
        report.startValidation(correlationId); // Move to IN_PROGRESS
        
        // When
        Result<Void> result = report.startValidation(correlationId);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("INVALID_STATE_TRANSITION", result.getError().get().getCode());
    }
    
    @Test
    void recordValidationResults_ShouldUpdateSummaryAndRaiseEvent() {
        // Given

        QualityReport report = QualityReport.createForBatch(batchId, bankId);
        report.startValidation(correlationId);
        
        ValidationSummary summary = ValidationSummary.builder()
            .totalExposures(100)
            .validExposures(95)
            .totalErrors(5)
            .build();
        
        ValidationResult validationResult = ValidationResult.builder()
            .summary(summary)
            .totalExposures(100)
            .validExposures(95)
            .build();
        
        // When
        Result<Void> result = report.recordValidationResults(validationResult,correlationId);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(summary, report.getValidationSummary());
        assertTrue(report.canCalculateScores());
        
        // Verify domain event was raised
        assertEquals(2, report.getDomainEvents().size()); // Start + Record events
        assertTrue(report.getDomainEvents().get(1) instanceof QualityResultsRecordedEvent);
    }
    
    @Test
    void calculateScores_ShouldUpdateScoresAndRaiseEvent() {
        // Given
        QualityReport report = QualityReport.createForBatch(batchId, bankId);
        report.startValidation(correlationId);
        
        ValidationResult validationResult = ValidationResult.builder()
            .summary(ValidationSummary.builder().totalExposures(100).validExposures(95).totalErrors(5).build())
            .totalExposures(100)
            .validExposures(95)
            .build();
        report.recordValidationResults(validationResult, correlationId);
        
        QualityScores scores = new QualityScores(95, 90, 85, 80, 75, 70, 85, QualityGrade.GOOD);
        
        // When
        Result<Void> result = report.calculateScores(scores, CorrelationContext.correlationId());
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(scores, report.getScores());
        
        // Verify domain event was raised
        assertEquals(3, report.getDomainEvents().size());
        assertTrue(report.getDomainEvents().get(2) instanceof QualityScoresCalculatedEvent);
    }
    
    @Test
    void completeValidation_WithAllRequiredData_ShouldSucceed() {
        // Given
        QualityReport report = setupCompleteReport();
        
        // When
        Result<Void> result = report.completeValidation("test-file.json");
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(QualityStatus.COMPLETED, report.getStatus());
        assertTrue(report.isCompleted());
        assertTrue(report.isTerminal());
        
        // Verify completion event was raised
        assertEquals(4, report.getDomainEvents().size());
        assertTrue(report.getDomainEvents().get(3) instanceof QualityValidationCompletedEvent);
    }
    
    @Test
    void completeValidation_WithoutScores_ShouldFail() {
        // Given
        QualityReport report = QualityReport.createForBatch(batchId, bankId);
        report.startValidation(correlationId);
        
        ValidationResult validationResult = ValidationResult.builder()
            .summary(ValidationSummary.builder().totalExposures(100).validExposures(95).totalErrors(5).build())
            .totalExposures(100)
            .validExposures(95)
            .build();
        report.recordValidationResults(validationResult, correlationId);
        
        // Store details but don't calculate scores
        S3Reference s3Reference = S3Reference.of("bucket", "quality/quality_batch_123.json", "v1");
        report.storeDetailedResults(s3Reference);
        
        // When
        Result<Void> result = report.completeValidation("test-file.json");
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("SCORES_NOT_CALCULATED", result.getError().get().getCode());
    }
    
    @Test
    void completeValidation_WithoutDetailsReference_ShouldFail() {
        // Given
        QualityReport report = QualityReport.createForBatch(batchId, bankId);
        report.startValidation(correlationId);
        
        ValidationResult validationResult = ValidationResult.builder()
            .summary(ValidationSummary.builder().totalExposures(100).validExposures(95).totalErrors(5).build())
            .totalExposures(100)
            .validExposures(95)
            .build();
        report.recordValidationResults(validationResult, correlationId);
        
        QualityScores scores = new QualityScores(95, 90, 85, 80, 75, 70, 85, QualityGrade.GOOD);
        report.calculateScores(scores, correlationId);
        
        // When (don't store details reference)
        Result<Void> result = report.completeValidation("test-file.json");
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("DETAILS_NOT_STORED", result.getError().get().getCode());
    }
    
    @Test
    void markAsFailed_ShouldTransitionToFailedAndRaiseEvent() {
        // Given
        QualityReport report = QualityReport.createForBatch(batchId, bankId);
        report.startValidation(correlationId);
        String errorMessage = "S3 download failed";
        
        // When
        Result<Void> result = report.markAsFailed(errorMessage, correlationId);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(QualityStatus.FAILED, report.getStatus());
        assertEquals(errorMessage, report.getErrorMessage());
        assertTrue(report.isFailed());
        assertTrue(report.isTerminal());
        
        // Verify failure event was raised
        assertEquals(2, report.getDomainEvents().size());
        assertTrue(report.getDomainEvents().get(1) instanceof QualityValidationFailedEvent);
        
        QualityValidationFailedEvent event = (QualityValidationFailedEvent) report.getDomainEvents().get(1);
        assertEquals(errorMessage, event.getErrorMessage());
    }
    
    @Test
    void markAsFailed_FromTerminalStatus_ShouldFail() {
        // Given
        QualityReport report = setupCompleteReport();
        report.completeValidation("test-file.json"); // Move to COMPLETED (terminal)
        
        // When
        Result<Void> result = report.markAsFailed("Some error", correlationId);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("INVALID_STATE_TRANSITION", result.getError().get().getCode());
    }
    
    @Test
    void isCompliant_WithCompliantScores_ShouldReturnTrue() {
        // Given
        QualityReport report = setupCompleteReport();
        QualityScores compliantScores = new QualityScores(95, 90, 85, 80, 75, 70, 85, QualityGrade.GOOD);
        report.calculateScores(compliantScores, correlationId);
        
        // When & Then
        assertTrue(report.isCompliant());
    }
    
    @Test
    void requiresAttention_WithPoorScores_ShouldReturnTrue() {
        // Given
        QualityReport report = setupCompleteReport();
        QualityScores poorScores = new QualityScores(60, 55, 50, 45, 40, 35, 50, QualityGrade.POOR);
        report.calculateScores(poorScores, correlationId);
        
        // When & Then
        assertTrue(report.requiresAttention());
    }

    @Test
    void toFrontendPresentation_ErrorRateShouldNeverExceed100Percent() {
        // Given: 1 exposure with 2 errors (same exposure)
        QualityReport report = QualityReport.createForBatch(batchId, bankId);

        ValidationSummary summary = new ValidationSummary(
            1,  // totalExposures
            0,  // validExposures
            1,  // invalidExposures
            2,  // totalErrors
            Map.of(QualityDimension.COMPLETENESS, 2),
            Map.of(),
            Map.of(),
            0.0
        );

        report.setValidationSummary(summary);
        report.setScores(new QualityScores(0, 100, 100, 100, 100, 100, 75, QualityGrade.POOR));

        // When
        QualityReportPresentation presentation = report.toFrontendPresentation(List.of());

        // Then
        assertNotNull(presentation);
        assertFalse(presentation.violations().isEmpty());

        var violation = presentation.violations().stream()
            .filter(v -> v.title() != null && v.title().contains("Completezza"))
            .findFirst()
            .orElseThrow();

        assertTrue(violation.title().contains("2"));
        assertTrue(violation.description().contains("100,00%"));
        assertFalse(violation.description().contains("200,00%"));
        assertEquals("1", violation.details().get("Record Interessati"));
        assertEquals("2", violation.details().get("Errori"));
    }
    
    private QualityReport setupCompleteReport() {
        QualityReport report = QualityReport.createForBatch(batchId, bankId);
        report.startValidation(correlationId);
        
        ValidationResult validationResult = ValidationResult.builder()
            .summary(ValidationSummary.builder().totalExposures(100).validExposures(95).totalErrors(5).build())
            .totalExposures(100)
            .validExposures(95)
            .build();
        report.recordValidationResults(validationResult, correlationId);
        
        QualityScores scores = new QualityScores(95, 90, 85, 80, 75, 70, 85, QualityGrade.GOOD);
        report.calculateScores(scores, correlationId);
        
        S3Reference s3Reference = S3Reference.of("bucket", "quality/quality_batch_123.json", "v1");
        report.storeDetailedResults(s3Reference);
        
        return report;
    }
}

