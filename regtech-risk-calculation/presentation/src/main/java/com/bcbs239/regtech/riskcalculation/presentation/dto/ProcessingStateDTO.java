package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * DTO representing the current state of portfolio analysis processing.
 * Aligned with ProcessingState domain enum.
 */
public enum ProcessingStateDTO {
    PENDING("PENDING"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");
    
    private final String value;
    
    ProcessingStateDTO(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static ProcessingStateDTO fromValue(String value) {
        for (ProcessingStateDTO state : ProcessingStateDTO.values()) {
            if (state.value.equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown processing state: " + value);
    }
}
