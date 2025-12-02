package com.bcbs239.regtech.riskcalculation.presentation.services;

import com.bcbs239.regtech.riskcalculation.domain.analysis.Breakdown;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.persistence.PortfolioAnalysisRepository;
import com.bcbs239.regtech.riskcalculation.presentation.dto.BreakdownDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ConcentrationIndicesDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.PortfolioAnalysisResponseDTO;
import com.bcbs239.regtech.riskcalculation.presentation.mappers.PortfolioAnalysisMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Query service for retrieving portfolio analysis data.
 * Provides read-only access to portfolio analysis, concentration indices, and breakdowns.
 * 
 * Requirements: 2.1, 2.3, 2.4
 */
@Service
@Transactional(readOnly = true)
public class PortfolioAnalysisQueryService {
    
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;
    private final PortfolioAnalysisMapper portfolioAnalysisMapper;
    
    public PortfolioAnalysisQueryService(
        PortfolioAnalysisRepository portfolioAnalysisRepository,
        PortfolioAnalysisMapper portfolioAnalysisMapper
    ) {
        this.portfolioAnalysisRepository = portfolioAnalysisRepository;
        this.portfolioAnalysisMapper = portfolioAnalysisMapper;
    }
    
    /**
     * Retrieves the complete portfolio analysis for a batch.
     * 
     * @param batchId the batch identifier
     * @return Optional containing the portfolio analysis DTO if found
     */
    public Optional<PortfolioAnalysisResponseDTO> getPortfolioAnalysis(String batchId) {
        return portfolioAnalysisRepository.findByBatchId(batchId)
            .map(portfolioAnalysisMapper::toResponseDTO);
    }
    
    /**
     * Retrieves concentration indices for a batch.
     * 
     * @param batchId the batch identifier
     * @return Optional containing the concentration indices DTO if found
     */
    public Optional<ConcentrationIndicesDTO> getConcentrationIndices(String batchId) {
        return portfolioAnalysisRepository.findByBatchId(batchId)
            .map(analysis -> portfolioAnalysisMapper.toConcentrationIndicesDTO(
                analysis.getGeographicHHI(),
                analysis.getSectorHHI()
            ));
    }
    
    /**
     * Retrieves geographic breakdown for a batch.
     * 
     * @param batchId the batch identifier
     * @return Optional containing the geographic breakdown DTO if found
     */
    public Optional<BreakdownDTO> getGeographicBreakdown(String batchId) {
        return portfolioAnalysisRepository.findByBatchId(batchId)
            .map(PortfolioAnalysis::getGeographicBreakdown)
            .map(breakdown -> portfolioAnalysisMapper.toBreakdownDTO(breakdown, "GEOGRAPHIC"));
    }
    
    /**
     * Retrieves sector breakdown for a batch.
     * 
     * @param batchId the batch identifier
     * @return Optional containing the sector breakdown DTO if found
     */
    public Optional<BreakdownDTO> getSectorBreakdown(String batchId) {
        return portfolioAnalysisRepository.findByBatchId(batchId)
            .map(PortfolioAnalysis::getSectorBreakdown)
            .map(breakdown -> portfolioAnalysisMapper.toBreakdownDTO(breakdown, "SECTOR"));
    }
    
    /**
     * Retrieves breakdowns filtered by type.
     * 
     * @param batchId the batch identifier
     * @param type the breakdown type ("GEOGRAPHIC" or "SECTOR"), null returns both
     * @return Optional containing the requested breakdown DTO if found
     */
    public Optional<BreakdownDTO> getBreakdownByType(String batchId, String type) {
        if (type == null) {
            // If no type specified, return geographic by default
            return getGeographicBreakdown(batchId);
        }
        
        return switch (type.toUpperCase()) {
            case "GEOGRAPHIC" -> getGeographicBreakdown(batchId);
            case "SECTOR" -> getSectorBreakdown(batchId);
            default -> Optional.empty();
        };
    }
}
