package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for detailed progress information during portfolio analysis processing.
 * Aligned with ProcessingProgress domain value object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingProgressDTO {
    
    @JsonProperty("total_exposures")
    private int totalExposures;
    
    @JsonProperty("processed_exposures")
    private int processedExposures;
    
    @JsonProperty("percentage_complete")
    private double percentageComplete;
    
    @JsonProperty("processing_rate")
    private double processingRate;
    
    @JsonProperty("estimated_time_remaining_seconds")
    private Long estimatedTimeRemainingSeconds;
    
    @JsonProperty("started_at")
    private Instant startedAt;
    
    @JsonProperty("last_update_at")
    private Instant lastUpdateAt;
}
