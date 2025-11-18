package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.dataquality.application.scoring.QualityScoresDto;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QualityReportSummaryDto unit tests")
class QualityReportSummaryDtoTest {

    @Test
    @DisplayName("fromDomain returns null for null report")
    void fromDomainNullReport() {
        assertNull(QualityReportSummaryDto.fromDomain(null));
    }

    @Test
    @DisplayName("fromDomain creates DTO with valid report")
    void fromDomainValidReport() {
        // Arrange
        BatchId batchId = BatchId.of("batch_batch-123");
        BankId bankId = BankId.of("bank-456");
        QualityReport report = QualityReport.createForBatch(batchId, bankId);
        
        // Act
        QualityReportSummaryDto dto = QualityReportSummaryDto.fromDomain(report);
        
        // Assert
        assertNotNull(dto);
        assertEquals(batchId.value(), dto.batchId());
        assertEquals(bankId.value(), dto.bankId());
    }

    @Test
    @DisplayName("isCompleted returns true for COMPLETED status")
    void isCompletedTrue() {
        QualityReportSummaryDto dto = new QualityReportSummaryDto(
            "r1", "b1", "bk1", "COMPLETED", null, 0, 0, 0, 0.0, null, null
        );
        assertTrue(dto.isCompleted());
    }

    @Test
    @DisplayName("isCompleted returns false for other statuses")
    void isCompletedFalse() {
        QualityReportSummaryDto dto = new QualityReportSummaryDto(
            "r1", "b1", "bk1", "IN_PROGRESS", null, 0, 0, 0, 0.0, null, null
        );
        assertFalse(dto.isCompleted());
    }

    @Test
    @DisplayName("DTO constructor sets all fields correctly")
    void dtoConstructorSetsAllFields() {
        QualityScoresDto scores = new QualityScoresDto(
            80.0, 85.0, 90.0, 75.0, 95.0, 70.0, 
            82.5, "B", true, false
        );
        
        QualityReportSummaryDto dto = new QualityReportSummaryDto(
            "report-1", 
            "batch-1", 
            "bank-1", 
            "COMPLETED", 
            scores, 
            100, 
            90, 
            10, 
            90.0, 
            null, 
            null
        );
        
        assertEquals("report-1", dto.reportId());
        assertEquals("batch-1", dto.batchId());
        assertEquals("bank-1", dto.bankId());
        assertEquals("COMPLETED", dto.status());
        assertEquals(scores, dto.scores());
        assertEquals(100, dto.totalExposures());
        assertEquals(90, dto.validExposures());
        assertEquals(10, dto.totalErrors());
        assertEquals(90.0, dto.validationRate());
    }
}
