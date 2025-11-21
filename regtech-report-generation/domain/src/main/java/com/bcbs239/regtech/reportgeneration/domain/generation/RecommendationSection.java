package com.bcbs239.regtech.reportgeneration.domain.generation;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Represents a recommendation section in the comprehensive report
 * 
 * Each section contains contextual guidance based on specific quality issues found
 * in the data validation results.
 */
@Getter
@Builder
public class RecommendationSection {
    /**
     * Icon to display (emoji or icon class)
     */
    private final String icon;
    
    /**
     * CSS color class for styling (e.g., "red", "yellow", "green")
     */
    private final String colorClass;
    
    /**
     * Section title
     */
    private final String title;
    
    /**
     * Main content/description
     */
    private final String content;
    
    /**
     * List of bullet points with specific recommendations
     */
    private final List<String> bullets;
}
