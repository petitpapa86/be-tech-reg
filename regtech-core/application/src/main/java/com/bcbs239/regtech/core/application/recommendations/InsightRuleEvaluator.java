package com.bcbs239.regtech.core.application.recommendations;

import com.bcbs239.regtech.core.application.recommendations.ports.RecommendationRuleLoader;
import com.bcbs239.regtech.core.domain.quality.QualityDimension;
import com.bcbs239.regtech.core.domain.quality.QualityThresholds;
import com.bcbs239.regtech.core.domain.recommendations.RecommendationRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Evaluates recommendation rules against quality metrics.
 * 
 * Determines which rules match the current quality situation based on:
 * - Overall quality score
 * - Individual dimension scores
 * - Configured thresholds
 * 
 * Architecture: Application layer service (business logic)
 * Dependencies: Infrastructure layer (YamlRecommendationRuleLoader)
 */
@Component
public class InsightRuleEvaluator {
    
    private final RecommendationRuleLoader ruleLoader;
    
    public InsightRuleEvaluator(RecommendationRuleLoader ruleLoader) {
        this.ruleLoader = ruleLoader;
    }
    
    /**
     * Load all recommendation rules from YAML configuration.
     */
    public List<RecommendationRule> loadRules() {
        return ruleLoader.loadRules();
    }
    
    /**
     * Load quality thresholds from YAML configuration.
     */
    public QualityThresholds loadThresholds() {
        return ruleLoader.loadThresholds();
    }
    
    /**
     * Evaluate if a rule matches the current quality situation.
     * 
     * Rule matching logic:
     * - critical_situation: overall score < 65%
     * - dimension_below_threshold: any dimension < threshold
     * - excellent_dimensions: any dimension >= 90%
     * - action_plan: always matches (generic guidance)
     * 
     * @param rule The rule to evaluate
     * @param overallScore Overall quality score
     * @param dimensionScores Individual dimension scores
     * @param thresholds Quality thresholds
     * @return true if rule matches, false otherwise
     */
    public boolean evaluateRule(
        RecommendationRule rule,
        BigDecimal overallScore,
        Map<QualityDimension, BigDecimal> dimensionScores,
        QualityThresholds thresholds
    ) {
        return switch (rule.id()) {
            case "critical_situation" -> evaluateCriticalSituation(overallScore, thresholds);
            case "dimension_below_threshold" -> evaluateDimensionBelowThreshold(dimensionScores, thresholds);
            case "excellent_dimensions" -> evaluateExcellentDimensions(dimensionScores, thresholds);
            case "action_plan" -> evaluateActionPlan(overallScore, thresholds);
            default -> false; // Unknown rule ID
        };
    }
    
    /**
     * Critical situation: overall score below critical threshold (< 65%).
     */
    private boolean evaluateCriticalSituation(BigDecimal overallScore, QualityThresholds thresholds) {
        if (overallScore == null) {
            return false;
        }
        return thresholds.isCritical(overallScore.doubleValue());
    }
    
    /**
     * Dimension below threshold: at least one dimension is below acceptable threshold (< 75%).
     */
    private boolean evaluateDimensionBelowThreshold(
        Map<QualityDimension, BigDecimal> dimensionScores,
        QualityThresholds thresholds
    ) {
        for (Map.Entry<QualityDimension, BigDecimal> entry : dimensionScores.entrySet()) {
            if (entry.getValue() != null) {
                double score = entry.getValue().doubleValue();
                
                // Check against dimension-specific acceptable threshold
                boolean belowThreshold = switch (entry.getKey()) {
                    case COMPLETENESS -> score < thresholds.completenessAcceptable();
                    case ACCURACY -> score < thresholds.accuracyAcceptable();
                    case CONSISTENCY -> score < thresholds.consistencyAcceptable();
                    case TIMELINESS -> score < thresholds.timelinessAcceptable();
                    case UNIQUENESS -> score < thresholds.uniquenessAcceptable();
                    case VALIDITY -> score < thresholds.validityAcceptable();
                };
                
                if (belowThreshold) {
                    return true; // At least one dimension below threshold
                }
            }
        }
        return false;
    }
    
    /**
     * Excellent dimensions: at least one dimension is excellent (>= 90%).
     */
    private boolean evaluateExcellentDimensions(
        Map<QualityDimension, BigDecimal> dimensionScores,
        QualityThresholds thresholds
    ) {
        for (Map.Entry<QualityDimension, BigDecimal> entry : dimensionScores.entrySet()) {
            if (entry.getValue() != null) {
                double score = entry.getValue().doubleValue();
                
                // Check against dimension-specific excellent threshold
                boolean isExcellent = switch (entry.getKey()) {
                    case COMPLETENESS -> score >= thresholds.completenessExcellent();
                    case ACCURACY -> score >= thresholds.accuracyExcellent();
                    case CONSISTENCY -> score >= thresholds.consistencyExcellent();
                    case TIMELINESS -> score >= thresholds.timelinessExcellent();
                    case UNIQUENESS -> score >= thresholds.uniquenessExcellent();
                    case VALIDITY -> score >= thresholds.validityExcellent();
                };
                
                if (isExcellent) {
                    return true; // At least one excellent dimension
                }
            }
        }
        return false;
    }
    
    /**
     * Action plan: always matches if overall score is not excellent (< 90%).
     * Provides generic improvement guidance.
     */
    private boolean evaluateActionPlan(BigDecimal overallScore, QualityThresholds thresholds) {
        if (overallScore == null) {
            return true; // Show guidance if no score available
        }
        // Show action plan if not excellent
        return !thresholds.isExcellent(overallScore.doubleValue());
    }
}
