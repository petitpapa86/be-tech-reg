package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

public record CurrencyCode(@NonNull String value) {
    public CurrencyCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Currency code cannot be null or blank");
        }
    }
    
    public static CurrencyCode of(String value) {
        return new CurrencyCode(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
