package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

public record BankName(@NonNull String value) {
    public BankName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Bank name cannot be null or blank");
        }
    }
    
    public static BankName of(String value) {
        return new BankName(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
