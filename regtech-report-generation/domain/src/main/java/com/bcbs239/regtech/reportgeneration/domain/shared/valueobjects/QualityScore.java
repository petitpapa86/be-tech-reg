package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import java.math.BigDecimal;

/**
 * Value object representing an overall quality score.
 * Encapsulates validation and formatting for a score typically expressed as a percentage (0-100).
 */
public record QualityScore(BigDecimal value) {

    public QualityScore {
        if (value == null) {
            throw new IllegalArgumentException("QualityScore cannot be null");
        }
        // Optional: enforce reasonable bounds 0..100
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("QualityScore must be between 0 and 100: " + value);
        }
    }

    public static QualityScore of(BigDecimal v) {
        return new QualityScore(v);
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
