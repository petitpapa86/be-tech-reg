package com.bcbs239.regtech.core.domain.recommendations;

import java.util.List;

/**
 * Represents a recommendation section in the comprehensive report
 * 
 * Each section contains contextual guidance based on specific quality issues found
 * in the data validation results.
 * 
 * This is the SHARED version - moved from report-generation module to core for reuse.
 */
public record RecommendationSection(
    String icon,                // Icon to display (emoji or icon class)
    String colorClass,          // CSS color class for styling (e.g., "red", "yellow", "green")
    String title,               // Section title
    String content,             // Main content/description
    List<String> bullets        // List of bullet points with specific recommendations
) {
    
    /**
     * Create a recommendation section with no bullets
     */
    public static RecommendationSection withoutBullets(
        String icon,
        String colorClass,
        String title,
        String content
    ) {
        return new RecommendationSection(icon, colorClass, title, content, List.of());
    }
    
    /**
     * Check if this section has bullet points
     */
    public boolean hasBullets() {
        return bullets != null && !bullets.isEmpty();
    }
}
