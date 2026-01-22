package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

public record CounterpartyName(@NonNull String value) {
    public CounterpartyName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Counterparty name cannot be null or blank");
        }
    }
    
    public static CounterpartyName of(String value) {
        return new CounterpartyName(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
