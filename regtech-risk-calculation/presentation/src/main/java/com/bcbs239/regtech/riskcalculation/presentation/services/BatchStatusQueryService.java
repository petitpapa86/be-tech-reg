package com.bcbs239.regtech.riskcalculation.presentation.services;

import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.analysis.ProcessingState;
import com.bcbs239.regtech.riskcalculation.domain.persistence.ExposureRepository;
import com.bcbs239.regtech.riskcalculation.domain.persistence.MitigationRepository;
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
 * Requirements: 2.1, 2.3, 2.4
 */
@Service
@Transactional(readOnly = true)
public class BatchStatusQueryService {
    
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;
    private final ExposureRepository exposureRepository;
    private final MitigationRepository mitigationRepository;
    private final StatusMapper statusMapper;
    private final PortfolioAnalysisMapper portfolioAnalysisMapper;
    
    public BatchStatusQueryService(
        PortfolioAnalysisRepository portfolioAnalysisRepository,
        ExposureRepository exposureRepository,
        MitigationRepository mitigationRepository,
        StatusMapper statusMapper,
        PortfolioAnalysisMapper portfolioAnalysisMapper
    ) {
        this.portfolioAnalysisRepository = portfolioAnalysisRepository;
        this.exposureRepository = exposureRepository;
        this.mitigationRepository = mitigationRepository;
        this.statusMapper = statusMapper;
        this.portfolioAnalysisMapper = portfolioAnalysisMapper;
    }
    
    /**
     * Retrieves the status of a batch including available results.
     * 
     * @param batchId the batch identifier
     * @return Optional containing the batch status DTO if found
     */
    public Optional<BatchStatusResponseDTO> getBatchStatus(String batchId) {
        Optional<PortfolioAnalysis> analysisOpt = portfolioAnalysisRepository.findByBatchId(batchId);
        
        if (analysisOpt.isEmpty()) {
            // No analysis exists yet - check if exposures exist
            int exposureCount = exposureRepository.findByBatchId(batchId).size();
            if (exposureCount == 0) {
                // Batch doesn't exist
                return Optional.empty();
            }
            
            // Batch exists but analysis not started
            return Optional.of(statusMapper.toPendingBatchStatusDTO(batchId));
        }
        
        PortfolioAnalysis analysis = analysisOpt.get();
        
        // Check for available results
        boolean hasClassifiedExposures = !exposureRepository.findByBatchId(batchId).isEmpty();
        boolean hasProtectedExposures = !mitigationRepository.findByBatchId(batchId).isEmpty();
        int totalExposures = exposureRepository.findByBatchId(batchId).size();
        
        String errorMessage = null;
        if (analysis.getState() == ProcessingState.FAILED) {
            errorMessage = "Risk calculation failed for batch " + batchId;
        }
        
        return Optional.of(statusMapper.toBatchStatusDTO(
            batchId,
            analysis,
            hasClassifiedExposures,
            hasProtectedExposures,
            totalExposures,
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
        return portfolioAnalysisRepository.findByBatchId(batchId)
            .map(PortfolioAnalysis::getProgress)
            .map(portfolioAnalysisMapper::toProcessingProgressDTO);
    }
    
    /**
     * Checks if a batch exists in the system.
     * 
     * @param batchId the batch identifier
     * @return true if the batch exists, false otherwise
     */
    public boolean batchExists(String batchId) {
        // A batch exists if it has either a portfolio analysis or exposures
        return portfolioAnalysisRepository.findByBatchId(batchId).isPresent() ||
               !exposureRepository.findByBatchId(batchId).isEmpty();
    }
    
    /**
     * Checks if a batch calculation is complete.
     * 
     * @param batchId the batch identifier
     * @return true if the batch calculation is complete, false otherwise
     */
    public boolean isCalculationComplete(String batchId) {
        return portfolioAnalysisRepository.findByBatchId(batchId)
            .map(analysis -> analysis.getState() == ProcessingState.COMPLETED)
            .orElse(false);
    }
    
    /**
     * Checks if a batch calculation has failed.
     * 
     * @param batchId the batch identifier
     * @return true if the batch calculation has failed, false otherwise
     */
    public boolean hasCalculationFailed(String batchId) {
        return portfolioAnalysisRepository.findByBatchId(batchId)
            .map(analysis -> analysis.getState() == ProcessingState.FAILED)
            .orElse(false);
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
