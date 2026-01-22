package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;
import java.math.BigDecimal;

public record Percentage(@NonNull BigDecimal value) {
    public Percentage {
        if (value == null) {
            throw new IllegalArgumentException("Percentage value cannot be null");
        }
        // Percentage can be negative? Probably not for capital percentage.
        // But let's assume it can't be null.
    }
    
    public static Percentage of(BigDecimal value) {
        return new Percentage(value);
    }
    
    public static Percentage of(double value) {
        return new Percentage(BigDecimal.valueOf(value));
    }
    
    public static Percentage zero() {
        return new Percentage(BigDecimal.ZERO);
    }
    
    public boolean isGreaterThan(Percentage other) {
        return value.compareTo(other.value) > 0;
    }
    
    public boolean isGreaterThan(BigDecimal other) {
        return value.compareTo(other) > 0;
    }
    
    public boolean isGreaterThanOrEqualTo(BigDecimal other) {
        return value.compareTo(other) >= 0;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
