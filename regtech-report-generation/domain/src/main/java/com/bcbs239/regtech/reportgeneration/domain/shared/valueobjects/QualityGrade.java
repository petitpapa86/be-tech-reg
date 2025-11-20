package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import java.math.BigDecimal;

/**
 * Quality Grade enumeration (A-F)
 * Represents the overall quality grade based on quality score
 * Provides intuitive letter-based grading system
 */
public enum QualityGrade {
    /**
     * Excellent quality (90-100%)
     */
    A,
    
    /**
     * Good quality (80-89%)
     */
    B,
    
    /**
     * Satisfactory quality (70-79%)
     */
    C,
    
    /**
     * Poor quality (60-69%)
     */
    D,
    
    /**
     * Very poor quality (50-59%)
     */
    E,
    
    /**
     * Failing quality (< 50%)
     */
    F;
    
    /**
     * Determine quality grade from overall quality score
     * 
     * @param score Overall quality score as percentage (0-100)
     * @return Corresponding quality grade
     */
    public static QualityGrade fromScore(BigDecimal score) {
        if (score == null) {
            throw new IllegalArgumentException("Quality score cannot be null");
        }
        
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Quality score must be between 0 and 100, got: " + score);
        }
        
        if (score.compareTo(new BigDecimal("90")) >= 0) {
            return A;
        } else if (score.compareTo(new BigDecimal("80")) >= 0) {
            return B;
        } else if (score.compareTo(new BigDecimal("70")) >= 0) {
            return C;
        } else if (score.compareTo(new BigDecimal("60")) >= 0) {
            return D;
        } else if (score.compareTo(new BigDecimal("50")) >= 0) {
            return E;
        } else {
            return F;
        }
    }
    
    /**
     * Check if this grade is passing (C or better)
     */
    public boolean isPassing() {
        return this == A || this == B || this == C;
    }
    
    /**
     * Check if this grade requires immediate attention (D or worse)
     */
    public boolean requiresAttention() {
        return this == D || this == E || this == F;
    }
    
    /**
     * Check if this grade is excellent (A)
     */
    public boolean isExcellent() {
        return this == A;
    }
    
    /**
     * Check if this grade is failing (F)
     */
    public boolean isFailing() {
        return this == F;
    }
    
    /**
     * Get color code for visual representation
     * Returns CSS color class name
     */
    public String getColorClass() {
        return switch (this) {
            case A -> "green";
            case B -> "blue";
            case C -> "yellow";
            case D -> "orange";
            case E -> "red";
            case F -> "dark-red";
        };
    }
    
    /**
     * Get human-readable description
     */
    public String getDescription() {
        return switch (this) {
            case A -> "Eccellente";
            case B -> "Buono";
            case C -> "Soddisfacente";
            case D -> "Scarso";
            case E -> "Molto Scarso";
            case F -> "Insufficiente";
        };
    }
    
    /**
     * Get score range for this grade
     */
    public String getScoreRange() {
        return switch (this) {
            case A -> "90-100%";
            case B -> "80-89%";
            case C -> "70-79%";
            case D -> "60-69%";
            case E -> "50-59%";
            case F -> "0-49%";
        };
    }
}
