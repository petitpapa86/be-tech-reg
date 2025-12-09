package com.bcbs239.regtech.dataquality.application.scoring;

import com.bcbs239.regtech.dataquality.domain.quality.QualityGrade;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;

/**
 * DTO for quality scores with all six dimensions.
 * Provides a serializable representation of quality scores for API responses.
 */
public record QualityScoresDto(
    double completenessScore,
    double accuracyScore,
    double consistencyScore,
    double timelinessScore,
    double uniquenessScore,
    double validityScore,
    double overallScore,
    String grade,
    boolean isCompliant,
    boolean requiresAttention
) {
    
    /**
     * Creates a DTO from domain QualityScores.
     */
    public static QualityScoresDto fromDomain(QualityScores scores) {
        if (scores == null) {
            return null;
        }
        
        return new QualityScoresDto(
            scores.completenessScore(),
            scores.accuracyScore(),
            scores.consistencyScore(),
            scores.timelinessScore(),
            scores.uniquenessScore(),
            scores.validityScore(),
            scores.overallScore(),
            scores.grade().name(),
            scores.isCompliant(),
            scores.requiresAttention()
        );
    }
    
    /**
     * Converts DTO back to domain object.
     */
    public QualityScores toDomain() {
        return new QualityScores(
            completenessScore,
            accuracyScore,
            consistencyScore,
            timelinessScore,
            uniquenessScore,
            validityScore,
            overallScore,
            QualityGrade.valueOf(grade)
        );
    }
    
    /**
     * Creates an empty scores DTO (all zeros).
     */
    public static QualityScoresDto empty() {
        return new QualityScoresDto(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            QualityGrade.POOR.name(),
            false,
            true
        );
    }
    
    /**
     * Gets the grade as an enum.
     */
    public QualityGrade getGradeEnum() {
        return QualityGrade.valueOf(grade);
    }
    
    /**
     * Gets a human-readable grade display.
     */
    public String getGradeDisplay() {
        return QualityGrade.valueOf(grade).getLetterGrade();
    }
    
    /**
     * Gets the compliance status as a string.
     */
    public String getComplianceStatus() {
        return isCompliant ? "COMPLIANT" : "NON_COMPLIANT";
    }
    
    /**
     * Gets the attention level as a string.
     */
    public String getAttentionLevel() {
        if (requiresAttention) {
            return "IMMEDIATE";
        } else if (!isCompliant) {
            return "MODERATE";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Gets dimension scores as a map for easy access.
     */
    public java.util.Map<String, Double> getDimensionScores() {
        return java.util.Map.of(
            "completeness", completenessScore,
            "accuracy", accuracyScore,
            "consistency", consistencyScore,
            "timeliness", timelinessScore,
            "uniqueness", uniquenessScore,
            "validity", validityScore
        );
    }
    
    /**
     * Gets the lowest scoring dimension.
     */
    public String getLowestScoringDimension() {
        java.util.Map<String, Double> scores = getDimensionScores();
        return scores.entrySet().stream()
            .min(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse("unknown");
    }
    
    /**
     * Gets the highest scoring dimension.
     */
    public String getHighestScoringDimension() {
        java.util.Map<String, Double> scores = getDimensionScores();
        return scores.entrySet().stream()
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse("unknown");
    }
}

