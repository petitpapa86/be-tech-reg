package com.bcbs239.regtech.core.infrastructure.recommendations;

import com.bcbs239.regtech.core.application.recommendations.ports.RecommendationRuleLoader;
import com.bcbs239.regtech.core.domain.quality.QualityDimension;
import com.bcbs239.regtech.core.domain.quality.QualityThresholds;
import com.bcbs239.regtech.core.domain.recommendations.RecommendationRule;
import com.bcbs239.regtech.core.domain.recommendations.RecommendationSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * YAML-based implementation of RecommendationRuleLoader.
 * 
 * Loads recommendation rules, thresholds, and localized messages from
 * quality-recommendations-config.yaml (677 lines).
 * 
 * Architecture: Infrastructure layer adapter (implements port)
 * Port Interface: RecommendationRuleLoader (application layer)
 */
@Component
public class YamlRecommendationRuleLoader implements RecommendationRuleLoader {
    
    private static final Logger log = LoggerFactory.getLogger(YamlRecommendationRuleLoader.class);
    private static final String CONFIG_FILE = "quality-recommendations-config.yaml";
    
    private Map<String, Object> yamlConfig;
    
    /**
     * Constructor loads YAML file on startup
     */
    public YamlRecommendationRuleLoader() {
        loadYamlConfig();
    }
    
