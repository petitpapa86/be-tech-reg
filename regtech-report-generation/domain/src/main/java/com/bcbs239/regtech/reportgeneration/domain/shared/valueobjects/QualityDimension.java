package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import java.math.BigDecimal;

/**
 * Data Quality Dimension enumeration
 * Represents the six dimensions of data quality assessment
 * Each dimension has specific thresholds for determining critical issues
 */
public enum QualityDimension {
    /**
     * Completeness - All required data fields are populated
     * Critical threshold: < 70%
     */
    COMPLETENESS(new BigDecimal("70")),
    
    /**
     * Accuracy - Data values are correct and reflect reality
     * Critical threshold: < 70%
     */
    ACCURACY(new BigDecimal("70")),
    
    /**
     * Consistency - Data is consistent across systems and over time
     * Critical threshold: < 80%
     */
    CONSISTENCY(new BigDecimal("80")),
    
    /**
     * Timeliness - Data is available when needed and up-to-date
     * Critical threshold: < 90%
     */
    TIMELINESS(new BigDecimal("90")),
    
    /**
     * Uniqueness - No duplicate records exist
     * Critical threshold: < 95%
     */
    UNIQUENESS(new BigDecimal("95")),
    
    /**
     * Validity - Data conforms to defined formats and business rules
     * Critical threshold: < 90%
     */
    VALIDITY(new BigDecimal("90"));
    
    private final BigDecimal criticalThreshold;
    
    /**
     * Constructor with critical threshold
     */
    QualityDimension(BigDecimal criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }
    
    /**
     * Get the critical threshold for this dimension
     * Scores below this threshold indicate critical issues requiring immediate attention
     */
    public BigDecimal getCriticalThreshold() {
        return criticalThreshold;
    }
    
    /**
     * Check if a score is below the critical threshold for this dimension
     * 
     * @param score Quality score as percentage (0-100)
     * @return true if score is below critical threshold
     */
    public boolean isCritical(BigDecimal score) {
        if (score == null) {
            throw new IllegalArgumentException("Score cannot be null");
        }
        return score.compareTo(criticalThreshold) < 0;
    }
    
    /**
     * Check if a score meets or exceeds the critical threshold
     * 
     * @param score Quality score as percentage (0-100)
     * @return true if score meets or exceeds threshold
     */
    public boolean meetsThreshold(BigDecimal score) {
        if (score == null) {
            throw new IllegalArgumentException("Score cannot be null");
        }
        return score.compareTo(criticalThreshold) >= 0;
    }
    
    /**
     * Check if a score indicates excellent quality (>= 95%)
     * 
     * @param score Quality score as percentage (0-100)
     * @return true if score is 95% or higher
     */
    public boolean isExcellent(BigDecimal score) {
        if (score == null) {
            throw new IllegalArgumentException("Score cannot be null");
        }
        return score.compareTo(new BigDecimal("95")) >= 0;
    }
    
    /**
     * Get human-readable name for this dimension
     */
    public String getDisplayName() {
        return switch (this) {
            case COMPLETENESS -> "Completezza";
            case ACCURACY -> "Accuratezza";
            case CONSISTENCY -> "Coerenza";
            case TIMELINESS -> "Tempestività";
            case UNIQUENESS -> "Unicità";
            case VALIDITY -> "Validità";
        };
    }
    
    /**
     * Get description of what this dimension measures
     */
    public String getDescription() {
        return switch (this) {
            case COMPLETENESS -> "Tutti i campi obbligatori sono popolati";
            case ACCURACY -> "I valori dei dati sono corretti e riflettono la realtà";
            case CONSISTENCY -> "I dati sono coerenti tra sistemi e nel tempo";
            case TIMELINESS -> "I dati sono disponibili quando necessario e aggiornati";
            case UNIQUENESS -> "Non esistono record duplicati";
            case VALIDITY -> "I dati sono conformi ai formati e alle regole di business definiti";
        };
    }
}
