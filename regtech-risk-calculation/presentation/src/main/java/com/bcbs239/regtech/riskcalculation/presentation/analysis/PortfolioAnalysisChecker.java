package com.bcbs239.regtech.riskcalculation.presentation.analysis;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.riskcalculation.presentation.dto.BreakdownDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ConcentrationIndicesDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.PortfolioAnalysisResponseDTO;
import com.bcbs239.regtech.riskcalculation.presentation.services.PortfolioAnalysisQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Portfolio analysis checker for risk calculation module.
 * Performs queries for portfolio analysis, concentration indices, and breakdowns.
 * Delegates to PortfolioAnalysisQueryService for data retrieval.
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3
 */
@Component
public class PortfolioAnalysisChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(PortfolioAnalysisChecker.class);
    
    private final PortfolioAnalysisQueryService portfolioAnalysisQueryService;
    
    public PortfolioAnalysisChecker(PortfolioAnalysisQueryService portfolioAnalysisQueryService) {
        this.portfolioAnalysisQueryService = portfolioAnalysisQueryService;
    }
    
    /**
     * Retrieves complete portfolio analysis for a batch.
     * 
     * @param batchId the batch identifier
     * @return Optional containing the portfolio analysis if found
     */
    public Optional<PortfolioAnalysisResponseDTO> getPortfolioAnalysis(String batchId) {
        logger.debug("Retrieving portfolio analysis for batchId: {}", batchId);
        Maybe<PortfolioAnalysisResponseDTO> result = portfolioAnalysisQueryService.getPortfolioAnalysis(batchId);
        return result.isPresent() ? Optional.of(result.getValue()) : Optional.empty();
    }
    
    /**
     * Retrieves concentration indices for a batch.
     * 
     * @param batchId the batch identifier
     * @return Optional containing the concentration indices if found
     */
    public Optional<ConcentrationIndicesDTO> getConcentrationIndices(String batchId) {
        logger.debug("Retrieving concentration indices for batchId: {}", batchId);
        Maybe<ConcentrationIndicesDTO> result = portfolioAnalysisQueryService.getConcentrationIndices(batchId);
        return result.isPresent() ? Optional.of(result.getValue()) : Optional.empty();
    }
    
    /**
     * Retrieves breakdowns for a batch with optional type filter.
     * 
     * @param batchId the batch identifier
     * @param type optional breakdown type filter ("GEOGRAPHIC" or "SECTOR")
     * @return Optional containing the breakdown if found
     */
    public Optional<BreakdownDTO> getBreakdownByType(String batchId, String type) {
        logger.debug("Retrieving breakdowns for batchId: {} with type filter: {}", batchId, type);
        Maybe<BreakdownDTO> result = portfolioAnalysisQueryService.getBreakdownByType(batchId, type);
        return result.isPresent() ? Optional.of(result.getValue()) : Optional.empty();
    }
}
