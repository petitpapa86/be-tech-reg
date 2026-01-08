package com.bcbs239.regtech.core.application.recommendations;

import com.bcbs239.regtech.core.domain.recommendations.RecommendationRule;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Provides localized recommendation messages and content.
 * 
 * Supports internationalization (i18n) for:
 * - Italian (it) - Primary language for BCBS 239 reporting
 * - English (en) - Secondary language
 * 
 * Architecture: Application layer service (localization)
 * Dependencies: Domain models (RecommendationRule)
 */
@Component
public class LocalizedRecommendationProvider {
    
    /**
     * Get localized message from recommendation rule.
     * 
     * @param rule The recommendation rule with localized messages
     * @param languageCode Language code (it, en)
     * @return Localized message, or empty string if not found
     */
    public String getLocalizedMessage(RecommendationRule rule, String languageCode) {
        Map<String, String> messages = rule.localizedMessages();
        
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        
        // Try requested language first
        String message = messages.get(languageCode);
        if (message != null && !message.isBlank()) {
            return message;
        }
        
        // Fallback to Italian (primary language)
        message = messages.get("it");
        if (message != null && !message.isBlank()) {
            return message;
        }
        
        // Fallback to English
        message = messages.get("en");
        if (message != null && !message.isBlank()) {
            return message;
        }
        
        // Last resort: return any available message
        return messages.values().stream()
            .filter(m -> m != null && !m.isBlank())
            .findFirst()
            .orElse("");
    }
    
    /**
     * Get localized actions/recommendations from rule.
     * 
     * @param rule The recommendation rule with localized actions
     * @param languageCode Language code (it, en)
     * @return List of localized actions, or empty list if not found
     */
    public java.util.List<String> getLocalizedActions(RecommendationRule rule, String languageCode) {
        Map<String, java.util.List<String>> actions = rule.localizedActions();
        
        if (actions == null || actions.isEmpty()) {
            return java.util.List.of();
        }
        
        // Try requested language first
        java.util.List<String> actionList = actions.get(languageCode);
        if (actionList != null && !actionList.isEmpty()) {
            return actionList;
        }
        
        // Fallback to Italian (primary language)
        actionList = actions.get("it");
        if (actionList != null && !actionList.isEmpty()) {
            return actionList;
        }
        
        // Fallback to English
        actionList = actions.get("en");
        if (actionList != null && !actionList.isEmpty()) {
            return actionList;
        }
        
        // Last resort: return any available actions
        return actions.values().stream()
            .filter(list -> list != null && !list.isEmpty())
            .findFirst()
            .orElse(java.util.List.of());
    }
    
    /**
     * Check if a language is supported.
     * 
     * @param languageCode Language code to check
     * @return true if supported, false otherwise
     */
    public boolean isLanguageSupported(String languageCode) {
        return "it".equals(languageCode) || "en".equals(languageCode);
    }
    
    /**
     * Get default language code (Italian for BCBS 239).
     */
    public String getDefaultLanguage() {
        return "it";
    }
    
    /**
     * Normalize language code to supported format.
     * 
     * Examples:
     * - "IT" -> "it"
     * - "it-IT" -> "it"
     * - "en-US" -> "en"
     * - "fr" -> "it" (fallback to default)
     * 
     * @param languageCode Language code to normalize
     * @return Normalized language code
     */
    public String normalizeLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return getDefaultLanguage();
        }
        
        // Extract base language code (before hyphen/underscore)
        String baseCode = languageCode.toLowerCase().split("[-_]")[0];
        
        // Check if supported
        if (isLanguageSupported(baseCode)) {
            return baseCode;
        }
        
        // Fallback to default
        return getDefaultLanguage();
    }
}
