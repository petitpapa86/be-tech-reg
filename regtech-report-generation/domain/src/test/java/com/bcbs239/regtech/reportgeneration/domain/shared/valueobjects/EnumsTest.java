package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for enum types
 * Validates Requirements 8.1, 8.2, 13.1
 */
class EnumsTest {

    @Test
    void testReportTypeEnum() {
        // Requirement 13.1: ReportType enum with COMPREHENSIVE, LARGE_EXPOSURES, DATA_QUALITY
        assertEquals(3, ReportType.values().length);
        assertNotNull(ReportType.COMPREHENSIVE);
        assertNotNull(ReportType.LARGE_EXPOSURES);
        assertNotNull(ReportType.DATA_QUALITY);
    }

    @Test
    void testReportStatusEnum() {
        // Requirement 13.1: ReportStatus enum with all lifecycle states
        assertEquals(5, ReportStatus.values().length);
        assertNotNull(ReportStatus.PENDING);
        assertNotNull(ReportStatus.IN_PROGRESS);
        assertNotNull(ReportStatus.COMPLETED);
        assertNotNull(ReportStatus.PARTIAL);
        assertNotNull(ReportStatus.FAILED);
    }

    @Test
    void testComplianceStatusFromScore() {
        // Requirement 8.1: ComplianceStatus with fromScore() method
        
        // Score >= 90% -> COMPLIANT
        assertEquals(ComplianceStatus.COMPLIANT, 
            ComplianceStatus.fromScore(new BigDecimal("90")));
        assertEquals(ComplianceStatus.COMPLIANT, 
            ComplianceStatus.fromScore(new BigDecimal("95")));
        assertEquals(ComplianceStatus.COMPLIANT, 
            ComplianceStatus.fromScore(new BigDecimal("100")));
        
        // Score >= 75% and < 90% -> LARGELY_COMPLIANT
        assertEquals(ComplianceStatus.LARGELY_COMPLIANT, 
            ComplianceStatus.fromScore(new BigDecimal("75")));
        assertEquals(ComplianceStatus.LARGELY_COMPLIANT, 
            ComplianceStatus.fromScore(new BigDecimal("85")));
        assertEquals(ComplianceStatus.LARGELY_COMPLIANT, 
            ComplianceStatus.fromScore(new BigDecimal("89.9")));
        
        // Score >= 60% and < 75% -> MATERIALLY_NON_COMPLIANT
        assertEquals(ComplianceStatus.MATERIALLY_NON_COMPLIANT, 
            ComplianceStatus.fromScore(new BigDecimal("60")));
        assertEquals(ComplianceStatus.MATERIALLY_NON_COMPLIANT, 
            ComplianceStatus.fromScore(new BigDecimal("70")));
        assertEquals(ComplianceStatus.MATERIALLY_NON_COMPLIANT, 
            ComplianceStatus.fromScore(new BigDecimal("74.9")));
        
        // Score < 60% -> NON_COMPLIANT
        assertEquals(ComplianceStatus.NON_COMPLIANT, 
            ComplianceStatus.fromScore(new BigDecimal("59.9")));
        assertEquals(ComplianceStatus.NON_COMPLIANT, 
            ComplianceStatus.fromScore(new BigDecimal("50")));
        assertEquals(ComplianceStatus.NON_COMPLIANT, 
            ComplianceStatus.fromScore(new BigDecimal("0")));
    }

    @Test
    void testComplianceStatusFromScoreValidation() {
        // Should throw exception for null score
        assertThrows(IllegalArgumentException.class, 
            () -> ComplianceStatus.fromScore(null));
        
        // Should throw exception for negative score
        assertThrows(IllegalArgumentException.class, 
            () -> ComplianceStatus.fromScore(new BigDecimal("-1")));
        
        // Should throw exception for score > 100
        assertThrows(IllegalArgumentException.class, 
            () -> ComplianceStatus.fromScore(new BigDecimal("101")));
    }

    @Test
    void testComplianceStatusHelperMethods() {
        assertTrue(ComplianceStatus.COMPLIANT.isCompliant());
        assertTrue(ComplianceStatus.LARGELY_COMPLIANT.isCompliant());
        assertFalse(ComplianceStatus.MATERIALLY_NON_COMPLIANT.isCompliant());
        assertFalse(ComplianceStatus.NON_COMPLIANT.isCompliant());
        
        assertTrue(ComplianceStatus.NON_COMPLIANT.requiresImmediateAction());
        assertFalse(ComplianceStatus.COMPLIANT.requiresImmediateAction());
    }

