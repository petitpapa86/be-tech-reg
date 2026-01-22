package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

public record CountryCode(@NonNull String value) {
    public CountryCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Country code cannot be null or blank");
        }
    }
    
    public static CountryCode of(String value) {
        return new CountryCode(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
