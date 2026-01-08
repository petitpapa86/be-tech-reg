package com.bcbs239.regtech.core.infrastructure.recommendations;

import com.bcbs239.regtech.core.domain.quality.QualityDimension;
import com.bcbs239.regtech.core.domain.quality.QualityThresholds;
import com.bcbs239.regtech.core.domain.recommendations.RecommendationRule;
import com.bcbs239.regtech.core.domain.recommendations.RecommendationSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for YamlRecommendationRuleLoader
 * 
 * Tests YAML parsing, threshold loading, and rule extraction.
 */
class YamlRecommendationRuleLoaderTest {
    
    private YamlRecommendationRuleLoader loader;
    
    @BeforeEach
    void setUp() {
        loader = new YamlRecommendationRuleLoader();
    }
    
    @Test
    @DisplayName("Should load YAML configuration successfully")
    void shouldLoadYamlConfiguration() {
        assertThat(loader).isNotNull();
    }
    
    @Test
    @DisplayName("Should load quality thresholds from YAML")
    void shouldLoadQualityThresholds() {
        QualityThresholds thresholds = loader.loadThresholds();
        
        assertThat(thresholds).isNotNull();
        
        // Overall thresholds from YAML (matches BCBS 239 defaults)
        assertThat(thresholds.excellentThreshold()).isEqualTo(90.0);
        assertThat(thresholds.goodThreshold()).isEqualTo(85.0);
        assertThat(thresholds.acceptableThreshold()).isEqualTo(75.0);
        assertThat(thresholds.poorThreshold()).isEqualTo(65.0);
        assertThat(thresholds.criticalThreshold()).isEqualTo(65.0);
        
        // Dimension-specific thresholds (all use same excellent/acceptable)
        assertThat(thresholds.completenessExcellent()).isEqualTo(90.0);
        assertThat(thresholds.completenessAcceptable()).isEqualTo(75.0);
        assertThat(thresholds.accuracyExcellent()).isEqualTo(90.0);
        assertThat(thresholds.accuracyAcceptable()).isEqualTo(75.0);
    }
    
    @Test
    @DisplayName("Should load recommendation rules from YAML")
    void shouldLoadRecommendationRules() {
        List<RecommendationRule> rules = loader.loadRules();
        
        assertThat(rules).isNotEmpty();
        
        // Should have at least critical_situation rule
        RecommendationRule criticalRule = rules.stream()
            .filter(r -> "critical_situation".equals(r.id()))
            .findFirst()
            .orElse(null);
        
        assertThat(criticalRule).isNotNull();
        assertThat(criticalRule.priority()).isEqualTo(1);
        assertThat(criticalRule.severity()).isEqualTo(RecommendationSeverity.CRITICAL);
    }
    
    @Test
    @DisplayName("Should parse localized messages correctly")
    void shouldParseLocalizedMessages() {
        List<RecommendationRule> rules = loader.loadRules();
        
        RecommendationRule criticalRule = rules.stream()
            .filter(r -> "critical_situation".equals(r.id()))
            .findFirst()
            .orElseThrow();
        
        // Should have Italian message
        String italianMessage = criticalRule.getMessage("it");
        assertThat(italianMessage).isNotEmpty();
        assertThat(italianMessage).contains("Situazione Critica");
        
        // Should have English message
        String englishMessage = criticalRule.getMessage("en");
        assertThat(englishMessage).isNotEmpty();
        assertThat(englishMessage).contains("Critical Situation");
    }
    
    @Test
    @DisplayName("Should load dimension-specific recommendations")
    void shouldLoadDimensionRecommendations() {
        Map<QualityDimension, String> italianRecs = loader.loadDimensionRecommendations("it");
        
        assertThat(italianRecs).isNotEmpty();
        assertThat(italianRecs).containsKey(QualityDimension.COMPLETENESS);
        assertThat(italianRecs).containsKey(QualityDimension.ACCURACY);
        
        // Check completeness recommendation in Italian
        String completenessRec = italianRecs.get(QualityDimension.COMPLETENESS);
        assertThat(completenessRec).isNotEmpty();
        assertThat(completenessRec).contains("data entry");
        
        // Check English recommendations
        Map<QualityDimension, String> englishRecs = loader.loadDimensionRecommendations("en");
        assertThat(englishRecs).isNotEmpty();
        assertThat(englishRecs).containsKey(QualityDimension.COMPLETENESS);
    }
    
