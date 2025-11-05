package com.bcbs239.regtech.modules.dataquality.application.validation;

import com.bcbs239.regtech.dataquality.application.reporting.QualityReportDto;
import com.bcbs239.regtech.dataquality.application.scoring.QualityScoresDto;
import com.bcbs239.regtech.dataquality.application.validation.ValidationSummaryDto;
import com.bcbs239.regtech.dataquality.domain.quality.DimensionScores;
import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.quality.QualityGrade;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify DTO mapping functionality.
 * Tests the mapping between domain objects and DTOs as required by task 4.3.
 */
class DtoMappingTest {
    
    @Test
    @DisplayName("Should map QualityScores domain to QualityScoresDto correctly")
    void shouldMapQualityScoresToDto() {
        // Given
        QualityScores domainScores = new QualityScores(
            85.0, 90.0, 80.0, 75.0, 95.0, 88.0, 85.5, QualityGrade.GOOD
        );
        
        // When
        QualityScoresDto dto = QualityScoresDto.fromDomain(domainScores);
        
        // Then
        assertNotNull(dto);
        assertEquals(85.0, dto.completenessScore());
        assertEquals(90.0, dto.accuracyScore());
        assertEquals(80.0, dto.consistencyScore());
        assertEquals(75.0, dto.timelinessScore());
        assertEquals(95.0, dto.uniquenessScore());
        assertEquals(88.0, dto.validityScore());
        assertEquals(85.5, dto.overallScore());
        assertEquals("GOOD", dto.grade());
        assertTrue(dto.isCompliant());
        assertFalse(dto.requiresAttention());
    }
    
    @Test
    @DisplayName("Should map QualityScoresDto back to domain correctly")
    void shouldMapQualityScoresDtoToDomain() {
        // Given
        QualityScoresDto dto = new QualityScoresDto(
            85.0, 90.0, 80.0, 75.0, 95.0, 88.0, 85.5, "GOOD", true, false
        );
        
        // When
        QualityScores domain = dto.toDomain();
        
        // Then
        assertNotNull(domain);
        assertEquals(85.0, domain.completenessScore());
        assertEquals(90.0, domain.accuracyScore());
        assertEquals(80.0, domain.consistencyScore());
        assertEquals(75.0, domain.timelinessScore());
        assertEquals(95.0, domain.uniquenessScore());
        assertEquals(88.0, domain.validityScore());
        assertEquals(85.5, domain.overallScore());
        assertEquals(QualityGrade.GOOD, domain.grade());
    }
    
    @Test
    @DisplayName("Should map ValidationSummary domain to ValidationSummaryDto correctly")
    void shouldMapValidationSummaryToDto() {
        // Given
        ValidationSummary domainSummary = ValidationSummary.builder()
            .totalExposures(1000)
            .validExposures(850)
            .totalErrors(150)
            .errorsByDimension(Map.of(
                QualityDimension.COMPLETENESS, 50,
                QualityDimension.ACCURACY, 100
            ))
            .errorsBySeverity(Map.of(
                ValidationError.ErrorSeverity.CRITICAL, 25,
                ValidationError.ErrorSeverity.HIGH, 125
            ))
            .errorsByCode(Map.of(
                "COMPLETENESS_MISSING_FIELD", 30,
                "ACCURACY_INVALID_FORMAT", 70
            ))
            .build();
        
        // When
        ValidationSummaryDto dto = ValidationSummaryDto.fromDomain(domainSummary);
        
        // Then
        assertNotNull(dto);
        assertEquals(1000, dto.totalExposures());
        assertEquals(850, dto.validExposures());
        assertEquals(150, dto.invalidExposures());
        assertEquals(150, dto.totalErrors());
        assertEquals(85.0, dto.validationRate());
        
        // Check dimension mapping
        assertEquals(50, dto.errorsByDimension().get("COMPLETENESS"));
        assertEquals(100, dto.errorsByDimension().get("ACCURACY"));
        
        // Check severity mapping
        assertEquals(25, dto.errorsBySeverity().get("CRITICAL"));
        assertEquals(125, dto.errorsBySeverity().get("HIGH"));
        
        // Check error codes
        assertEquals(30, dto.errorsByCode().get("COMPLETENESS_MISSING_FIELD"));
        assertEquals(70, dto.errorsByCode().get("ACCURACY_INVALID_FORMAT"));
    }
    
