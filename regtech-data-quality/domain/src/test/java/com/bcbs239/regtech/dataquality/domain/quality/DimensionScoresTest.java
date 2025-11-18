package com.bcbs239.regtech.dataquality.domain.quality;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DimensionScores Tests")
class DimensionScoresTest {

    @Test
    @DisplayName("Should create valid dimension scores")
    void shouldCreateValidDimensionScores() {
        DimensionScores scores = new DimensionScores(90.0, 85.0, 80.0, 75.0, 70.0, 65.0);
        
        assertEquals(90.0, scores.completeness());
        assertEquals(85.0, scores.accuracy());
        assertEquals(80.0, scores.consistency());
        assertEquals(75.0, scores.timeliness());
        assertEquals(70.0, scores.uniqueness());
        assertEquals(65.0, scores.validity());
    }

    @Test
    @DisplayName("Should reject negative scores")
    void shouldRejectNegativeScores() {
        assertThrows(IllegalArgumentException.class, 
            () -> new DimensionScores(-1.0, 80.0, 80.0, 80.0, 80.0, 80.0));
        assertThrows(IllegalArgumentException.class, 
            () -> new DimensionScores(80.0, -5.0, 80.0, 80.0, 80.0, 80.0));
        assertThrows(IllegalArgumentException.class, 
            () -> new DimensionScores(80.0, 80.0, -10.0, 80.0, 80.0, 80.0));
    }

    @Test
    @DisplayName("Should reject scores over 100")
    void shouldRejectScoresOver100() {
        assertThrows(IllegalArgumentException.class, 
            () -> new DimensionScores(101.0, 80.0, 80.0, 80.0, 80.0, 80.0));
        assertThrows(IllegalArgumentException.class, 
            () -> new DimensionScores(80.0, 150.0, 80.0, 80.0, 80.0, 80.0));
        assertThrows(IllegalArgumentException.class, 
            () -> new DimensionScores(80.0, 80.0, 80.0, 200.0, 80.0, 80.0));
    }

    @Test
    @DisplayName("Should allow boundary values 0 and 100")
    void shouldAllowBoundaryValues() {
        assertDoesNotThrow(() -> new DimensionScores(0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
        assertDoesNotThrow(() -> new DimensionScores(100.0, 100.0, 100.0, 100.0, 100.0, 100.0));
        assertDoesNotThrow(() -> new DimensionScores(0.0, 50.0, 100.0, 25.0, 75.0, 100.0));
    }

    @Test
    @DisplayName("Should create empty dimension scores")
    void shouldCreateEmptyDimensionScores() {
        DimensionScores scores = DimensionScores.empty();
        
        assertEquals(0.0, scores.completeness());
        assertEquals(0.0, scores.accuracy());
        assertEquals(0.0, scores.consistency());
        assertEquals(0.0, scores.timeliness());
        assertEquals(0.0, scores.uniqueness());
        assertEquals(0.0, scores.validity());
    }

    @Test
    @DisplayName("Should create perfect dimension scores")
    void shouldCreatePerfectDimensionScores() {
        DimensionScores scores = DimensionScores.perfect();
        
        assertEquals(100.0, scores.completeness());
        assertEquals(100.0, scores.accuracy());
        assertEquals(100.0, scores.consistency());
        assertEquals(100.0, scores.timeliness());
        assertEquals(100.0, scores.uniqueness());
        assertEquals(100.0, scores.validity());
    }

    @Test
    @DisplayName("Should get score for specific dimension")
    void shouldGetScoreForSpecificDimension() {
        DimensionScores scores = new DimensionScores(90.0, 85.0, 80.0, 75.0, 70.0, 65.0);
        
        assertEquals(90.0, scores.getScore(QualityDimension.COMPLETENESS));
        assertEquals(85.0, scores.getScore(QualityDimension.ACCURACY));
        assertEquals(80.0, scores.getScore(QualityDimension.CONSISTENCY));
        assertEquals(75.0, scores.getScore(QualityDimension.TIMELINESS));
        assertEquals(70.0, scores.getScore(QualityDimension.UNIQUENESS));
        assertEquals(65.0, scores.getScore(QualityDimension.VALIDITY));
    }

    @Test
    @DisplayName("Should calculate overall score with default weights")
    void shouldCalculateOverallScoreWithDefaultWeights() {
        DimensionScores scores = new DimensionScores(100.0, 80.0, 90.0, 70.0, 60.0, 50.0);
        QualityWeights weights = QualityWeights.defaultWeights();
        
        double expectedScore = 
            100.0 * 0.25 +  // completeness
            80.0 * 0.25 +   // accuracy
            90.0 * 0.20 +   // consistency
            70.0 * 0.15 +   // timeliness
            60.0 * 0.10 +   // uniqueness
            50.0 * 0.05;    // validity
        
        double actualScore = scores.calculateOverallScore(weights);
        assertEquals(expectedScore, actualScore, 0.001);
    }

    @Test
    @DisplayName("Should calculate overall score with equal weights")
    void shouldCalculateOverallScoreWithEqualWeights() {
        DimensionScores scores = new DimensionScores(90.0, 80.0, 70.0, 60.0, 50.0, 40.0);
        QualityWeights weights = QualityWeights.equalWeights();
        
        double expectedScore = (90.0 + 80.0 + 70.0 + 60.0 + 50.0 + 40.0) / 6.0;
        double actualScore = scores.calculateOverallScore(weights);
        assertEquals(expectedScore, actualScore, 0.001);
    }

    @Test
    @DisplayName("Should calculate perfect overall score")
    void shouldCalculatePerfectOverallScore() {
        DimensionScores scores = DimensionScores.perfect();
        QualityWeights weights = QualityWeights.defaultWeights();
        
        assertEquals(100.0, scores.calculateOverallScore(weights), 0.001);
    }

    @Test
    @DisplayName("Should calculate zero overall score")
    void shouldCalculateZeroOverallScore() {
        DimensionScores scores = DimensionScores.empty();
        QualityWeights weights = QualityWeights.defaultWeights();
        
        assertEquals(0.0, scores.calculateOverallScore(weights), 0.001);
    }

    @Test
    @DisplayName("Should calculate average score")
    void shouldCalculateAverageScore() {
        DimensionScores scores = new DimensionScores(90.0, 80.0, 70.0, 60.0, 50.0, 40.0);
        
        double expectedAverage = (90.0 + 80.0 + 70.0 + 60.0 + 50.0 + 40.0) / 6.0;
        assertEquals(expectedAverage, scores.getAverageScore(), 0.001);
    }

    @Test
    @DisplayName("Should get minimum score")
    void shouldGetMinimumScore() {
        DimensionScores scores = new DimensionScores(90.0, 80.0, 70.0, 60.0, 50.0, 40.0);
        assertEquals(40.0, scores.getMinimumScore());
    }

    @Test
    @DisplayName("Should get maximum score")
    void shouldGetMaximumScore() {
        DimensionScores scores = new DimensionScores(90.0, 80.0, 70.0, 60.0, 50.0, 40.0);
        assertEquals(90.0, scores.getMaximumScore());
    }
}
