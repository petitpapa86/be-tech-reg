package com.bcbs239.regtech.dataquality.domain.quality;

/**
 * Value object representing individual quality scores for each dimension.
 * All scores are percentages (0-100).
 */
public record DimensionScores(
    double completeness,
    double accuracy,
    double consistency,
    double timeliness,
    double uniqueness,
    double validity
) {
    
    /**
     * Constructor with validation
     */
    public DimensionScores {
        validateScore("completeness", completeness);
        validateScore("accuracy", accuracy);
        validateScore("consistency", consistency);
        validateScore("timeliness", timeliness);
        validateScore("uniqueness", uniqueness);
        validateScore("validity", validity);
    }
    
    private static void validateScore(String dimensionName, double score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException(
                dimensionName + " score must be between 0 and 100, but was: " + score);
        }
    }
    
    /**
     * Creates dimension scores with all dimensions set to zero
     */
    public static DimensionScores empty() {
        return new DimensionScores(0, 0, 0, 0, 0, 0);
    }
    
    /**
     * Creates dimension scores with all dimensions set to perfect (100)
     */
    public static DimensionScores perfect() {
        return new DimensionScores(100, 100, 100, 100, 100, 100);
    }
    
    /**
     * Gets the score for a specific quality dimension
     */
    public double getScore(QualityDimension dimension) {
        return switch (dimension) {
            case COMPLETENESS -> completeness;
            case ACCURACY -> accuracy;
            case CONSISTENCY -> consistency;
            case TIMELINESS -> timeliness;
            case UNIQUENESS -> uniqueness;
            case VALIDITY -> validity;
        };
    }
    
    /**
     * Creates a new DimensionScores with a modified score for a specific dimension
     */
    public DimensionScores withScore(QualityDimension dimension, double newScore) {
        validateScore(dimension.name().toLowerCase(), newScore);
        
        return switch (dimension) {
            case COMPLETENESS -> new DimensionScores(newScore, accuracy, consistency, timeliness, uniqueness, validity);
            case ACCURACY -> new DimensionScores(completeness, newScore, consistency, timeliness, uniqueness, validity);
            case CONSISTENCY -> new DimensionScores(completeness, accuracy, newScore, timeliness, uniqueness, validity);
            case TIMELINESS -> new DimensionScores(completeness, accuracy, consistency, newScore, uniqueness, validity);
            case UNIQUENESS -> new DimensionScores(completeness, accuracy, consistency, timeliness, newScore, validity);
            case VALIDITY -> new DimensionScores(completeness, accuracy, consistency, timeliness, uniqueness, newScore);
        };
    }
    
    /**
     * Calculates the weighted overall score using the provided weights
     */
    public double calculateOverallScore(QualityWeights weights) {
        return (completeness * weights.completeness()) +
               (accuracy * weights.accuracy()) +
               (consistency * weights.consistency()) +
               (timeliness * weights.timeliness()) +
               (uniqueness * weights.uniqueness()) +
               (validity * weights.validity());
    }
    
    /**
     * Gets the minimum score across all dimensions
     */
    public double getMinimumScore() {
        return Math.min(completeness, 
               Math.min(accuracy, 
               Math.min(consistency, 
               Math.min(timeliness, 
               Math.min(uniqueness, validity)))));
    }
    
    /**
     * Gets the maximum score across all dimensions
     */
    public double getMaximumScore() {
        return Math.max(completeness, 
               Math.max(accuracy, 
               Math.max(consistency, 
               Math.max(timeliness, 
               Math.max(uniqueness, validity)))));
    }
    
    /**
     * Gets the average score across all dimensions (unweighted)
     */
    public double getAverageScore() {
        return (completeness + accuracy + consistency + timeliness + uniqueness + validity) / 6.0;
    }
}