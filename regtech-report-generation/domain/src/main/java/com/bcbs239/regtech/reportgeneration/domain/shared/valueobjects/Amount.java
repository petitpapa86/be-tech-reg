package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;
import java.math.BigDecimal;

public record Amount(@NonNull BigDecimal value) {
    public Amount {
        if (value == null) {
            throw new IllegalArgumentException("Amount value cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
    
    public static Amount of(BigDecimal value) {
        return new Amount(value);
    }
    
    public static Amount zero() {
        return new Amount(BigDecimal.ZERO);
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}
