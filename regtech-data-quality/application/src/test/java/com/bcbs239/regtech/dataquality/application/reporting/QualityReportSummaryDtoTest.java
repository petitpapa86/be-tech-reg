package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.dataquality.application.scoring.QualityScoresDto;
import com.bcbs239.regtech.dataquality.domain.quality.QualityGrade;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.report.QualityStatus;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QualityReportSummaryDto unit tests")
class QualityReportSummaryDtoTest {

    // Helper to create a mock QualityReport
    private QualityReport createMockReport(String reportId, String batchId, String bankId, QualityStatus status,
                                           QualityScoresDto scores, ValidationSummary validationSummary,
                                           Instant created, Instant updated) {
        QualityReport report = new QualityReport();
        report.setReportId(() -> reportId);
        report.setBatchId(() -> batchId);
        report.setBankId(() -> bankId);
        report.setStatus(status);
        report.setScores(scores != null ? scores.toDomain() : null);
        report.setValidationSummary(validationSummary);
        report.setCreatedAt(created);
        report.setUpdatedAt(updated);
        return report;
    }

    // Helper to create QualityScoresDto
    private QualityScoresDto createScores(double overall, String grade, boolean compliant) {
        return new QualityScoresDto(
            80.0, 85.0, 90.0, 75.0, 95.0, 70.0, // dimension scores
            overall, grade, compliant, !compliant
        );
    }

    // Helper to create ValidationSummary
    private ValidationSummary createValidationSummary(int total, int valid, int errors) {
        return new ValidationSummary(
            total, valid, total - valid, errors,
            Map.of(), Map.of(), Map.of(), (double) valid / total * 100.0
        );
    }

    @Test
    @DisplayName("fromDomain returns null for null report")
    void fromDomain_nullReport() {
        assertNull(QualityReportSummaryDto.fromDomain(null));
    }

    @Test
    @DisplayName("fromDomain creates DTO with valid report")
    void fromDomain_validReport() {
        QualityScoresDto scores = createScores(85.0, "B", true);
        ValidationSummary validationSummary = createValidationSummary(100, 80, 20);
        QualityReport report = createMockReport("r1", "b1", "bk1", QualityStatus.COMPLETED, scores, validationSummary,
                                                Instant.now(), Instant.now().plusSeconds(10));
        QualityReportSummaryDto dto = QualityReportSummaryDto.fromDomain(report);
        assertNotNull(dto);
        assertEquals("r1", dto.reportId());
        assertEquals("b1", dto.batchId());
        assertEquals("bk1", dto.bankId());
        assertEquals("COMPLETED", dto.status());
        assertEquals(scores, dto.scores());
        assertEquals(100, dto.totalExposures());
        assertEquals(80, dto.validExposures());
        assertEquals(20, dto.totalErrors());
        assertEquals(80.0, dto.validationRate());
    }

    @Test
    @DisplayName("fromDomain handles null validationSummary")
    void fromDomain_nullValidationSummary() {
        QualityReport report = createMockReport("r1", "b1", "bk1", QualityStatus.COMPLETED, null, null,
                                                Instant.now(), Instant.now());
        QualityReportSummaryDto dto = QualityReportSummaryDto.fromDomain(report);
        assertEquals(0, dto.totalExposures());
        assertEquals(0, dto.validExposures());
        assertEquals(0, dto.totalErrors());
        assertEquals(0.0, dto.validationRate());
    }

    @Test
    @DisplayName("fromDomain calculates validationRate with zero totalExposures")
    void fromDomain_zeroTotalExposures() {
        ValidationSummary validationSummary = createValidationSummary(0, 0, 0);
        QualityReport report = createMockReport("r1", "b1", "bk1", QualityStatus.COMPLETED, null, validationSummary,
                                                Instant.now(), Instant.now());
        QualityReportSummaryDto dto = QualityReportSummaryDto.fromDomain(report);
        assertEquals(0.0, dto.validationRate());
    }