    /**
     * Load YAML configuration file from classpath
     */
    @SuppressWarnings("unchecked")
    private void loadYamlConfig() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException("YAML configuration file not found: " + CONFIG_FILE);
            }
            
            Yaml yaml = new Yaml();
            this.yamlConfig = yaml.load(inputStream);
            
            log.info("Successfully loaded YAML configuration from {}", CONFIG_FILE);
            
        } catch (IOException e) {
            log.error("Failed to load YAML configuration from {}", CONFIG_FILE, e);
            throw new RuntimeException("Failed to load recommendation rules configuration", e);
        }
    }
    
    /**
     * Load quality thresholds from YAML
     * 
     * Returns thresholds matching color-rules-config-COMPLETE.yaml:
     * - excellent: 90%
     * - good: 85%
     * - acceptable: 75%
     * - poor: 65%
     * - critical: < 65%
     */
    @Override
    @SuppressWarnings("unchecked")
    public QualityThresholds loadThresholds() {
        try {
            // Navigate to quality_insights.severity_thresholds
            Map<String, Object> qualityInsights = (Map<String, Object>) yamlConfig.get("quality_insights");
            if (qualityInsights == null) {
                log.warn("quality_insights section not found in YAML, using defaults");
                return QualityThresholds.bcbs239Defaults();
            }
            
            Map<String, Object> severityThresholds = (Map<String, Object>) qualityInsights.get("severity_thresholds");
            if (severityThresholds == null) {
                log.warn("severity_thresholds section not found in YAML, using defaults");
                return QualityThresholds.bcbs239Defaults();
            }
            
            // Extract overall thresholds from severity levels
            Map<String, Object> critical = (Map<String, Object>) severityThresholds.get("critical");
            Map<String, Object> high = (Map<String, Object>) severityThresholds.get("high");
            Map<String, Object> medium = (Map<String, Object>) severityThresholds.get("medium");
            Map<String, Object> low = (Map<String, Object>) severityThresholds.get("low");
            
            // Map YAML severity thresholds to QualityThresholds
            // critical: < 65%, high: < 75%, medium: < 85%, low: >= 85%
            double criticalThreshold = getDoubleValue(critical, "overall_score_below", 65.0);
            double highThreshold = getDoubleValue(high, "overall_score_below", 75.0);
            double mediumThreshold = getDoubleValue(medium, "overall_score_below", 85.0);
            double lowThreshold = getDoubleValue(low, "overall_score_above", 85.0);
            
            // Also read dimension thresholds from dimension_scores section
            Map<String, Object> dimensionScores = (Map<String, Object>) yamlConfig.get("dimension_scores");
            Map<String, Object> dimensionThresholds = dimensionScores != null 
                ? (Map<String, Object>) dimensionScores.get("thresholds")
                : null;
            
            double excellentThreshold = 90.0;  // Default
            double acceptableThreshold = 75.0;  // Default
            
            if (dimensionThresholds != null) {
                Map<String, Object> excellent = (Map<String, Object>) dimensionThresholds.get("excellent");
                Map<String, Object> acceptable = (Map<String, Object>) dimensionThresholds.get("acceptable");
                
                if (excellent != null) {
                    excellentThreshold = getDoubleValue(excellent, "value", 90.0);
                }
                if (acceptable != null) {
                    acceptableThreshold = getDoubleValue(acceptable, "value", 75.0);
                }
            }

            // Read error_distribution thresholds
            Map<String, Object> errorDistribution = (Map<String, Object>) yamlConfig.get("error_distribution");
            Map<String, Object> errorThresholds = errorDistribution != null 
                ? (Map<String, Object>) errorDistribution.get("thresholds")
                : null;
            
            int violationCritical = 100;
            int violationHigh = 50;
            int violationMedium = 10;
            
            if (errorThresholds != null) {
                Map<String, Object> errCritical = (Map<String, Object>) errorThresholds.get("critical");
                Map<String, Object> errHigh = (Map<String, Object>) errorThresholds.get("high");
                Map<String, Object> errMedium = (Map<String, Object>) errorThresholds.get("medium");
                
                if (errCritical != null) violationCritical = getIntValue(errCritical, "value", 100);
                if (errHigh != null) violationHigh = getIntValue(errHigh, "value", 50);
                if (errMedium != null) violationMedium = getIntValue(errMedium, "value", 10);
            }
            
            // Build QualityThresholds with values from YAML
            return new QualityThresholds(
                excellentThreshold,      // 90.0% - excellent
                lowThreshold,            // 85.0% - good
                acceptableThreshold,     // 75.0% - acceptable
                criticalThreshold,       // 65.0% - poor (lower bound)
                criticalThreshold,       // 65.0% - critical (same value)
                // Dimension-specific thresholds (all use same excellent/acceptable)
                excellentThreshold, acceptableThreshold,  // Completeness
                excellentThreshold, acceptableThreshold,  // Accuracy
                excellentThreshold, acceptableThreshold,  // Consistency
                excellentThreshold, acceptableThreshold,  // Timeliness
                excellentThreshold, acceptableThreshold,  // Uniqueness
                excellentThreshold, acceptableThreshold,  // Validity
                violationCritical, violationHigh, violationMedium // Violations
            );
            
        } catch (Exception e) {
            log.error("Failed to parse thresholds from YAML, using defaults", e);
            return QualityThresholds.bcbs239Defaults();
        }
    }

    /**
     * Safely extract int value from map
     */
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        if (map == null || !map.containsKey(key)) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse int value for key {}: {}, using default {}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Load recommendation rules from YAML
     * 
     * Returns list of RecommendationRule objects parsed from quality_insights.insight_rules
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<RecommendationRule> loadRules() {
        List<RecommendationRule> rules = new ArrayList<>();
        
        try {
            Map<String, Object> qualityInsights = (Map<String, Object>) yamlConfig.get("quality_insights");
            if (qualityInsights == null) {
                log.warn("quality_insights section not found in YAML");
                return rules;
            }
            
            List<Map<String, Object>> insightRules = (List<Map<String, Object>>) qualityInsights.get("insight_rules");
            if (insightRules == null) {
                log.warn("insight_rules section not found in YAML");
                return rules;
            }
            
            for (Map<String, Object> ruleConfig : insightRules) {
                try {
                    RecommendationRule rule = parseRule(ruleConfig);
                    rules.add(rule);
                } catch (Exception e) {
                    log.error("Failed to parse rule: {}", ruleConfig.get("id"), e);
                }
            }
            
            log.info("Loaded {} recommendation rules from YAML", rules.size());
            
        } catch (Exception e) {
            log.error("Failed to load recommendation rules from YAML", e);
        }
        
        return rules;
    }
    
    /**
     * Parse a single rule from YAML config
     */
    @SuppressWarnings("unchecked")
    private RecommendationRule parseRule(Map<String, Object> ruleConfig) {
        String id = (String) ruleConfig.get("id");
        Integer priority = (Integer) ruleConfig.get("priority");
        
        Map<String, Object> condition = (Map<String, Object>) ruleConfig.get("condition");
        Map<String, String> conditions = new HashMap<>();
        if (condition != null) {
            condition.forEach((key, value) -> conditions.put(key, String.valueOf(value)));
        }
        
        Map<String, Object> output = (Map<String, Object>) ruleConfig.get("output");
        String severityStr = output != null ? (String) output.get("severity") : "medium";
        RecommendationSeverity severity = parseSeverity(severityStr);
        
        Map<String, String> localizedMessages = new HashMap<>();
        if (output != null) {
            String titleIt = (String) output.get("title_it");
            String titleEn = (String) output.get("title_en");
            String contentIt = (String) output.get("content_template_it");
            String contentEn = (String) output.get("content_template_en");
            
            if (titleIt != null && contentIt != null) {
                localizedMessages.put("it", titleIt + ": " + contentIt);
            }
            if (titleEn != null && contentEn != null) {
                localizedMessages.put("en", titleEn + ": " + contentEn);
            }
        }
        
        // Parse dimension-specific recommendations (actions)
        Map<String, List<String>> localizedActions = new HashMap<>();
        if (output != null) {
            Map<String, Object> recommendations = (Map<String, Object>) output.get("recommendations");
            if (recommendations != null) {
                for (QualityDimension dimension : QualityDimension.values()) {
                    String dimensionKey = dimension.name().toLowerCase();
                    Map<String, Object> dimRec = (Map<String, Object>) recommendations.get(dimensionKey);
                    if (dimRec != null) {
                        String actionIt = (String) dimRec.get("it");
                        String actionEn = (String) dimRec.get("en");
                        if (actionIt != null) {
                            localizedActions.put("it", Arrays.asList(actionIt));
                        }
                        if (actionEn != null) {
                            localizedActions.put("en", Arrays.asList(actionEn));
                        }
                    }
                }
            }
        }
        
        return new RecommendationRule(
            id,
            priority != null ? priority : 999,
            conditions,
            severity,
            localizedMessages,
            localizedActions.isEmpty() ? null : localizedActions
        );
    }
    
    /**
     * Parse severity string to enum
     */
    private RecommendationSeverity parseSeverity(String severityStr) {
        if (severityStr == null || "dynamic".equals(severityStr)) {
            return RecommendationSeverity.MEDIUM;  // Default
        }
        
        return switch (severityStr.toLowerCase()) {
            case "critical" -> RecommendationSeverity.CRITICAL;
            case "high" -> RecommendationSeverity.HIGH;
            case "medium" -> RecommendationSeverity.MEDIUM;
            case "low" -> RecommendationSeverity.LOW;
            case "success", "info" -> RecommendationSeverity.SUCCESS;
            default -> RecommendationSeverity.MEDIUM;
        };
    }
    
    /**
     * Safely extract double value from map
     */
    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        if (map == null || !map.containsKey(key)) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse double value for key {}: {}, using default {}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get dimension-specific recommendations for a given locale
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<QualityDimension, String> loadDimensionRecommendations(String languageCode) {
        Map<QualityDimension, String> recommendations = new HashMap<>();
        
        try {
            Map<String, Object> qualityInsights = (Map<String, Object>) yamlConfig.get("quality_insights");
            if (qualityInsights == null) return recommendations;
            
            List<Map<String, Object>> insightRules = (List<Map<String, Object>>) qualityInsights.get("insight_rules");
            if (insightRules == null) return recommendations;
            
            // Find dimension_below_threshold rule
            for (Map<String, Object> rule : insightRules) {
                if ("dimension_below_threshold".equals(rule.get("id"))) {
                    Map<String, Object> output = (Map<String, Object>) rule.get("output");
                    if (output != null) {
                        Map<String, Object> dimRecommendations = (Map<String, Object>) output.get("recommendations");
                        if (dimRecommendations != null) {
                            for (QualityDimension dimension : QualityDimension.values()) {
                                String dimensionKey = dimension.name().toLowerCase();
                                Map<String, Object> dimRec = (Map<String, Object>) dimRecommendations.get(dimensionKey);
                                if (dimRec != null) {
                                    String text = (String) dimRec.get(languageCode);
                                    if (text != null) {
                                        recommendations.put(dimension, text);
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to load dimension recommendations", e);
        }
        
        return recommendations;
    }
}
