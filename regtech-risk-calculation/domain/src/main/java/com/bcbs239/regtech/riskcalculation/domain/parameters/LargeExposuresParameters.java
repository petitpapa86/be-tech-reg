package com.bcbs239.regtech.riskcalculation.domain.parameters;

import com.bcbs239.regtech.riskcalculation.domain.shared.Money;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;

/**
 * Value Object: Large Exposures Parameters
 * 
 * Defines parameters for large exposures as per CRR Art. 395
 */
public record LargeExposuresParameters(
    double limitPercent,
    double classificationThresholdPercent,
    @NonNull Money eligibleCapital,
    @NonNull Money absoluteLimitValue,
    @NonNull Money absoluteClassificationValue,
    @NonNull String regulatoryReference
) {
    
    public LargeExposuresParameters {
        if (limitPercent <= 0 || limitPercent > 100) {
            throw new IllegalArgumentException("Limit percent must be between 0 and 100");
        }
        if (classificationThresholdPercent <= 0 || classificationThresholdPercent > 100) {
            throw new IllegalArgumentException("Classification threshold must be between 0 and 100");
        }
        if (eligibleCapital == null) {
            throw new IllegalArgumentException("Eligible capital cannot be null");
        }
        if (absoluteLimitValue == null) {
            throw new IllegalArgumentException("Absolute limit value cannot be null");
        }
        if (absoluteClassificationValue == null) {
            throw new IllegalArgumentException("Absolute classification value cannot be null");
        }
        if (regulatoryReference == null || regulatoryReference.isBlank()) {
            throw new IllegalArgumentException("Regulatory reference cannot be null or empty");
        }
    }
    
    @NonNull
    public static LargeExposuresParameters createDefault() {
        Money eligibleCapital = Money.of(new BigDecimal("2500000000"), "EUR");
        double limitPercent = 25.0;
        double classificationThresholdPercent = 10.0;
        
        Money absoluteLimitValue = Money.of(
            eligibleCapital.amount().multiply(new BigDecimal(limitPercent / 100.0)),
            eligibleCapital.currency()
        );
        
        Money absoluteClassificationValue = Money.of(
            eligibleCapital.amount().multiply(new BigDecimal(classificationThresholdPercent / 100.0)),
            eligibleCapital.currency()
        );
        
        return new LargeExposuresParameters(
            limitPercent,
            classificationThresholdPercent,
            eligibleCapital,
            absoluteLimitValue,
            absoluteClassificationValue,
            "CRR Art. 395"
        );
    }
    
    public boolean isValid() {
        return limitPercent > 0 && 
               classificationThresholdPercent > 0 &&
               eligibleCapital.amount().compareTo(BigDecimal.ZERO) > 0;
    }
}
