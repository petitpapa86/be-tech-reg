package com.bcbs239.regtech.core.application.recommendations.ports;

import com.bcbs239.regtech.core.domain.quality.QualityDimension;
import com.bcbs239.regtech.core.domain.quality.QualityThresholds;
import com.bcbs239.regtech.core.domain.recommendations.RecommendationRule;

import java.util.List;
import java.util.Map;

/**
 * Port interface for loading recommendation configuration.
 * 
 * This is a **port** in Clean Architecture - defined in application layer,
 * implemented in infrastructure layer (adapter pattern).
 * 
 * Implementations can load from:
 * - YAML files (YamlRecommendationRuleLoader)
 * - Database (future)
 * - External service (future)
 * 
 * Architecture: Application layer port interface
 * Implementations: Infrastructure layer adapters
 */
public interface RecommendationRuleLoader {
    
    /**
     * Load all recommendation rules.
     * 
     * @return List of recommendation rules with conditions and messages
     */
    List<RecommendationRule> loadRules();
    
    /**
     * Load quality thresholds for all dimensions.
     * 
     * @return Quality thresholds for scoring and grading
     */
    QualityThresholds loadThresholds();
    
    /**
     * Load dimension-specific recommendations in specified language.
     * 
     * @param languageCode Language code (it, en)
     * @return Map of dimension to localized recommendation text
     */
    Map<QualityDimension, String> loadDimensionRecommendations(String languageCode);
}
