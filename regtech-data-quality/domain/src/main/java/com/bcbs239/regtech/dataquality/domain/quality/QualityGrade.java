package com.bcbs239.regtech.dataquality.domain.quality;

import java.util.Arrays;

/**
 * Enumeration representing quality grades based on overall quality scores.
 * Grades are assigned based on configurable thresholds.
 */
public enum QualityGrade {
    
    /**
     * Excellent quality (A+) - Score >= 95%
     */
    EXCELLENT(95.0, "A+", "Excellent"),
    
    /**
     * Very good quality (A) - Score >= 90% and < 95%
     */
    VERY_GOOD(90.0, "A", "Very Good"),
    
    /**
     * Good quality (B) - Score >= 80% and < 90%
     */
    GOOD(80.0, "B", "Good"),
    
    /**
     * Acceptable quality (C) - Score >= 70% and < 80%
     */
    ACCEPTABLE(70.0, "C", "Acceptable"),
    
    /**
     * Poor quality (F) - Score < 70%
     */
    POOR(0.0, "F", "Poor");

    private final double threshold;
    private final String letterGrade;
    private final String description;

    QualityGrade(double threshold, String letterGrade, String description) {
        this.threshold = threshold;
        this.letterGrade = letterGrade;
        this.description = description;
    }

    /**
     * Gets the minimum score threshold for this grade
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * Gets the letter grade representation (A+, A, B, C, F)
     */
    public String getLetterGrade() {
        return letterGrade;
    }

    /**
     * Gets the descriptive name of this grade
     */
    public String getDescription() {
        return description;
    }

    /**
     * Determines the quality grade based on a score (0-100)
     *
     * @param score The quality score as a percentage (0-100)
     * @return The corresponding quality grade
     */
    public static QualityGrade fromScore(double score) {
        return Arrays.stream(values())
            .filter(grade -> score >= grade.threshold)
            .findFirst()
            .orElse(POOR);
    }

    /**
     * Checks if this grade indicates compliance (B or better)
     */
    public boolean isCompliant() {
        return this.ordinal() <= GOOD.ordinal();
    }

    /**
     * Checks if this grade requires immediate attention (C or worse)
     */
    public boolean requiresAttention() {
        return this.ordinal() >= ACCEPTABLE.ordinal();
    }
}

