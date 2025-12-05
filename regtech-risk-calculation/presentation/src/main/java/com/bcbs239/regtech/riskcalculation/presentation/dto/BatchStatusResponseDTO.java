package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the status of a batch calculation.
 * Provides information about processing state and available results.
 * Aligned with bounded context architecture.
 * 
 * Requirements: 1.1, 1.3, 1.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStatusResponseDTO {
    
    @NotBlank(message = "Batch ID is required")
    @JsonProperty("batch_id")
    private String batchId;
    
    @NotNull(message = "Processing state is required")
    @JsonProperty("processing_state")
    private ProcessingStateDTO processingState;
    
    @JsonProperty("processing_progress")
    private ProcessingProgressDTO processingProgress;
    
    @JsonProperty("processing_timestamps")
    private ProcessingTimestampsDTO processingTimestamps;
    
    @JsonProperty("has_portfolio_analysis")
    private boolean hasPortfolioAnalysis;
    
    @JsonProperty("has_classified_exposures")
    private boolean hasClassifiedExposures;
    
    @JsonProperty("has_protected_exposures")
    private boolean hasProtectedExposures;
    
    @JsonProperty("total_exposures")
    private Integer totalExposures;
    
    @JsonProperty("error_message")
    private String errorMessage;
}
