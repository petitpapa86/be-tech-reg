package com.bcbs239.regtech.riskcalculation.domain.persistence;

import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;

import java.util.Optional;

/**
 * Repository interface for PortfolioAnalysis aggregate
 * Defines persistence operations for portfolio analysis results
 */
public interface PortfolioAnalysisRepository {
    
    /**
     * Save portfolio analysis results
     * 
     * @param analysis the portfolio analysis to save
     */
    void save(PortfolioAnalysis analysis);
    
    /**
     * Find portfolio analysis by batch identifier
     * 
     * @param batchId the batch identifier
     * @return Optional containing the analysis if found, empty otherwise
     */
    Optional<PortfolioAnalysis> findByBatchId(String batchId);
}
