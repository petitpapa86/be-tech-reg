package com.bcbs239.regtech.core.domain.recommendations;

import java.util.List;
import java.util.Locale;

/**
 * Represents a generated quality insight/recommendation
 * 
 * This is the result of evaluating a RecommendationRule against quality data.
 * Contains localized message, actions, and severity information.
 * 
 * Example:
 * ```
 * QualityInsight insight = new QualityInsight(
 *     "critical_situation",
 *     RecommendationSeverity.CRITICAL,
 *     "Situazione Critica: Il punteggio complessivo è molto basso (62%)",
 *     List.of("Verificare la completezza dei dati", "Controllare l'accuratezza"),
 *     Locale.forLanguageTag("it-IT")
 * );
 * ```
 */
public record QualityInsight(
    String ruleId,                      // ID of the rule that generated this insight
    RecommendationSeverity severity,    // Severity level (CRITICAL, HIGH, MEDIUM, LOW, SUCCESS)
    String message,                     // Localized message (with substituted variables)
    List<String> actionItems,           // Localized action items (what to do about it)
    Locale locale                       // Locale used for localization
) {
    
    /**
     * Create an insight with no actions
     */
    public static QualityInsight withoutActions(
        String ruleId,
        RecommendationSeverity severity,
        String message,
        Locale locale
    ) {
        return new QualityInsight(ruleId, severity, message, List.of(), locale);
    }
    
    /**
     * Check if this insight has action items
     */
    public boolean hasActions() {
        return actionItems != null && !actionItems.isEmpty();
    }
    
    /**
     * Get the icon for this insight's severity
     */
    public String getIcon() {
        return severity.getIcon();
    }
    
    /**
     * Get the color for this insight's severity
     */
    public String getColor() {
        return severity.getColor();
    }
    
    /**
     * Get the priority for this insight's severity (higher = more urgent)
     */
    public int getPriority() {
        return severity.getPriority();
    }
    
    /**
     * Get the language code for this insight's locale
     */
    public String getLanguageCode() {
        return locale.getLanguage();
    }
}