    @Test
    void testQualityDimensionThresholds() {
        // Requirement 8.2: QualityDimension enum with thresholds
        
        // COMPLETENESS threshold: 70%
        assertEquals(new BigDecimal("70"), QualityDimension.COMPLETENESS.getCriticalThreshold());
        assertTrue(QualityDimension.COMPLETENESS.isCritical(new BigDecimal("69")));
        assertFalse(QualityDimension.COMPLETENESS.isCritical(new BigDecimal("70")));
        
        // ACCURACY threshold: 70%
        assertEquals(new BigDecimal("70"), QualityDimension.ACCURACY.getCriticalThreshold());
        assertTrue(QualityDimension.ACCURACY.isCritical(new BigDecimal("69")));
        assertFalse(QualityDimension.ACCURACY.isCritical(new BigDecimal("70")));
        
        // CONSISTENCY threshold: 80%
        assertEquals(new BigDecimal("80"), QualityDimension.CONSISTENCY.getCriticalThreshold());
        assertTrue(QualityDimension.CONSISTENCY.isCritical(new BigDecimal("79")));
        assertFalse(QualityDimension.CONSISTENCY.isCritical(new BigDecimal("80")));
        
        // TIMELINESS threshold: 90%
        assertEquals(new BigDecimal("90"), QualityDimension.TIMELINESS.getCriticalThreshold());
        assertTrue(QualityDimension.TIMELINESS.isCritical(new BigDecimal("89")));
        assertFalse(QualityDimension.TIMELINESS.isCritical(new BigDecimal("90")));
        
        // UNIQUENESS threshold: 95%
        assertEquals(new BigDecimal("95"), QualityDimension.UNIQUENESS.getCriticalThreshold());
        assertTrue(QualityDimension.UNIQUENESS.isCritical(new BigDecimal("94")));
        assertFalse(QualityDimension.UNIQUENESS.isCritical(new BigDecimal("95")));
        
        // VALIDITY threshold: 90%
        assertEquals(new BigDecimal("90"), QualityDimension.VALIDITY.getCriticalThreshold());
        assertTrue(QualityDimension.VALIDITY.isCritical(new BigDecimal("89")));
        assertFalse(QualityDimension.VALIDITY.isCritical(new BigDecimal("90")));
    }

    @Test
    void testQualityDimensionExcellence() {
        // Score >= 95% is excellent
        assertTrue(QualityDimension.COMPLETENESS.isExcellent(new BigDecimal("95")));
        assertTrue(QualityDimension.COMPLETENESS.isExcellent(new BigDecimal("100")));
        assertFalse(QualityDimension.COMPLETENESS.isExcellent(new BigDecimal("94.9")));
    }

    @Test
    void testQualityGradeFromScore() {
        // Requirement 8.1: QualityGrade enum (A-F)
        
        // A: 90-100%
        assertEquals(QualityGrade.A, QualityGrade.fromScore(new BigDecimal("90")));
        assertEquals(QualityGrade.A, QualityGrade.fromScore(new BigDecimal("95")));
        assertEquals(QualityGrade.A, QualityGrade.fromScore(new BigDecimal("100")));
        
        // B: 80-89%
        assertEquals(QualityGrade.B, QualityGrade.fromScore(new BigDecimal("80")));
        assertEquals(QualityGrade.B, QualityGrade.fromScore(new BigDecimal("85")));
        assertEquals(QualityGrade.B, QualityGrade.fromScore(new BigDecimal("89.9")));
        
        // C: 70-79%
        assertEquals(QualityGrade.C, QualityGrade.fromScore(new BigDecimal("70")));
        assertEquals(QualityGrade.C, QualityGrade.fromScore(new BigDecimal("75")));
        assertEquals(QualityGrade.C, QualityGrade.fromScore(new BigDecimal("79.9")));
        
        // D: 60-69%
        assertEquals(QualityGrade.D, QualityGrade.fromScore(new BigDecimal("60")));
        assertEquals(QualityGrade.D, QualityGrade.fromScore(new BigDecimal("65")));
        assertEquals(QualityGrade.D, QualityGrade.fromScore(new BigDecimal("69.9")));
        
        // E: 50-59%
        assertEquals(QualityGrade.E, QualityGrade.fromScore(new BigDecimal("50")));
        assertEquals(QualityGrade.E, QualityGrade.fromScore(new BigDecimal("55")));
        assertEquals(QualityGrade.E, QualityGrade.fromScore(new BigDecimal("59.9")));
        
        // F: < 50%
        assertEquals(QualityGrade.F, QualityGrade.fromScore(new BigDecimal("49.9")));
        assertEquals(QualityGrade.F, QualityGrade.fromScore(new BigDecimal("25")));
        assertEquals(QualityGrade.F, QualityGrade.fromScore(new BigDecimal("0")));
    }

    @Test
    void testQualityGradeHelperMethods() {
        assertTrue(QualityGrade.A.isPassing());
        assertTrue(QualityGrade.B.isPassing());
        assertTrue(QualityGrade.C.isPassing());
        assertFalse(QualityGrade.D.isPassing());
        
        assertTrue(QualityGrade.D.requiresAttention());
        assertTrue(QualityGrade.E.requiresAttention());
        assertTrue(QualityGrade.F.requiresAttention());
        assertFalse(QualityGrade.A.requiresAttention());
        
        assertTrue(QualityGrade.A.isExcellent());
        assertFalse(QualityGrade.B.isExcellent());
        
        assertTrue(QualityGrade.F.isFailing());
        assertFalse(QualityGrade.E.isFailing());
    }

    @Test
    void testQualityGradeColorClasses() {
        assertEquals("green", QualityGrade.A.getColorClass());
        assertEquals("blue", QualityGrade.B.getColorClass());
        assertEquals("yellow", QualityGrade.C.getColorClass());
        assertEquals("orange", QualityGrade.D.getColorClass());
        assertEquals("red", QualityGrade.E.getColorClass());
        assertEquals("dark-red", QualityGrade.F.getColorClass());
    }

    @Test
    void testAllEnumsHaveValues() {
        // Verify all required enums exist and have expected values
        assertEquals(3, ReportType.values().length);
        assertEquals(5, ReportStatus.values().length);
        assertEquals(4, ComplianceStatus.values().length);
        assertEquals(6, QualityDimension.values().length);
        assertEquals(6, QualityGrade.values().length);
    }
}
