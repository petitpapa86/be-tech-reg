package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

public record LeiCode(@NonNull String value) {
    public LeiCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LEI code cannot be null or blank");
        }
    }
    
    public static LeiCode of(String value) {
        return new LeiCode(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
