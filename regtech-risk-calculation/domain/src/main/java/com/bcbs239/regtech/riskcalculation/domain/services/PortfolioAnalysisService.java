package com.bcbs239.regtech.riskcalculation.domain.services;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Domain service for orchestrating portfolio analysis.
 * 
 * This service encapsulates the portfolio analysis business logic,
 * providing a clean interface for creating portfolio analyses from
 * classified exposures.
 */
@Slf4j
public class PortfolioAnalysisService {
    
    /**
     * Analyze a portfolio from classified exposures.
     * 
     * @param batchId The batch identifier
     * @param classifiedExposures The classified exposures to analyze
     * @return Result containing the portfolio analysis
     */
    public Result<PortfolioAnalysis> analyzePortfolio(
            String batchId, 
            List<ClassifiedExposure> classifiedExposures) {
        
        try {
            log.info("Starting portfolio analysis for batch: {} with {} exposures", 
                batchId, classifiedExposures.size());
            
            if (classifiedExposures.isEmpty()) {
                return Result.failure("Cannot analyze portfolio with no exposures");
            }
            
            // Delegate to the aggregate's factory method
            PortfolioAnalysis analysis = PortfolioAnalysis.analyze(batchId, classifiedExposures);
            
            log.info("Portfolio analysis completed - Geographic HHI: {}, Sector HHI: {}",
                analysis.getGeographicHHI().value(), analysis.getSectorHHI().value());
            
            return Result.success(analysis);
            
        } catch (Exception e) {
            log.error("Portfolio analysis failed for batch: {}", batchId, e);
            return Result.failure("Portfolio analysis failed: " + e.getMessage());
        }
   