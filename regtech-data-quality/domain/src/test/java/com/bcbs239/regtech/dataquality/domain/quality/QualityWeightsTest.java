package com.bcbs239.regtech.dataquality.domain.quality;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QualityWeights Tests")
class QualityWeightsTest {

    @Test
    @DisplayName("Should create valid quality weights")
    void shouldCreateValidQualityWeights() {
        QualityWeights weights = new QualityWeights(0.25, 0.25, 0.20, 0.15, 0.10, 0.05);
        
        assertEquals(0.25, weights.completeness(), 0.001);
        assertEquals(0.25, weights.accuracy(), 0.001);
        assertEquals(0.20, weights.consistency(), 0.001);
        assertEquals(0.15, weights.timeliness(), 0.001);
        assertEquals(0.10, weights.uniqueness(), 0.001);
        assertEquals(0.05, weights.validity(), 0.001);
    }

    @Test
    @DisplayName("Should reject weights that don't sum to 1.0")
    void shouldRejectWeightsThatDontSumToOne() {
        // These should throw exceptions because they don't sum to 1.0
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityWeights(0.3, 0.3, 0.2, 0.1, 0.1, 0.05)); // Sum = 1.05
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityWeights(0.2, 0.2, 0.2, 0.2, 0.1, 0.05)); // Sum = 0.95
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityWeights(0.5, 0.5, 0.0, 0.0, 0.0, 0.1)); // Sum = 1.1
    }

    @Test
    @DisplayName("Should reject negative weights")
    void shouldRejectNegativeWeights() {
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityWeights(-0.1, 0.25, 0.25, 0.2, 0.2, 0.2));
        assertThrows(IllegalArgumentException.class, 
            () -> new QualityWeights(0.25, -0.05, 0.25, 0.2, 0.2, 0.15));
    }

    @Test
    @DisplayName("Should allow weights that sum to approximately 1.0 (floating point tolerance)")
    void shouldAllowWeightsSumToApproximatelyOne() {
        // Small floating point differences should be allowed
        assertDoesNotThrow(() -> new QualityWeights(
            0.25, 0.25, 0.20, 0.15, 0.10, 0.050001));
    }

    @Test
    @DisplayName("Should create default weights")
    void shouldCreateDefaultWeights() {
        QualityWeights weights = QualityWeights.defaultWeights();
        
        assertEquals(0.25, weights.completeness(), 0.001);
        assertEquals(0.25, weights.accuracy(), 0.001);
        assertEquals(0.20, weights.consistency(), 0.001);
        assertEquals(0.15, weights.timeliness(), 0.001);
        assertEquals(0.10, weights.uniqueness(), 0.001);
        assertEquals(0.05, weights.validity(), 0.001);
    }

    @Test
    @DisplayName("Should create equal weights")
    void shouldCreateEqualWeights() {
        QualityWeights weights = QualityWeights.equalWeights();
        
        double expectedWeight = 1.0 / 6.0;
        assertEquals(expectedWeight, weights.completeness(), 0.001);
        assertEquals(expectedWeight, weights.accuracy(), 0.001);
        assertEquals(expectedWeight, weights.consistency(), 0.001);
        assertEquals(expectedWeight, weights.timeliness(), 0.001);
        assertEquals(expectedWeight, weights.uniqueness(), 0.001);
        assertEquals(expectedWeight, weights.validity(), 0.001);
    }

    @Test
    @DisplayName("Should get weight for specific dimension")
    void shouldGetWeightForSpecificDimension() {
        QualityWeights weights = QualityWeights.defaultWeights();
        
        assertEquals(0.25, weights.getWeight(QualityDimension.COMPLETENESS), 0.001);
        assertEquals(0.25, weights.getWeight(QualityDimension.ACCURACY), 0.001);
        assertEquals(0.20, weights.getWeight(QualityDimension.CONSISTENCY), 0.001);
        assertEquals(0.15, weights.getWeight(QualityDimension.TIMELINESS), 0.001);
        assertEquals(0.10, weights.getWeight(QualityDimension.UNIQUENESS), 0.001);
        assertEquals(0.05, weights.getWeight(QualityDimension.VALIDITY), 0.001);
    }

    @Test
    @DisplayName("Should modify weight for specific dimension and adjust others proportionally")
    void shouldModifyWeightAndAdjustOthers() {
        QualityWeights original = QualityWeights.defaultWeights();
        QualityWeights modified = original.withWeight(QualityDimension.COMPLETENESS, 0.50);
        
        assertEquals(0.50, modified.completeness(), 0.001);
        // Other weights should be proportionally adjusted to sum to 0.50 (1.0 - 0.50)
        assertTrue(modified.accuracy() + modified.consistency() + modified.timeliness() 
                   + modified.uniqueness() + modified.validity() <= 0.501);
    }

    @Test
    @DisplayName("Should reject weight modification outside valid range")
    void shouldRejectWeightModificationOutsideValidRange() {
        QualityWeights weights = QualityWeights.defaultWeights();
        
        assertThrows(IllegalArgumentException.class, 
            () -> weights.withWeight(QualityDimension.COMPLETENESS, -0.1));
        assertThrows(IllegalArgumentException.class, 
            () -> weights.withWeight(QualityDimension.ACCURACY, 1.5));
    }

    @Test
    @DisplayName("Should create custom weights for completeness-focused assessment")
    void shouldCreateCompletenessFocusedWeights() {
        QualityWeights weights = new QualityWeights(0.40, 0.20, 0.15, 0.10, 0.10, 0.05);
        
        assertEquals(0.40, weights.completeness(), 0.001);
        assertTrue(weights.completeness() > weights.accuracy());
        assertTrue(weights.completeness() > weights.consistency());
    }

    @Test
    @DisplayName("Should create custom weights for accuracy-focused assessment")
    void shouldCreateAccuracyFocusedWeights() {
        QualityWeights weights = new QualityWeights(0.20, 0.40, 0.15, 0.10, 0.10, 0.05);
        
        assertEquals(0.40, weights.accuracy(), 0.001);
        assertTrue(weights.accuracy() > weights.completeness());
        assertTrue(weights.accuracy() > weights.consistency());
    }

    @Test
    @DisplayName("Should validate that all dimension weights are accessible")
    void shouldAccessAllDimensionWeights() {
        QualityWeights weights = new QualityWeights(0.3, 0.25, 0.2, 0.15, 0.07, 0.03);
        
        for (QualityDimension dimension : QualityDimension.values()) {
            double weight = weights.getWeight(dimension);
            assertTrue(weight >= 0.0 && weight <= 1.0);
        }
    }

    @Test
    @DisplayName("Should handle zero weights for non-critical dimensions")
    void shouldHandleZeroWeights() {
        // Valid scenario where some dimensions have zero weight
        QualityWeights weights = new QualityWeights(0.5, 0.5, 0.0, 0.0, 0.0, 0.0);
        
        assertEquals(0.5, weights.completeness(), 0.001);
        assertEquals(0.5, weights.accuracy(), 0.001);
        assertEquals(0.0, weights.consistency(), 0.001);
        assertEquals(0.0, weights.timeliness(), 0.001);
        assertEquals(0.0, weights.uniqueness(), 0.001);
        assertEquals(0.0, weights.validity(), 0.001);
    }
}
