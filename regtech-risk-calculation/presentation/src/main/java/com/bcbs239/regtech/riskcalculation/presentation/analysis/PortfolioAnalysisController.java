package com.bcbs239.regtech.riskcalculation.presentation.analysis;

import com.bcbs239.regtech.core.presentation.controllers.BaseController;
import com.bcbs239.regtech.riskcalculation.presentation.dto.BreakdownDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ConcentrationIndicesDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.PortfolioAnalysisResponseDTO;
import com.bcbs239.regtech.riskcalculation.presentation.exceptions.BatchNotFoundException;
import com.bcbs239.regtech.riskcalculation.presentation.services.PortfolioAnalysisQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for portfolio analysis queries.
 * Provides endpoints to retrieve portfolio analysis, concentration indices, and breakdowns.
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3
 */
@RestController
@RequestMapping("/api/v1/risk-calculation/portfolio-analysis")
public class PortfolioAnalysisController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(PortfolioAnalysisController.class);
    
    private final PortfolioAnalysisQueryService portfolioAnalysisQueryService;
    
    public PortfolioAnalysisController(PortfolioAnalysisQueryService portfolioAnalysisQueryService) {
        this.portfolioAnalysisQueryService = portfolioAnalysisQueryService;
    }
    
    /**
     * Get complete portfolio analysis for a batch.
     * 
     * Endpoint: GET /api/v1/risk-calculation/portfolio-analysis/{batchId}
     * 
     * @param batchId the batch identifier
     * @return ResponseEntity containing the portfolio analysis
     * @throws BatchNotFoundException if the batch is not found
     */
    @GetMapping("/{batchId}")
    public ResponseEntity<PortfolioAnalysisResponseDTO> getPortfolioAnalysis(@PathVariable String batchId) {
        logger.debug("Retrieving portfolio analysis for batchId: {}", batchId);
        
        PortfolioAnalysisResponseDTO analysis = portfolioAnalysisQueryService.getPortfolioAnalysis(batchId)
            .orElseThrow(() -> {
                logger.warn("Portfolio analysis not found for batchId: {}", batchId);
                return new BatchNotFoundException(batchId, 
                    String.format("Portfolio analysis not found for batch: %s", batchId));
            });
        
        logger.debug("Successfully retrieved portfolio analysis for batchId: {}", batchId);
        return ResponseEntity.ok(analysis);
    }
    
    /**
     * Get concentration indices for a batch.
     * 
     * Endpoint: GET /api/v1/risk-calculation/portfolio-analysis/{batchId}/concentrations
     * 
     * @param batchId the batch identifier
     * @return ResponseEntity containing the concentration indices
     * @throws BatchNotFoundException if the batch is not found
     */
    @GetMapping("/{batchId}/concentrations")
    public ResponseEntity<ConcentrationIndicesDTO> getConcentrationIndices(@PathVariable String batchId) {
        logger.debug("Retrieving concentration indices for batchId: {}", batchId);
        
        ConcentrationIndicesDTO indices = portfolioAnalysisQueryService.getConcentrationIndices(batchId)
            .orElseThrow(() -> {
                logger.warn("Concentration indices not found for batchId: {}", batchId);
                return new BatchNotFoundException(batchId, 
                    String.format("Concentration indices not found for batch: %s", batchId));
            });
        
        logger.debug("Successfully retrieved concentration indices for batchId: {}", batchId);
        return ResponseEntity.ok(indices);
    }
    
    /**
     * Get breakdowns for a batch with optional type filter.
     * 
     * Endpoint: GET /api/v1/risk-calculation/portfolio-analysis/{batchId}/breakdowns
     * 
     * @param batchId the batch identifier
     * @param type optional breakdown type filter ("GEOGRAPHIC" or "SECTOR")
     * @return ResponseEntity containing the breakdown
     * @throws BatchNotFoundException if the batch is not found
     * @throws IllegalArgumentException if the type parameter is invalid
     */
    @GetMapping("/{batchId}/breakdowns")
    public ResponseEntity<BreakdownDTO> getBreakdowns(
            @PathVariable String batchId,
            @RequestParam(required = false) String type) {
        
        logger.debug("Retrieving breakdowns for batchId: {} with type filter: {}", batchId, type);
        
        BreakdownDTO breakdown = portfolioAnalysisQueryService.getBreakdownByType(batchId, type)
            .orElseThrow(() -> {
                logger.warn("Breakdown not found for batchId: {} and type: {}", batchId, type);
                return new BatchNotFoundException(batchId, 
                    String.format("Breakdown not found for batch: %s", batchId));
            });
        
        logger.debug("Successfully retrieved breakdown for batchId: {} and type: {}", 
            batchId, type);
        return ResponseEntity.ok(breakdown);
    }
}