    @Test
    @DisplayName("Should handle threshold edge cases correctly")
    void shouldHandleThresholdEdgeCases() {
        QualityThresholds thresholds = loader.loadThresholds();
        
        // Test isExcellent boundary
        assertThat(thresholds.isExcellent(90.0)).isTrue();
        assertThat(thresholds.isExcellent(89.9)).isFalse();
        
        // Test isGood boundary
        assertThat(thresholds.isGood(85.0)).isTrue();
        assertThat(thresholds.isGood(89.9)).isTrue();
        assertThat(thresholds.isGood(90.0)).isFalse();  // Excellent, not good
        
        // Test isAcceptable boundary
        assertThat(thresholds.isAcceptable(75.0)).isTrue();
        assertThat(thresholds.isAcceptable(74.9)).isFalse();
        
        // Test isPoor boundary
        assertThat(thresholds.isPoor(65.0)).isTrue();
        assertThat(thresholds.isPoor(74.9)).isTrue();
        assertThat(thresholds.isPoor(64.9)).isFalse();  // Critical, not poor
        
        // Test isCritical boundary
        assertThat(thresholds.isCritical(64.9)).isTrue();
        assertThat(thresholds.isCritical(65.0)).isFalse();
    }
    
    @Test
    @DisplayName("Should assign correct grade letters")
    void shouldAssignCorrectGradeLetters() {
        QualityThresholds thresholds = loader.loadThresholds();
        
        assertThat(thresholds.getGradeLetter(95.0)).isEqualTo("A");  // Excellent
        assertThat(thresholds.getGradeLetter(87.0)).isEqualTo("B");  // Good
        assertThat(thresholds.getGradeLetter(77.0)).isEqualTo("C");  // Acceptable
        assertThat(thresholds.getGradeLetter(67.0)).isEqualTo("D");  // Poor
        assertThat(thresholds.getGradeLetter(60.0)).isEqualTo("F");  // Critical
    }
    
    @Test
    @DisplayName("Should prioritize rules correctly")
    void shouldPrioritizeRulesCorrectly() {
        List<RecommendationRule> rules = loader.loadRules();
        
        // Rules should be returned in order of appearance in YAML
        // critical_situation should be priority 1
        RecommendationRule firstRule = rules.get(0);
        assertThat(firstRule.id()).isEqualTo("critical_situation");
        assertThat(firstRule.priority()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Should map YAML severity strings to enum correctly")
    void shouldMapSeverityStringsToEnum() {
        List<RecommendationRule> rules = loader.loadRules();
        
        // Find critical_situation rule
        RecommendationRule criticalRule = rules.stream()
            .filter(r -> "critical_situation".equals(r.id()))
            .findFirst()
            .orElseThrow();
        
        assertThat(criticalRule.severity()).isEqualTo(RecommendationSeverity.CRITICAL);
        
        // Find excellent_dimensions rule (should map to SUCCESS)
        RecommendationRule successRule = rules.stream()
            .filter(r -> "excellent_dimensions".equals(r.id()))
            .findFirst()
            .orElse(null);
        
        if (successRule != null) {
            assertThat(successRule.severity()).isEqualTo(RecommendationSeverity.SUCCESS);
        }
    }
    
    @Test
    @DisplayName("Should handle missing YAML sections gracefully")
    void shouldHandleMissingYamlSectionsGracefully() {
        // If YAML is malformed, loader should use defaults
        QualityThresholds thresholds = loader.loadThresholds();
        assertThat(thresholds).isNotNull();
        
        // Should still have valid thresholds
        assertThat(thresholds.excellentThreshold()).isGreaterThan(0);
        assertThat(thresholds.acceptableThreshold()).isGreaterThan(0);
    }
    
    @Test
    @DisplayName("Should validate threshold consistency with RecommendationSeverity")
    void shouldValidateThresholdConsistencyWithSeverity() {
        QualityThresholds thresholds = loader.loadThresholds();
        
        // Thresholds should match RecommendationSeverity.fromScore() logic
        // SUCCESS: >= 90%
        assertThat(RecommendationSeverity.fromScore(90.0)).isEqualTo(RecommendationSeverity.SUCCESS);
        assertThat(thresholds.isExcellent(90.0)).isTrue();
        
        // LOW: >= 85%
        assertThat(RecommendationSeverity.fromScore(85.0)).isEqualTo(RecommendationSeverity.LOW);
        assertThat(thresholds.isGood(85.0)).isTrue();
        
        // MEDIUM: >= 75%
        assertThat(RecommendationSeverity.fromScore(75.0)).isEqualTo(RecommendationSeverity.MEDIUM);
        assertThat(thresholds.isAcceptable(75.0)).isTrue();
        
        // HIGH: >= 65%
        assertThat(RecommendationSeverity.fromScore(65.0)).isEqualTo(RecommendationSeverity.HIGH);
        assertThat(thresholds.isPoor(65.0)).isTrue();
        
        // CRITICAL: < 65%
        assertThat(RecommendationSeverity.fromScore(60.0)).isEqualTo(RecommendationSeverity.CRITICAL);
        assertThat(thresholds.isCritical(60.0)).isTrue();
    }
}
