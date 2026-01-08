package com.bcbs239.regtech.core.domain.quality;

/**
 * Quality thresholds for dimension scores and overall quality assessment.
 * Values loaded from color-rules-config-COMPLETE.yaml
 * 
 * BCBS 239 compliance thresholds:
 * - Excellent: >= 90% (Grade A)
 * - Good: 85-90% (Grade B)
 * - Acceptable: 75-85% (Grade C)
 * - Poor: 65-75% (Grade D)
 * - Critical: < 65% (Grade E/F)
 */
public record QualityThresholds(
    // Overall score thresholds
    double excellentThreshold,      // 90.0%
    double goodThreshold,            // 85.0%
    double acceptableThreshold,      // 75.0%
    double poorThreshold,            // 65.0%
    double criticalThreshold,        // < 65.0%
    
    // Dimension-specific thresholds (from YAML dimension_score_thresholds)
    double completenessExcellent,    // 90.0%
    double completenessAcceptable,   // 75.0%
    
    double accuracyExcellent,        // 90.0%
    double accuracyAcceptable,       // 75.0%
    
    double timelinessExcellent,      // 90.0%
    double timelinessAcceptable,     // 75.0%
    
    double consistencyExcellent,     // 90.0%
    double consistencyAcceptable,    // 75.0%
    
    double uniquenessExcellent,      // 90.0%
    double uniquenessAcceptable,     // 75.0%
    
    double validityExcellent,        // 90.0%
    double validityAcceptable        // 75.0%
) {
    
    /**
     * Default BCBS 239 thresholds from color-rules-config-COMPLETE.yaml
     */
    public static QualityThresholds bcbs239Defaults() {
        return new QualityThresholds(
            90.0, 85.0, 75.0, 65.0, 65.0,  // Overall thresholds
            90.0, 75.0,  // Completeness
            90.0, 75.0,  // Accuracy
            90.0, 75.0,  // Timeliness
            90.0, 75.0,  // Consistency
            90.0, 75.0,  // Uniqueness
            90.0, 75.0   // Validity
        );
    }
    
    /**
     * Check if overall score is excellent (>= 90%)
     */
    public boolean isExcellent(double score) {
        return score >= excellentThreshold;
    }
    
    /**
     * Check if overall score is good (85-90%)
     */
    public boolean isGood(double score) {
        return score >= goodThreshold && score < excellentThreshold;
    }
    
    /**
     * Check if overall score is acceptable (75-85%)
     */
    public boolean isAcceptable(double score) {
        return score >= acceptableThreshold && score < goodThreshold;
    }
    
    /**
     * Check if overall score is poor (65-75%)
     */
    public boolean isPoor(double score) {
        return score >= poorThreshold && score < acceptableThreshold;
    }
    
    /**
     * Check if overall score is critical (< 65%)
     */
    public boolean isCritical(double score) {
        return score < criticalThreshold;
    }
    
    /**
     * Get quality grade letter (A, B, C, D, E/F) for given score
     */
    public String getGradeLetter(double score) {
        if (isExcellent(score)) return "A";
        if (isGood(score)) return "B";
        if (isAcceptable(score)) return "C";
        if (isPoor(score)) return "D";
        return "F";
    }
}
