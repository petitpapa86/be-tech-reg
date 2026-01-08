package com.bcbs239.regtech.core.application.recommendations;

import com.bcbs239.regtech.core.application.recommendations.ports.RecommendationRuleLoader;
import com.bcbs239.regtech.core.domain.quality.QualityDimension;
import com.bcbs239.regtech.core.domain.quality.QualityThresholds;
import com.bcbs239.regtech.core.domain.recommendations.RecommendationRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates dimension-specific recommendations based on quality scores.
 * 
 * Analyzes individual dimension performance and provides:
 * - Targeted improvement actions
 * - Root cause analysis guidance
 * - Best practice recommendations
 * 
 * Architecture: Application layer service (business logic)
 * Dependencies: Infrastructure layer (YamlRecommendationRuleLoader)
 */
@Component
public class DimensionRecommendationService {
    
    private final RecommendationRuleLoader ruleLoader;
    
    public DimensionRecommendationService(RecommendationRuleLoader ruleLoader) {
        this.ruleLoader = ruleLoader;
    }
    
    /**
     * Generate recommendations for dimensions that need improvement.
     * 
     * @param rule The matched recommendation rule
     * @param dimensionScores Current dimension scores
     * @param thresholds Quality thresholds
     * @param languageCode Language for recommendations (it, en)
     * @return List of dimension-specific recommendations
     */
    public List<String> generateRecommendations(
        RecommendationRule rule,
        Map<QualityDimension, BigDecimal> dimensionScores,
        QualityThresholds thresholds,
        String languageCode
    ) {
        return switch (rule.id()) {
            case "dimension_below_threshold" -> 
                generateDimensionImprovementRecommendations(dimensionScores, thresholds, languageCode);
            case "excellent_dimensions" -> 
                generateExcellenceCelebration(dimensionScores, thresholds, languageCode);
            default -> List.of(); // No dimension-specific recommendations for other rules
        };
    }
    
    /**
     * Generate recommendations for dimensions below acceptable threshold.
     */
    private List<String> generateDimensionImprovementRecommendations(
        Map<QualityDimension, BigDecimal> dimensionScores,
        QualityThresholds thresholds,
        String languageCode
    ) {
        List<String> recommendations = new ArrayList<>();
        Map<QualityDimension, String> dimensionRecommendations = 
            ruleLoader.loadDimensionRecommendations(languageCode);
        
        for (Map.Entry<QualityDimension, BigDecimal> entry : dimensionScores.entrySet()) {
            QualityDimension dimension = entry.getKey();
            BigDecimal score = entry.getValue();
            
            if (score != null && isDimensionBelowThreshold(dimension, score.doubleValue(), thresholds)) {
                String recommendation = dimensionRecommendations.get(dimension);
                if (recommendation != null && !recommendation.isBlank()) {
                    recommendations.add(recommendation);
                }
            }
        }
        
        return recommendations;
    }
    
    /**
     * Generate celebration messages for excellent dimensions.
     */
    private List<String> generateExcellenceCelebration(
        Map<QualityDimension, BigDecimal> dimensionScores,
        QualityThresholds thresholds,
        String languageCode
    ) {
        List<String> celebrations = new ArrayList<>();
        
        for (Map.Entry<QualityDimension, BigDecimal> entry : dimensionScores.entrySet()) {
            QualityDimension dimension = entry.getKey();
            BigDecimal score = entry.getValue();
            
            if (score != null && isDimensionExcellent(dimension, score.doubleValue(), thresholds)) {
                String dimensionName = getDimensionDisplayName(dimension, languageCode);
                String message = languageCode.equals("it")
                    ? String.format("✓ %s: Eccellente (%.1f%%)", dimensionName, score.doubleValue())
                    : String.format("✓ %s: Excellent (%.1f%%)", dimensionName, score.doubleValue());
                celebrations.add(message);
            }
        }
        
        return celebrations;
    }
    
    /**
     * Check if dimension is below acceptable threshold.
     */
    private boolean isDimensionBelowThreshold(
        QualityDimension dimension,
        double score,
        QualityThresholds thresholds
    ) {
        return switch (dimension) {
            case COMPLETENESS -> score < thresholds.completenessAcceptable();
            case ACCURACY -> score < thresholds.accuracyAcceptable();
            case CONSISTENCY -> score < thresholds.consistencyAcceptable();
            case TIMELINESS -> score < thresholds.timelinessAcceptable();
            case UNIQUENESS -> score < thresholds.uniquenessAcceptable();
            case VALIDITY -> score < thresholds.validityAcceptable();
        };
    }
    
    /**
     * Check if dimension is excellent.
     */
    private boolean isDimensionExcellent(
        QualityDimension dimension,
        double score,
        QualityThresholds thresholds
    ) {
        return switch (dimension) {
            case COMPLETENESS -> score >= thresholds.completenessExcellent();
            case ACCURACY -> score >= thresholds.accuracyExcellent();
            case CONSISTENCY -> score >= thresholds.consistencyExcellent();
            case TIMELINESS -> score >= thresholds.timelinessExcellent();
            case UNIQUENESS -> score >= thresholds.uniquenessExcellent();
            case VALIDITY -> score >= thresholds.validityExcellent();
        };
    }
    
    /**
     * Get localized display name for dimension.
     */
    private String getDimensionDisplayName(QualityDimension dimension, String languageCode) {
        return switch (dimension) {
            case COMPLETENESS -> languageCode.equals("it") ? "Completezza" : "Completeness";
            case ACCURACY -> languageCode.equals("it") ? "Accuratezza" : "Accuracy";
            case CONSISTENCY -> languageCode.equals("it") ? "Coerenza" : "Consistency";
            case TIMELINESS -> languageCode.equals("it") ? "Tempestività" : "Timeliness";
            case UNIQUENESS -> languageCode.equals("it") ? "Unicità" : "Uniqueness";
            case VALIDITY -> languageCode.equals("it") ? "Validità" : "Validity";
        };
    }
}
