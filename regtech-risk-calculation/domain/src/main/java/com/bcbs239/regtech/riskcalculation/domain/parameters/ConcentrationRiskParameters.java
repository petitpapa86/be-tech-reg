package com.bcbs239.regtech.riskcalculation.domain.parameters;

import org.jspecify.annotations.NonNull;

/**
 * Value Object: Concentration Risk Parameters
 * 
 * Defines thresholds for concentration risk monitoring
 */
public record ConcentrationRiskParameters(
    double alertThresholdPercent,
    double attentionThresholdPercent,
    int maxLargeExposures
) {
    
    public ConcentrationRiskParameters {
        if (alertThresholdPercent <= 0 || alertThresholdPercent > 100) {
            throw new IllegalArgumentException("Alert threshold must be between 0 and 100");
        }
        if (attentionThresholdPercent <= 0 || attentionThresholdPercent > 100) {
            throw new IllegalArgumentException("Attention threshold must be between 0 and 100");
        }
        if (alertThresholdPercent >= attentionThresholdPercent) {
            throw new IllegalArgumentException(
                "Alert threshold must be less than attention threshold"
            );
        }
        if (maxLargeExposures <= 0) {
            throw new IllegalArgumentException("Max large exposures must be positive");
        }
    }
    
    @NonNull
    public static ConcentrationRiskParameters createDefault() {
        return new ConcentrationRiskParameters(
            15.0,  // Alert at 15%
            20.0,  // Attention at 20%
            50     // Max 50 large exposures
        );
    }
    
    public boolean isValid() {
        return alertThresholdPercent > 0 && 
               attentionThresholdPercent > alertThresholdPercent &&
               maxLargeExposures > 0;
    }
}
