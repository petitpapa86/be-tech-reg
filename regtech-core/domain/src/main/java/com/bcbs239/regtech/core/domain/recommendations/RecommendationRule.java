package com.bcbs239.regtech.core.domain.recommendations;

import java.util.List;
import java.util.Map;

/**
 * Represents a recommendation rule from color-rules-config-COMPLETE.yaml
 * 
 * Example YAML structure:
 * ```yaml
 * insight_rules:
 *   - id: "critical_situation"
 *     priority: 1
 *     conditions:
 *       overall_score: "< 65"
 *     severity: "critical"
 *     localized_messages:
 *       it: "Situazione Critica"
 *       en: "Critical Situation"
 * ```
 */
public record RecommendationRule(
    String id,                          // Unique rule identifier (e.g., "critical_situation")
    int priority,                       // Rule priority (1 = highest)
    Map<String, String> conditions,     // Evaluation conditions (e.g., "overall_score": "< 65")
    RecommendationSeverity severity,    // Severity level (CRITICAL, HIGH, MEDIUM, LOW, SUCCESS)
    Map<String, String> localizedMessages,  // Localized messages (e.g., "it": "...", "en": "...")
    Map<String, List<String>> localizedActions  // Localized action lists (optional)
) {
    
    /**
     * Get localized message for given language code (e.g., "it", "en")
     * Falls back to English if language not found
     */
    public String getMessage(String languageCode) {
        return localizedMessages.getOrDefault(
            languageCode,
            localizedMessages.getOrDefault("en", "No message available")
        );
    }
    
    /**
     * Get localized actions for given language code
     * Returns empty list if no actions defined
     */
    public List<String> getActions(String languageCode) {
        if (localizedActions == null) {
            return List.of();
        }
        return localizedActions.getOrDefault(
            languageCode,
            localizedActions.getOrDefault("en", List.of())
        );
    }
    
    /**
     * Check if this rule has actions defined
     */
    public boolean hasActions() {
        return localizedActions != null && !localizedActions.isEmpty();
    }
}
