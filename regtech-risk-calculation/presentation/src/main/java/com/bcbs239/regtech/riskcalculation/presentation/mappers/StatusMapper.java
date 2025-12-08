package com.bcbs239.regtech.riskcalculation.presentation.mappers;

import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.analysis.ProcessingState;
import com.bcbs239.regtech.riskcalculation.presentation.dto.BatchStatusResponseDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ProcessingStateDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ProcessingTimestampsDTO;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting batch status domain objects to DTOs.
 * Stateless component that provides null-safe conversion logic.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
@Component
public class StatusMapper {
    
    private final PortfolioAnalysisMapper portfolioAnalysisMapper;
    
    public StatusMapper(PortfolioAnalysisMapper portfolioAnalysisMapper) {
        this.portfolioAnalysisMapper = portfolioAnalysisMapper;
    }
    
    /**
     * Converts a PortfolioAnalysis to BatchStatusResponseDTO using only database metadata.
     * 
     * @param batchId the batch identifier
     * @param portfolioAnalysis the portfolio analysis (may be null if not yet created)
     * @param hasResults whether calculation results are available (based on completion status)
     * @param errorMessage error message if processing failed
     * @return the batch status DTO
     * @throws MappingException if batchId is null
     */
    public BatchStatusResponseDTO toBatchStatusDTO(
        String batchId,
        PortfolioAnalysis portfolioAnalysis,
        boolean hasResults,
        String errorMessage
    ) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new MappingException("Batch ID cannot be null or empty");
        }
        
        try {
            ProcessingStateDTO stateDTO;
            ProcessingTimestampsDTO timestampsDTO = null;
            
            if (portfolioAnalysis == null) {
                // No analysis exists yet - batch is pending
                stateDTO = ProcessingStateDTO.PENDING;
            } else {
                stateDTO = portfolioAnalysisMapper.toProcessingStateDTO(portfolioAnalysis.getState());
                timestampsDTO = portfolioAnalysisMapper.toProcessingTimestampsDTO(portfolioAnalysis);
            }
            
            return BatchStatusResponseDTO.builder()
                .batchId(batchId)
                .processingState(stateDTO)
                .processingProgress(portfolioAnalysis != null ? 
                    portfolioAnalysisMapper.toProcessingProgressDTO(portfolioAnalysis.getProgress()) : null)
                .processingTimestamps(timestampsDTO)
                .hasPortfolioAnalysis(portfolioAnalysis != null && 
                    portfolioAnalysis.getState() == ProcessingState.COMPLETED)
                .hasClassifiedExposures(hasResults)  // Results available when calculation completes
                .hasProtectedExposures(hasResults)   // Results available when calculation completes
                .totalExposures(portfolioAnalysis != null && portfolioAnalysis.getProgress() != null ? 
                    portfolioAnalysis.getProgress().totalExposures() : null)
                .errorMessage(errorMessage)
                .build();
        } catch (Exception e) {
            throw new MappingException("Failed to map batch status to DTO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts a PortfolioAnalysis to BatchStatusResponseDTO using only database metadata.
     * Deprecated method signature for backward compatibility.
     * 
     * @param batchId the batch identifier
     * @param portfolioAnalysis the portfolio analysis (may be null if not yet created)
     * @param hasClassifiedExposures whether classified exposures exist
     * @param hasProtectedExposures whether protected exposures exist
     * @param totalExposures total number of exposures
     * @param errorMessage error message if processing failed
     * @return the batch status DTO
     * @throws MappingException if batchId is null
     * @deprecated Use {@link #toBatchStatusDTO(String, PortfolioAnalysis, boolean, String)} instead
     */
    @Deprecated
    public BatchStatusResponseDTO toBatchStatusDTO(
        String batchId,
        PortfolioAnalysis portfolioAnalysis,
        boolean hasClassifiedExposures,
        boolean hasProtectedExposures,
        Integer totalExposures,
        String errorMessage
    ) {
        // Delegate to new method - assume results available if exposures exist
        boolean hasResults = hasClassifiedExposures || hasProtectedExposures;
        return toBatchStatusDTO(batchId, portfolioAnalysis, hasResults, errorMessage);
    }
    
    /**
     * Converts a PortfolioAnalysis to BatchStatusResponseDTO with minimal information.
     * 
     * @param batchId the batch identifier
     * @param portfolioAnalysis the portfolio analysis
     * @return the batch status DTO
     * @throws MappingException if batchId is null
     */
    public BatchStatusResponseDTO toBatchStatusDTO(String batchId, PortfolioAnalysis portfolioAnalysis) {
        return toBatchStatusDTO(batchId, portfolioAnalysis, false, false, null, null);
    }
    
    /**
     * Creates a BatchStatusResponseDTO for a pending batch.
     * 
     * @param batchId the batch identifier
     * @return the batch status DTO with PENDING state
     * @throws MappingException if batchId is null
     */
    public BatchStatusResponseDTO toPendingBatchStatusDTO(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new MappingException("Batch ID cannot be null or empty");
        }
        
        return BatchStatusResponseDTO.builder()
            .batchId(batchId)
            .processingState(ProcessingStateDTO.PENDING)
            .processingProgress(null)
            .processingTimestamps(null)
            .hasPortfolioAnalysis(false)
            .hasClassifiedExposures(false)
            .hasProtectedExposures(false)
            .totalExposures(null)
            .errorMessage(null)
            .build();
    }
    
    /**
     * Creates a BatchStatusResponseDTO for a failed batch.
     * 
     * @param batchId the batch identifier
     * @param errorMessage the error message
     * @return the batch status DTO with FAILED state
     * @throws MappingException if batchId is null
     */
    public BatchStatusResponseDTO toFailedBatchStatusDTO(String batchId, String errorMessage) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new MappingException("Batch ID cannot be null or empty");
        }
        
        return BatchStatusResponseDTO.builder()
            .batchId(batchId)
            .processingState(ProcessingStateDTO.FAILED)
            .processingProgress(null)
            .processingTimestamps(null)
            .hasPortfolioAnalysis(false)
            .hasClassifiedExposures(false)
            .hasProtectedExposures(false)
            .totalExposures(null)
            .errorMessage(errorMessage)
            .build();
    }
}
