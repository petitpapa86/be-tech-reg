package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

/**
 * Total number of exposures in a batch
 * Immutable value object that represents the count of exposure records
 */
public record TotalExposures(int count) {
    
    public TotalExposures {
        if (count < 0) {
            throw new IllegalArgumentException("Total exposures count cannot be negative");
        }
    }
    
    public static TotalExposures of(int count) {
        return new TotalExposures(count);
    }
    
    public static TotalExposures zero() {
        return new TotalExposures(0);
    }
    
    public boolean isEmpty() {
        return count == 0;
    }
    
    public boolean isGreaterThan(TotalExposures other) {
        return count > other.count;
    }
}