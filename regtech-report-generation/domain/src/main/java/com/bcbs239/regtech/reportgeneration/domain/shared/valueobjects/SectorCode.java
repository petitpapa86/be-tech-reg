package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

public record SectorCode(@NonNull String value) {
    public SectorCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Sector code cannot be null or blank");
        }
    }
    
    public static SectorCode of(String value) {
        return new SectorCode(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
