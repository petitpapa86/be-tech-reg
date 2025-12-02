package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for temporal tracking of portfolio analysis processing.
 * Aligned with ProcessingTimestamps domain value object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingTimestampsDTO {
    
    @JsonProperty("started_at")
    private Instant startedAt;
    
    @JsonProperty("completed_at")
    private Instant completedAt;
    
    @JsonProperty("failed_at")
    private Instant failedAt;
    
    @JsonProperty("analyzed_at")
    private Instant analyzedAt;
}