    @Test
    @DisplayName("isCompleted returns true for COMPLETED status")
    void isCompleted_true() {
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "COMPLETED", null, 0, 0, 0, 0.0, null, null);
        assertTrue(dto.isCompleted());
    }

    @Test
    @DisplayName("isCompleted returns false for other statuses")
    void isCompleted_false() {
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "PENDING", null, 0, 0, 0, 0.0, null, null);
        assertFalse(dto.isCompleted());
    }

    @Test
    @DisplayName("isFailed returns true for FAILED status")
    void isFailed_true() {
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "FAILED", null, 0, 0, 0, 0.0, null, null);
        assertTrue(dto.isFailed());
    }

    @Test
    @DisplayName("isFailed returns false for other statuses")
    void isFailed_false() {
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "COMPLETED", null, 0, 0, 0, 0.0, null, null);
        assertFalse(dto.isFailed());
    }

    @Test
    @DisplayName("isCompliant returns true when scores are compliant")
    void isCompliant_true() {
        QualityScoresDto scores = createScores(85.0, "B", true);
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "COMPLETED", scores, 0, 0, 0, 0.0, null, null);
        assertTrue(dto.isCompliant());
    }

    @Test
    @DisplayName("isCompliant returns false when scores are null or not compliant")
    void isCompliant_false() {
        QualityReportSummaryDto dto1 = new QualityReportSummaryDto("r1", "b1", "bk1", "COMPLETED", null, 0, 0, 0, 0.0, null, null);
        QualityScoresDto scores = createScores(85.0, "B", false);
        QualityReportSummaryDto dto2 = new QualityReportSummaryDto("r1", "b1", "bk1", "COMPLETED", scores, 0, 0, 0, 0.0, null, null);
        assertFalse(dto1.isCompliant());
        assertFalse(dto2.isCompliant());
    }

    @Test
    @DisplayName("getOverallScore returns score when scores not null")
    void getOverallScore_withScores() {
        QualityScoresDto scores = createScores(85.0, "B", true);
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "COMPLETED", scores, 0, 0, 0, 0.0, null, null);
        assertEquals(85.0, dto.getOverallScore());
    }

    @Test
    @DisplayName("getOverallScore returns 0.0 when scores null")
    void getOverallScore_nullScores() {
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "COMPLETED", null, 0, 0, 0, 0.0, null, null);
        assertEquals(0.0, dto.getOverallScore());
    }

    @Test
    @DisplayName("getGrade returns grade when scores not null")
    void getGrade_withScores() {
        QualityScoresDto scores = createScores(85.0, "B", true);
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "COMPLETED", scores, 0, 0, 0, 0.0, null, null);
        assertEquals("B", dto.getGrade());
    }

    @Test
    @DisplayName("getGrade returns UNKNOWN when scores null")
    void getGrade_nullScores() {
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "COMPLETED", null, 0, 0, 0, 0.0, null, null);
        assertEquals("UNKNOWN", dto.getGrade());
    }

    @Test
    @DisplayName("getValidationRatePercentage formats rate correctly")
    void getValidationRatePercentage() {
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "COMPLETED", null, 0, 0, 0, 85.7, null, null);
        assertEquals("85.7%", dto.getValidationRatePercentage());
    }

    @Test
    @DisplayName("getProcessingDurationMs returns 0 for null timestamps")
    void getProcessingDurationMs_nullTimestamps() {
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "COMPLETED", null, 0, 0, 0, 0.0, null, null);
        assertEquals(0, dto.getProcessingDurationMs());
    }

    @Test
    @DisplayName("getProcessingDurationMs calculates duration correctly")
    void getProcessingDurationMs_valid() {
        Instant created = Instant.ofEpochMilli(1000);
        Instant updated = Instant.ofEpochMilli(2000);
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "COMPLETED", null, 0, 0, 0, 0.0, created, updated);
        assertEquals(1000, dto.getProcessingDurationMs());
    }

    @Test
    @DisplayName("getBriefSummary returns 'Failed' for failed status")
    void getBriefSummary_failed() {
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "FAILED", null, 0, 0, 0, 0.0, null, null);
        assertEquals("Failed", dto.getBriefSummary());
    }

    @Test
    @DisplayName("getBriefSummary returns status for incomplete")
    void getBriefSummary_incomplete() {
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "IN_PROGRESS", null, 0, 0, 0, 0.0, null, null);
        assertEquals("IN PROGRESS", dto.getBriefSummary());
    }

    @Test
    @DisplayName("getBriefSummary returns 'Completed' when completed but no scores")
    void getBriefSummary_completedNoScores() {
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "COMPLETED", null, 0, 0, 0, 0.0, null, null);
        assertEquals("Completed", dto.getBriefSummary());
    }

    @Test
    @DisplayName("getBriefSummary returns formatted grade and score when completed with scores")
    void getBriefSummary_completedWithScores() {
        QualityScoresDto scores = createScores(85.0, "B", true);
        QualityReportSummaryDto dto = new QualityReportSummaryDto("r1", "b1", "bk1", "COMPLETED", scores, 0, 0, 0, 0.0, null, null);
        assertEquals("B (85.0%)", dto.getBriefSummary());
    }
}