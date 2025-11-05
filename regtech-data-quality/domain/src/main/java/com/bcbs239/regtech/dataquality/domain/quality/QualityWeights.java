package com.bcbs239.regtech.dataquality.domain.quality;

/**
 * Value object representing the weights for each quality dimension in overall score calculation.
 * Weights must sum to 1.0 (100%) and all values must be non-negative.
 */
public record QualityWeights(
    double completeness,    // Default: 0.25 (25%)
    double accuracy,        // Default: 0.25 (25%)
    double consistency,     // Default: 0.20 (20%)
    double timeliness,      // Default: 0.15 (15%)
    double uniqueness,      // Default: 0.10 (10%)
    double validity         // Default: 0.05 (5%)
) {
    
    /**
     * Constructor with validation
     */
    public QualityWeights {
        if (completeness < 0 || accuracy < 0 || consistency < 0 || 
            timeliness < 0 || uniqueness < 0 || validity < 0) {
            throw new IllegalArgumentException("All quality weights must be non-negative");
        }
        
        double sum = completeness + accuracy + consistency + timeliness + uniqueness + validity;
        if (Math.abs(sum - 1.0) > 0.001) { // Allow for small floating point errors
            throw new IllegalArgumentException("Quality weights must sum to 1.0, but sum is: " + sum);
        }
    }
    
    /**
     * Creates default quality weights based on BCBS 239 recommendations
     */
    public static QualityWeights defaultWeights() {
        return new QualityWeights(0.25, 0.25, 0.20, 0.15, 0.10, 0.05);
    }
    
    /**
     * Creates equal weights for all dimensions (16.67% each, rounded)
     */
    public static QualityWeights equalWeights() {
        double weight = 1.0 / 6.0;
        return new QualityWeights(weight, weight, weight, weight, weight, weight);
    }
    
    /**
     * Gets the weight for a specific quality dimension
     */
    public double getWeight(QualityDimension dimension) {
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
     * Creates a new QualityWeights with a modified weight for a specific dimension.
     * Other weights are proportionally adjusted to maintain sum of 1.0.
     */
    public QualityWeights withWeight(QualityDimension dimension, double newWeight) {
        if (newWeight < 0 || newWeight > 1) {
            throw new IllegalArgumentException("Weight must be between 0 and 1");
        }
        
        double currentWeight = getWeight(dimension);
        double remainingWeight = 1.0 - newWeight;
        double currentRemainingWeight = 1.0 - currentWeight;
        
        if (currentRemainingWeight == 0) {
            throw new IllegalArgumentException("Cannot adjust weights when current weight is 1.0");
        }
        
        double adjustmentFactor = remainingWeight / currentRemainingWeight;
        
        return switch (dimension) {
            case COMPLETENESS -> new QualityWeights(
                newWeight,
                accuracy * adjustmentFactor,
                consistency * adjustmentFactor,
                timeliness * adjustmentFactor,
                uniqueness * adjustmentFactor,
                validity * adjustmentFactor
            );
            case ACCURACY -> new QualityWeights(
                completeness * adjustmentFactor,
                newWeight,
                consistency * adjustmentFactor,
                timeliness * adjustmentFactor,
                uniqueness * adjustmentFactor,
                validity * adjustmentFactor
            );
            case CONSISTENCY -> new QualityWeights(
                completeness * adjustmentFactor,
                accuracy * adjustmentFactor,
                newWeight,
                timeliness * adjustmentFactor,
                uniqueness * adjustmentFactor,
                validity * adjustmentFactor
            );
            case TIMELINESS -> new QualityWeights(
                completeness * adjustmentFactor,
                accuracy * adjustmentFactor,
                consistency * adjustmentFactor,
                newWeight,
                uniqueness * adjustmentFactor,
                validity * adjustmentFactor
            );
            case UNIQUENESS -> new QualityWeights(
                completeness * adjustmentFactor,
                accuracy * adjustmentFactor,
                consistency * adjustmentFactor,
                timeliness * adjustmentFactor,
                newWeight,
                validity * adjustmentFactor
            );
            case VALIDITY -> new QualityWeights(
                completeness * adjustmentFactor,
                accuracy * adjustmentFactor,
                consistency * adjustmentFactor,
                timeliness * adjustmentFactor,
                uniqueness * adjustmentFactor,
                newWeight
            );
        };
    }
}

