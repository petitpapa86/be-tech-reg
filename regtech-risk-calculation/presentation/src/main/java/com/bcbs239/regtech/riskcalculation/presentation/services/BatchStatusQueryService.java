package com.bcbs239.regtech.riskcalculation.presentation.services;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.analysis.ProcessingState;
import com.bcbs239.regtech.riskcalculation.domain.persistence.BatchRepository;
import com.bcbs239.regtech.riskcalculation.domain.persistence.PortfolioAnalysisRepository;
import com.bcbs239.regtech.riskcalculation.presentation.dto.BatchStatusResponseDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ProcessingProgressDTO;
import com.bcbs239.regtech.riskcalculation.presentation.mappers.PortfolioAnalysisMapper;
import com.bcbs239.regtech.riskcalculation.presentation.mappers.StatusMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Query service for retrieving batch status and progress information.
 * Provides read-only access to batch processing state and progress.
 * 
 * This service uses ONLY database metadata for status queries (no file I/O).
 * Detailed exposure data is accessed through separate services that use JSON files.
 * 
 * Requirements: 2.1, 9.1
 */
@Service
@Transactional(readOnly = true)
public class BatchStatusQueryService {
    
    private final BatchRepository batchRepository;
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;
    private final StatusMapper statusMapper;
    private final PortfolioAnalysisMapper portfolioAnalysisMapper;
    
    public BatchStatusQueryService(
        BatchRepository batchRepository,
        PortfolioAnalysisRepository portfolioAnalysisRepository,
        StatusMapper statusMapper,
        PortfolioAnalysisMapper portfolioAnalysisMapper
    ) {
        this.batchRepository = batchRepository;
        this.portfolioAnalysisRepository = portfolioAnalysisRepository;
        this.statusMapper = statusMapper;
        this.portfolioAnalysisMapper = portfolioAnalysisMapper;
    }
    
    /**
     * Retrieves the status of a batch using only database metadata.
     * Does NOT access JSON files - uses only batch metadata and portfolio analysis tables.
     * 
     * Requirement 9.1: Status queries use database only (no file access)
     * 
     * @param batchId the batch identifier
     * @return Optional containing the batch status DTO if found
     */
    public Optional<BatchStatusResponseDTO> getBatchStatus(String batchId) {
        // Check if batch exists using database metadata only
        if (!batchRepository.exists(batchId)) {
            return Optional.empty();
        }
        
        // Get portfolio analysis if available (database query only)
        Maybe<PortfolioAnalysis> analysisMaybe = portfolioAnalysisRepository.findByBatchId(batchId);
        
        if (analysisMaybe.isEmpty()) {
            // Batch exists but analysis not started
            return Optional.of(statusMapper.toPendingBatchStatusDTO(batchId));
        }
        
        PortfolioAnalysis analysis = analysisMaybe.getValue();
        
        // Determine if results are available based on completion status
        // Results are available when calculation completes successfully
        boolean hasResults = analysis.getState() == ProcessingState.COMPLETED;
        
        String errorMessage = null;
        if (analysis.getState() == ProcessingState.FAILED) {
            errorMessage = "Risk calculation failed for batch " + batchId;
        }
        
        return Optional.of(statusMapper.toBatchStatusDTO(
            batchId,
            analysis,
            hasResults,
            errorMessage
        ));
    }
    
    /**
     * Retrieves detailed processing progress for a batch.
     * 
     * @param batchId the batch identifier
     * @return Optional containing the processing progress DTO if found
     */
    public Optional<ProcessingProgressDTO> getProcessingProgress(String batchId) {
        Maybe<ProcessingProgressDTO> result = portfolioAnalysisRepository.findByBatchId(batchId)
            .map(PortfolioAnalysis::getProgress)
            .map(portfolioAnalysisMapper::toProcessingProgressDTO);
        return result.isPresent() ? Optional.of(result.getValue()) : Optional.empty();
    }
    
    /**
     * Checks if a batch exists in the system using only database metadata.
     * 
     * @param batchId the batch identifier
     * @return true if the batch exists, false otherwise
     */
    public boolean batchExists(String batchId) {
        // Use batch repository for existence check - database only, no file I/O
        return batchRepository.exists(batchId);
    }
    
    /**
     * Checks if a batch calculation is complete.
     * 
     * @param batchId the batch identifier
     * @return true if the batch calculation is complete, false otherwise
     */
    public boolean isCalculationComplete(String batchId) {
        Maybe<Boolean> result = portfolioAnalysisRepository.findByBatchId(batchId)
            .map(analysis -> analysis.getState() == ProcessingState.COMPLETED);
        return result.orElse(false);
    }
    
    /**
     * Checks if a batch calculation has failed.
     * 
     * @param batchId the batch identifier
     * @return true if the batch calculation has failed, false otherwise
     */
    public boolean hasCalculationFailed(String batchId) {
        Maybe<Boolean> result = portfolioAnalysisRepository.findByBatchId(batchId)
            .map(analysis -> analysis.getState() == ProcessingState.FAILED);
        return result.orElse(false);
    }
    
    /**
     * Retrieves all active batches (in progress or pending) with optional bank filter.
     * 
     * @param bankIdFilter optional bank ID to filter results
     * @return list of batch status DTOs for active batches
     */
    public List<BatchStatusResponseDTO> getActiveBatches(Optional<String> bankIdFilter) {
        // For now, return empty list as we need to add repository methods to support this
        // This will be implemented when we add findActiveBatches to the repository
        return List.of();
    }
}