    @Test
    @DisplayName("Should map QualityReport domain to QualityReportDto correctly")
    void shouldMapQualityReportToDto() {
        // Given
        QualityReport domainReport = QualityReport.createForBatch(
            BatchId.of("batch_20241103_120000_test"),
            BankId.of("BANK001")
        );
        
        // Simulate completed report
        QualityScores scores = new QualityScores(
            85.0, 90.0, 80.0, 75.0, 95.0, 88.0, 85.5, QualityGrade.GOOD
        );
        
        ValidationSummary summary = ValidationSummary.builder()
            .totalExposures(1000)
            .validExposures(850)
            .totalErrors(150)
            .build();
        
        S3Reference s3Ref = S3Reference.of("test-bucket", "quality/test.json", "v1");
        
        domainReport.startValidation();
        domainReport.recordValidationResults(createMockValidationResult(summary));
        domainReport.calculateScores(scores);
        domainReport.storeDetailedResults(s3Ref);
        domainReport.completeValidation();
        
        // When
        QualityReportDto dto = QualityReportDto.fromDomain(domainReport);
        
        // Then
        assertNotNull(dto);
        assertEquals(domainReport.getReportId().value(), dto.reportId());
        assertEquals("batch_20241103_120000_test", dto.batchId());
        assertEquals("BANK001", dto.bankId());
        assertEquals("COMPLETED", dto.status());
        assertNotNull(dto.scores());
        assertNotNull(dto.validationSummary());
        assertEquals("s3://test-bucket/quality/test.json", dto.detailsUri());
        assertTrue(dto.isCompleted());
        assertTrue(dto.isCompliant());
        assertEquals(85.5, dto.getOverallScore());
        assertEquals("B", dto.getGrade());
    }
    
    @Test
    @DisplayName("Should handle null domain objects gracefully")
    void shouldHandleNullDomainObjects() {
        // When/Then
        assertNull(QualityScoresDto.fromDomain(null));
        assertNull(QualityReportDto.fromDomain(null));
        assertEquals(ValidationSummaryDto.empty(), ValidationSummaryDto.fromDomain(null));
    }
    
    @Test
    @DisplayName("Should provide empty DTOs correctly")
    void shouldProvideEmptyDtos() {
        // When
        QualityScoresDto emptyScores = QualityScoresDto.empty();
        ValidationSummaryDto emptySummary = ValidationSummaryDto.empty();
        
        // Then
        assertNotNull(emptyScores);
        assertEquals(0.0, emptyScores.overallScore());
        assertEquals("POOR", emptyScores.grade());
        assertFalse(emptyScores.isCompliant());
        assertTrue(emptyScores.requiresAttention());
        
        assertNotNull(emptySummary);
        assertEquals(0, emptySummary.totalExposures());
        assertEquals(0, emptySummary.validExposures());
        assertEquals(0.0, emptySummary.validationRate());
    }
    
    @Test
    @DisplayName("Should provide utility methods for DTOs")
    void shouldProvideUtilityMethods() {
        // Given
        QualityScoresDto scores = new QualityScoresDto(
            85.0, 90.0, 80.0, 75.0, 95.0, 88.0, 85.5, "GOOD", true, false
        );
        
        ValidationSummaryDto summary = new ValidationSummaryDto(
            1000, 850, 150, 150, 85.0,
            Map.of("COMPLETENESS", 50),
            Map.of("CRITICAL", 25),
            Map.of("MISSING_FIELD", 30),
            Map.of("MISSING_FIELD", 30)
        );
        
        // When/Then
        assertEquals("B", scores.getGradeDisplay());
        assertEquals("COMPLIANT", scores.getComplianceStatus());
        assertEquals("LOW", scores.getAttentionLevel());
        assertEquals("completeness", scores.getLowestScoringDimension());
        assertEquals("uniqueness", scores.getHighestScoringDimension());
        
        assertEquals("85.0%", summary.getValidationRatePercentage());
        assertEquals(15.0, summary.getErrorRate());
        assertEquals("15.0%", summary.getErrorRatePercentage());
        assertEquals("COMPLETENESS", summary.getMostProblematicDimension());
        assertEquals("CRITICAL", summary.getMostCommonSeverity());
        assertEquals("MISSING_FIELD", summary.getMostFrequentErrorCode());
        assertTrue(summary.meetsQualityThreshold(80.0));
        assertFalse(summary.meetsQualityThreshold(90.0));
    }
    
    private ValidationResult createMockValidationResult(ValidationSummary summary) {
        // Create a mock ValidationResult for testing
        return new ValidationResult(
            Map.of(), // exposureResults
            java.util.List.of(), // batchErrors
            java.util.List.of(), // allErrors
            summary,
            new DimensionScores(85.0, 90.0, 80.0, 75.0, 95.0, 88.0),
            1000, // totalExposures
            850 // validExposures
        );
    }
}