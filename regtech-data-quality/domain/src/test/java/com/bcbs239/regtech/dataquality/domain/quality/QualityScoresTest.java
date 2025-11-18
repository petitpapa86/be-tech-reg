package com.bcbs239.regtech.dataquality.domain.quality;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QualityScores Tests")
class QualityScoresTest {

    @Test
    @DisplayName("Should create valid quality scores")
    void shouldCreateValidQualityScores() {
        QualityScores scores = new QualityScores(
            90.0, 85.0, 80.0, 75.0, 70.0, 65.0, 82.5, QualityGrade.GOOD);
        
        assertEquals(90.0, scores.completenessScore());
        assertEquals(85.0, scores.accuracyScore());
        assertEquals(80.0, scores.consistencyScore());
        assertEquals(75.0, scores.timelinessScore());
        assertEquals(70.0, scores.uniquenessScore());
        assertEquals(65.0, scores.validityScore());
        assertEquals(82.5, scores.overallScore());
        assertEquals(QualityGrade.GOOD, scores.grade());
    }

    @Test
    @DisplayName("Should reject scores below 0")
    void shouldRejectScoresBelowZero() {
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityScores(-1.0, 80.0, 80.0, 80.0, 80.0, 80.0, 80.0, QualityGrade.GOOD));
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityScores(80.0, -5.0, 80.0, 80.0, 80.0, 80.0, 80.0, QualityGrade.GOOD));
    }

    @Test
    @DisplayName("Should reject scores above 100")
    void shouldRejectScoresAbove100() {
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityScores(101.0, 80.0, 80.0, 80.0, 80.0, 80.0, 80.0, QualityGrade.GOOD));
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityScores(80.0, 80.0, 80.0, 80.0, 80.0, 150.0, 80.0, QualityGrade.GOOD));
    }

    @Test
    @DisplayName("Should reject null grade")
    void shouldRejectNullGrade() {
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityScores(80.0, 80.0, 80.0, 80.0, 80.0, 80.0, 80.0, null));
    }

    @Test
    @DisplayName("Should create empty quality scores")
    void shouldCreateEmptyQualityScores() {
        QualityScores scores = QualityScores.empty();
        
        assertEquals(0.0, scores.completenessScore());
        assertEquals(0.0, scores.accuracyScore());
        assertEquals(0.0, scores.consistencyScore());
        assertEquals(0.0, scores.timelinessScore());
        assertEquals(0.0, scores.uniquenessScore());
        assertEquals(0.0, scores.validityScore());
        assertEquals(0.0, scores.overallScore());
        assertEquals(QualityGrade.POOR, scores.grade());
    }

    @Test
    @DisplayName("Should create perfect quality scores")
    void shouldCreatePerfectQualityScores() {
        QualityScores scores = QualityScores.perfect();
        
        assertEquals(100.0, scores.completenessScore());
        assertEquals(100.0, scores.accuracyScore());
        assertEquals(100.0, scores.consistencyScore());
        assertEquals(100.0, scores.timelinessScore());
        assertEquals(100.0, scores.uniquenessScore());
        assertEquals(100.0, scores.validityScore());
        assertEquals(100.0, scores.overallScore());
        assertEquals(QualityGrade.EXCELLENT, scores.grade());
    }

    @Test
    @DisplayName("Should calculate quality scores from dimension scores and weights")
    void shouldCalculateFromDimensionScoresAndWeights() {
        DimensionScores dimensions = new DimensionScores(90.0, 85.0, 80.0, 75.0, 70.0, 65.0);
        QualityWeights weights = QualityWeights.defaultWeights();
        
        QualityScores scores = QualityScores.calculate(dimensions, weights);
        
        assertEquals(90.0, scores.completenessScore());
        assertEquals(85.0, scores.accuracyScore());
        assertEquals(80.0, scores.consistencyScore());
        assertEquals(75.0, scores.timelinessScore());
        assertEquals(70.0, scores.uniquenessScore());
        assertEquals(65.0, scores.validityScore());
        assertTrue(scores.overallScore() > 0);
        assertNotNull(scores.grade());
    }

    @Test
    @DisplayName("Should calculate correct overall score with default weights")
    void shouldCalculateCorrectOverallScoreWithDefaultWeights() {
        DimensionScores dimensions = new DimensionScores(100.0, 80.0, 90.0, 70.0, 60.0, 50.0);
        QualityWeights weights = QualityWeights.defaultWeights();
        
        double expectedOverall = 
            100.0 * 0.25 +  // completeness
            80.0 * 0.25 +   // accuracy
            90.0 * 0.20 +   // consistency
            70.0 * 0.15 +   // timeliness
            60.0 * 0.10 +   // uniqueness
            50.0 * 0.05;    // validity
        
        QualityScores scores = QualityScores.calculate(dimensions, weights);
        
        assertEquals(expectedOverall, scores.overallScore(), 0.001);
    }

    @Test
    @DisplayName("Should assign correct grade based on overall score")
    void shouldAssignCorrectGradeBasedOnOverallScore() {
        // Test EXCELLENT grade
        DimensionScores excellentDimensions = new DimensionScores(96.0, 96.0, 96.0, 96.0, 96.0, 96.0);
        QualityScores excellentScores = QualityScores.calculate(excellentDimensions, QualityWeights.equalWeights());
        assertEquals(QualityGrade.EXCELLENT, excellentScores.grade());
        
        // Test GOOD grade
        DimensionScores goodDimensions = new DimensionScores(85.0, 85.0, 85.0, 85.0, 85.0, 85.0);
        QualityScores goodScores = QualityScores.calculate(goodDimensions, QualityWeights.equalWeights());
        assertEquals(QualityGrade.GOOD, goodScores.grade());
        
        // Test POOR grade
        DimensionScores poorDimensions = new DimensionScores(50.0, 50.0, 50.0, 50.0, 50.0, 50.0);
        QualityScores poorScores = QualityScores.calculate(poorDimensions, QualityWeights.equalWeights());
        assertEquals(QualityGrade.POOR, poorScores.grade());
    }

    @Test
    @DisplayName("Should handle boundary score values")
    void shouldHandleBoundaryScoreValues() {
        assertDoesNotThrow(() -> new QualityScores(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, QualityGrade.POOR));
        assertDoesNotThrow(() -> new QualityScores(
            100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, QualityGrade.EXCELLENT));
    }

    @Test
    @DisplayName("Should validate all dimension scores independently")
    void shouldValidateAllDimensionScoresIndependently() {
        // Valid case - all within range
        assertDoesNotThrow(() -> new QualityScores(
            50.0, 60.0, 70.0, 80.0, 90.0, 100.0, 75.0, QualityGrade.ACCEPTABLE));
        
        // Invalid case - each dimension
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityScores(-1.0, 60.0, 70.0, 80.0, 90.0, 100.0, 75.0, QualityGrade.ACCEPTABLE));
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityScores(50.0, 101.0, 70.0, 80.0, 90.0, 100.0, 75.0, QualityGrade.ACCEPTABLE));
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityScores(50.0, 60.0, -1.0, 80.0, 90.0, 100.0, 75.0, QualityGrade.ACCEPTABLE));
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityScores(50.0, 60.0, 70.0, 101.0, 90.0, 100.0, 75.0, QualityGrade.ACCEPTABLE));
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityScores(50.0, 60.0, 70.0, 80.0, -1.0, 100.0, 75.0, QualityGrade.ACCEPTABLE));
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityScores(50.0, 60.0, 70.0, 80.0, 90.0, 101.0, 75.0, QualityGrade.ACCEPTABLE));
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityScores(50.0, 60.0, 70.0, 80.0, 90.0, 100.0, -1.0, QualityGrade.ACCEPTABLE));
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityScores(50.0, 60.0, 70.0, 80.0, 90.0, 100.0, 101.0, QualityGrade.ACCEPTABLE));
    }

    @Test
    @DisplayName("Should calculate scores with custom weights")
    void shouldCalculateScoresWithCustomWeights() {
        DimensionScores dimensions = new DimensionScores(100.0, 80.0, 90.0, 70.0, 60.0, 50.0);
        // Custom weights emphasizing completeness and accuracy
        QualityWeights customWeights = new QualityWeights(0.40, 0.40, 0.10, 0.05, 0.03, 0.02);
        
        QualityScores scores = QualityScores.calculate(dimensions, customWeights);
        
        double expectedOverall = 
            100.0 * 0.40 +  // completeness
            80.0 * 0.40 +   // accuracy
            90.0 * 0.10 +   // consistency
            70.0 * 0.05 +   // timeliness
            60.0 * 0.03 +   // uniqueness
            50.0 * 0.02;    // validity
        
        assertEquals(expectedOverall, scores.overallScore(), 0.001);
    }
}
