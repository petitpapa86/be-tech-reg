package com.bcbs239.regtech.dataquality.domain.quality;

/**
 * Value object representing complete quality assessment results including
 * individual dimension scores, overall weighted score, and quality grade.
 */
public record QualityScores(
    double completenessScore,
    double accuracyScore,
    double consistencyScore,
    double timelinessScore,
    double uniquenessScore,
    double validityScore,
    double overallScore,
    QualityGrade grade
) {
    
    /**
     * Constructor with validation
     */
    public QualityScores {
        validateScore("completeness", completenessScore);
        validateScore("accuracy", accuracyScore);
        validateScore("consistency", consistencyScore);
        validateScore("timeliness", timelinessScore);
        validateScore("uniqueness", uniquenessScore);
        validateScore("validity", validityScore);
        validateScore("overall", overallScore);
        
        if (grade == null) {
            throw new IllegalArgumentException("Quality grade cannot be null");
        }
    }
    
    private static void validateScore(String scoreName, double score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException(
                scoreName + " score must be between 0 and 100, but was: " + score);
        }
    }
    
    /**
     * Creates quality scores with all dimensions set to zero
     */
    public static QualityScores empty() {
        return new QualityScores(0, 0, 0, 0, 0, 0, 0, QualityGrade.POOR);
    }
    
    /**
     * Creates quality scores with all dimensions set to perfect (100)
     */
    public static QualityScores perfect() {
        return new QualityScores(100, 100, 100, 100, 100, 100, 100, QualityGrade.EXCELLENT);
    }
    
    /**
     * Calculates quality scores from dimension scores and weights
     */
    public static QualityScores calculate(DimensionScores dimensions, QualityWeights weights) {
        double overall = dimensions.calculateOverallScore(weights);
        QualityGrade grade = QualityGrade.fromScore(overall);
        
        return new QualityScores(
            dimensions.completeness(),
            dimensions.accuracy(),
            dimensions.consistency(),
            dimensions.timeliness(),
            dimensions.uniqueness(),
            dimensions.validity(),
            overall,
            grade
        );
    }
    
    /**
     * Gets the dimension scores as a separate value object
     */
    public DimensionScores getDimensionScores() {
        return new DimensionScores(
            completenessScore,
            accuracyScore,
            consistencyScore,
            timelinessScore,
            uniquenessScore,
            validityScore
        );
    }
    
    /**
     * Gets the score for a specific quality dimension
     */
    public double getScore(QualityDimension dimension) {
        return switch (dimension) {
            case COMPLETENESS -> completenessScore;
            case ACCURACY -> accuracyScore;
            case CONSISTENCY -> consistencyScore;
            case TIMELINESS -> timelinessScore;
            case UNIQUENESS -> uniquenessScore;
            case VALIDITY -> validityScore;
        };
    }
    
    /**
     * Checks if the overall quality meets compliance standards (grade B or better)
     */
    public boolean isCompliant() {
        return grade.isCompliant();
    }
    
    /**
     * Checks if the quality requires immediate attention (grade C or worse)
     */
    public boolean requiresAttention() {
        return grade.requiresAttention();
    }
    
    /**
     * Gets the lowest scoring dimension
     */
    public QualityDimension getLowestScoringDimension() {
        double minScore = getDimensionScores().getMinimumScore();
        
        for (QualityDimension dimension : QualityDimension.values()) {
            if (Math.abs(getScore(dimension) - minScore) < 0.001) {
                return dimension;
            }
        }
        
        return QualityDimension.COMPLETENESS; // Fallback, should not happen
    }
    
    /**
     * Gets the highest scoring dimension
     */
    public QualityDimension getHighestScoringDimension() {
        double maxScore = getDimensionScores().getMaximumScore();
        
        for (QualityDimension dimension : QualityDimension.values()) {
            if (Math.abs(getScore(dimension) - maxScore) < 0.001) {
                return dimension;
            }
        }
        
        return QualityDimension.COMPLETENESS; // Fallback, should not happen
    }
}