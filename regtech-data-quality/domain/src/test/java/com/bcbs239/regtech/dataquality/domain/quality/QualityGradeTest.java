package com.bcbs239.regtech.dataquality.domain.quality;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QualityGrade Tests")
class QualityGradeTest {

    @Test
    @DisplayName("Should assign EXCELLENT grade for score >= 95")
    void shouldAssignExcellentGrade() {
        assertEquals(QualityGrade.EXCELLENT, QualityGrade.fromScore(95.0));
        assertEquals(QualityGrade.EXCELLENT, QualityGrade.fromScore(98.5));
        assertEquals(QualityGrade.EXCELLENT, QualityGrade.fromScore(100.0));
    }

    @Test
    @DisplayName("Should assign VERY_GOOD grade for score >= 90 and < 95")
    void shouldAssignVeryGoodGrade() {
        assertEquals(QualityGrade.VERY_GOOD, QualityGrade.fromScore(90.0));
        assertEquals(QualityGrade.VERY_GOOD, QualityGrade.fromScore(92.5));
        assertEquals(QualityGrade.VERY_GOOD, QualityGrade.fromScore(94.9));
    }

    @Test
    @DisplayName("Should assign GOOD grade for score >= 80 and < 90")
    void shouldAssignGoodGrade() {
        assertEquals(QualityGrade.GOOD, QualityGrade.fromScore(80.0));
        assertEquals(QualityGrade.GOOD, QualityGrade.fromScore(85.0));
        assertEquals(QualityGrade.GOOD, QualityGrade.fromScore(89.9));
    }

    @Test
    @DisplayName("Should assign ACCEPTABLE grade for score >= 70 and < 80")
    void shouldAssignAcceptableGrade() {
        assertEquals(QualityGrade.ACCEPTABLE, QualityGrade.fromScore(70.0));
        assertEquals(QualityGrade.ACCEPTABLE, QualityGrade.fromScore(75.0));
        assertEquals(QualityGrade.ACCEPTABLE, QualityGrade.fromScore(79.9));
    }

    @Test
    @DisplayName("Should assign POOR grade for score < 70")
    void shouldAssignPoorGrade() {
        assertEquals(QualityGrade.POOR, QualityGrade.fromScore(0.0));
        assertEquals(QualityGrade.POOR, QualityGrade.fromScore(50.0));
        assertEquals(QualityGrade.POOR, QualityGrade.fromScore(69.9));
    }

    @Test
    @DisplayName("Should return correct threshold for each grade")
    void shouldReturnCorrectThreshold() {
        assertEquals(95.0, QualityGrade.EXCELLENT.getThreshold());
        assertEquals(90.0, QualityGrade.VERY_GOOD.getThreshold());
        assertEquals(80.0, QualityGrade.GOOD.getThreshold());
        assertEquals(70.0, QualityGrade.ACCEPTABLE.getThreshold());
        assertEquals(0.0, QualityGrade.POOR.getThreshold());
    }

    @Test
    @DisplayName("Should return correct letter grade for each grade")
    void shouldReturnCorrectLetterGrade() {
        assertEquals("A+", QualityGrade.EXCELLENT.getLetterGrade());
        assertEquals("A", QualityGrade.VERY_GOOD.getLetterGrade());
        assertEquals("B", QualityGrade.GOOD.getLetterGrade());
        assertEquals("C", QualityGrade.ACCEPTABLE.getLetterGrade());
        assertEquals("F", QualityGrade.POOR.getLetterGrade());
    }

    @Test
    @DisplayName("Should return correct description for each grade")
    void shouldReturnCorrectDescription() {
        assertEquals("Excellent", QualityGrade.EXCELLENT.getDescription());
        assertEquals("Very Good", QualityGrade.VERY_GOOD.getDescription());
        assertEquals("Good", QualityGrade.GOOD.getDescription());
        assertEquals("Acceptable", QualityGrade.ACCEPTABLE.getDescription());
        assertEquals("Poor", QualityGrade.POOR.getDescription());
    }

    @Test
    @DisplayName("Should correctly identify compliant grades (B or better)")
    void shouldIdentifyCompliantGrades() {
        assertTrue(QualityGrade.EXCELLENT.isCompliant());
        assertTrue(QualityGrade.VERY_GOOD.isCompliant());
        assertTrue(QualityGrade.GOOD.isCompliant());
        assertFalse(QualityGrade.ACCEPTABLE.isCompliant());
        assertFalse(QualityGrade.POOR.isCompliant());
    }

    @Test
    @DisplayName("Should correctly identify grades requiring attention (C or worse)")
    void shouldIdentifyGradesRequiringAttention() {
        assertFalse(QualityGrade.EXCELLENT.requiresAttention());
        assertFalse(QualityGrade.VERY_GOOD.requiresAttention());
        assertFalse(QualityGrade.GOOD.requiresAttention());
        assertTrue(QualityGrade.ACCEPTABLE.requiresAttention());
        assertTrue(QualityGrade.POOR.requiresAttention());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 25.0, 50.0, 69.9, 70.0, 75.0, 79.9, 80.0, 85.0, 89.9, 90.0, 92.5, 94.9, 95.0, 98.0, 100.0})
    @DisplayName("Should handle boundary cases correctly")
    void shouldHandleBoundaryCases(double score) {
        assertNotNull(QualityGrade.fromScore(score));
    }
}
