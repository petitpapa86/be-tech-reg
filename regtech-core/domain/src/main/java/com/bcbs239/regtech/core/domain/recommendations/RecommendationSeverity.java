package com.bcbs239.regtech.core.domain.recommendations;

/**
 * Severity levels for quality recommendations.
 * Maps to color-rules-config-COMPLETE.yaml severity thresholds.
 */
public enum RecommendationSeverity {
    /**
     * Critical situation (score < 65%)
     * Red badge, highest priority
     */
    CRITICAL("🚨", "red", 5),
    
    /**
     * High priority issues (score 65-75%)
     * Orange badge, needs attention
     */
    HIGH("⚠️", "orange", 4),
    
    /**
     * Medium priority improvements (score 75-85%)
     * Yellow badge, recommended actions
     */
    MEDIUM("⚡", "yellow", 3),
    
    /**
     * Low priority suggestions (score 85-90%)
     * Blue badge, nice-to-have improvements
     */
    LOW("ℹ️", "blue", 2),
    
    /**
     * Success level (score >= 90%)
     * Green badge, excellence achieved
     */
    SUCCESS("✅", "green", 1);
    
    private final String icon;
    private final String color;
    private final int priority;
    
    RecommendationSeverity(String icon, String color, int priority) {
        this.icon = icon;
        this.color = color;
        this.priority = priority;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public String getColor() {
        return color;
    }
    
    public int getPriority() {
        return priority;
    }
    
    /**
     * Determine severity based on quality score percentage.
     * Matches thresholds from color-rules-config-COMPLETE.yaml
     */
    public static RecommendationSeverity fromScore(double scorePercentage) {
        if (scorePercentage >= 90.0) {
            return SUCCESS;
        } else if (scorePercentage >= 85.0) {
            return LOW;
        } else if (scorePercentage >= 75.0) {
            return MEDIUM;
        } else if (scorePercentage >= 65.0) {
            return HIGH;
        } else {
            return CRITICAL;
        }
    }
}
