package com.bcbs239.regtech.core.application.recommendations;

import com.bcbs239.regtech.core.domain.quality.QualityDimension;
import com.bcbs239.regtech.core.domain.quality.QualityThresholds;
import com.bcbs239.regtech.core.domain.recommendations.QualityInsight;
import com.bcbs239.regtech.core.domain.recommendations.RecommendationRule;
import com.bcbs239.regtech.core.domain.recommendations.RecommendationSeverity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main orchestrator for generating quality recommendations and insights.
 * 
 * Uses YAML-driven rules to analyze quality metrics and generate:
 * - Critical situation alerts
 * - Dimension-specific recommendations
 * - Action plans
 * - Success messages for excellent dimensions
 * 
 * Architecture: Application layer service (orchestration)
 * Dependencies: Domain models, InsightRuleEvaluator, LocalizedRecommendationProvider
 */
@Service
public class RecommendationEngine {
    
    private final InsightRuleEvaluator ruleEvaluator;
    private final DimensionRecommendationService dimensionService;
    private final LocalizedRecommendationProvider localizationProvider;
    
    public RecommendationEngine(
        InsightRuleEvaluator ruleEvaluator,
        DimensionRecommendationService dimensionService,
        LocalizedRecommendationProvider localizationProvider
    ) {
        this.ruleEvaluator = ruleEvaluator;
        this.dimensionService = dimensionService;
        this.localizationProvider = localizationProvider;
    }
    
    /**
     * Generate comprehensive quality insights and recommendations.
     * 
     * @param overallScore Overall quality score (0-100)
     * @param dimensionScores Map of dimension scores
     * @param languageCode Language for localization (it, en)
     * @return List of quality insights with recommendations
     */
    public List<QualityInsight> generateInsights(
        BigDecimal overallScore,
        Map<QualityDimension, BigDecimal> dimensionScores,
        String languageCode
    ) {
        List<QualityInsight> insights = new ArrayList<>();
        
        // 1. Load rules and thresholds
        List<RecommendationRule> rules = ruleEvaluator.loadRules();
        QualityThresholds thresholds = ruleEvaluator.loadThresholds();
        
        // 2. Evaluate each rule in priority order
        for (RecommendationRule rule : rules) {
            boolean ruleMatches = ruleEvaluator.evaluateRule(
                rule,
                overallScore,
                dimensionScores,
                thresholds
            );
            
            if (ruleMatches) {
                QualityInsight insight = buildInsight(
                    rule,
                    overallScore,
                    dimensionScores,
                    thresholds,
                    languageCode
                );
                
                if (insight != null) {
                    insights.add(insight);
                }
            }
        }
        
        return insights;
    }
    
    /**
     * Build a quality insight from a matched rule.
     */
    private QualityInsight buildInsight(
        RecommendationRule rule,
        BigDecimal overallScore,
        Map<QualityDimension, BigDecimal> dimensionScores,
        QualityThresholds thresholds,
        String languageCode
    ) {
        // Get localized message from rule
        String message = localizationProvider.getLocalizedMessage(rule, languageCode);
        
        // Replace placeholders in message template
        message = replacePlaceholders(message, overallScore, dimensionScores);
        
        // Get dimension-specific recommendations
        List<String> recommendations = dimensionService.generateRecommendations(
            rule,
            dimensionScores,
            thresholds,
            languageCode
        );
        
        // Parse language code to Locale
        Locale locale = Locale.forLanguageTag(languageCode);
        
        return new QualityInsight(
            rule.id(),
            rule.severity(),
            message,
            recommendations,
            locale
        );
    }
    
    /**
     * Replace placeholders in message template with actual values.
     * 
     * Supported placeholders:
     * - {{overallScore}} - Overall quality score
     * - {{dimension.completeness}} - Completeness score
     * - {{dimension.accuracy}} - Accuracy score
     * - etc.
     */
    private String replacePlaceholders(
        String template,
        BigDecimal overallScore,
        Map<QualityDimension, BigDecimal> dimensionScores
    ) {
        String result = template;
        
        // Replace overall score
        if (overallScore != null) {
            result = result.replace("{{overallScore}}", 
                String.format("%.1f", overallScore.doubleValue()));
        }
        
        // Replace dimension scores
        for (Map.Entry<QualityDimension, BigDecimal> entry : dimensionScores.entrySet()) {
            String placeholder = "{{dimension." + entry.getKey().name().toLowerCase() + "}}";
            String value = String.format("%.1f", entry.getValue().doubleValue());
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
    
    /**
     * Determine overall severity based on score and thresholds.
     */
    public RecommendationSeverity determineSeverity(
        BigDecimal overallScore,
        QualityThresholds thresholds
    ) {
        double score = overallScore.doubleValue();
        
        if (thresholds.isExcellent(score)) {
            return RecommendationSeverity.SUCCESS;
        } else if (thresholds.isGood(score)) {
            return RecommendationSeverity.LOW;
        } else if (thresholds.isAcceptable(score)) {
            return RecommendationSeverity.MEDIUM;
        } else if (thresholds.isPoor(score)) {
            return RecommendationSeverity.HIGH;
        } else {
            return RecommendationSeverity.CRITICAL;
        }
    }
}
