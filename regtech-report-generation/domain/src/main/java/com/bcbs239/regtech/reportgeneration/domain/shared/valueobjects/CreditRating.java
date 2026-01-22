package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

public record CreditRating(@NonNull String value) {
    public CreditRating {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Credit rating cannot be null or blank");
        }
    }
    
    public static CreditRating of(String value) {
        return new CreditRating(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
