package com.bcbs239.regtech.riskcalculation.domain.persistence;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;

/**
 * Repository interface for PortfolioAnalysis aggregate
 * Defines persistence operations for portfolio analysis results
 */
public interface PortfolioAnalysisRepository {
    
    /**
     * Save portfolio analysis results
     * 
     * @param analysis the portfolio analysis to save
     * @return Result indicating success or failure
     */
    Result<Void> save(PortfolioAnalysis analysis);
    
    /**
     * Find portfolio analysis by batch identifier
     * 
     * @param batchId the batch identifier
     * @return Maybe containing the analysis if found, empty otherwise
     */
    Maybe<PortfolioAnalysis> findByBatchId(String batchId);
}
