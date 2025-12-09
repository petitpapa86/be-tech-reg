package com.bcbs239.regtech.riskcalculation.presentation.analysis;

import com.bcbs239.regtech.riskcalculation.presentation.common.IEndpoint;
import com.bcbs239.regtech.riskcalculation.presentation.dto.BreakdownDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ConcentrationIndicesDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.PortfolioAnalysisResponseDTO;
import com.bcbs239.regtech.riskcalculation.presentation.exceptions.BatchNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Optional;

/**
 * Controller for portfolio analysis queries.
 * Provides endpoints to retrieve portfolio analysis, concentration indices, and breakdowns.
 * 
 * This controller focuses on orchestrating portfolio analysis queries by delegating to:
 * - PortfolioAnalysisChecker for data retrieval
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3
 */
@Component
public class PortfolioAnalysisController implements IEndpoint {
    
    private static final Logger logger = LoggerFactory.getLogger(PortfolioAnalysisController.class);
    
    private final PortfolioAnalysisChecker portfolioAnalysisChecker;
    
    public PortfolioAnalysisController(PortfolioAnalysisChecker portfolioAnalysisChecker) {
        this.portfolioAnalysisChecker = portfolioAnalysisChecker;
    }
    
    /**
     * Maps the portfolio analysis endpoints.
     * Note: This method is implemented for the IEndpoint interface but routing is 
     * handled by PortfolioAnalysisRoutes to avoid circular dependencies.
     */
    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        // This is handled by PortfolioAnalysisRoutes to avoid circular dependency
        throw new UnsupportedOperationException(
            "Endpoint mapping is handled by PortfolioAnalysisRoutes component"
        );
    }
    
    /**
     * Get complete portfolio analysis for a batch.
     * Endpoint: GET /api/v1/risk-calculation/portfolio-analysis/{batchId}
     */
    public ServerResponse getPortfolioAnalysis(ServerRequest request) {
        String batchId = request.pathVariable("batchId");
        logger.debug("Processing portfolio analysis request for batchId: {}", batchId);
        
        Optional<PortfolioAnalysisResponseDTO> analysis = portfolioAnalysisChecker.getPortfolioAnalysis(batchId);
        
        if (analysis.isEmpty()) {
            logger.warn("Portfolio analysis not found for batchId: {}", batchId);
            throw new BatchNotFoundException(batchId, 
                String.format("Portfolio analysis not found for batch: %s", batchId));
        }
        
        logger.debug("Successfully retrieved portfolio analysis for batchId: {}", batchId);
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(analysis.get());
    }
    
    /**
     * Get concentration indices for a batch.
     * Endpoint: GET /api/v1/risk-calculation/portfolio-analysis/{batchId}/concentrations
     */
    public ServerResponse getConcentrationIndices(ServerRequest request) {
        String batchId = request.pathVariable("batchId");
        logger.debug("Processing concentration indices request for batchId: {}", batchId);
        
        Optional<ConcentrationIndicesDTO> indices = portfolioAnalysisChecker.getConcentrationIndices(batchId);
        
        if (indices.isEmpty()) {
            logger.warn("Concentration indices not found for batchId: {}", batchId);
            throw new BatchNotFoundException(batchId, 
                String.format("Concentration indices not found for batch: %s", batchId));
        }
        
        logger.debug("Successfully retrieved concentration indices for batchId: {}", batchId);
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(indices.get());
    }
    
    /**
     * Get breakdowns for a batch with optional type filter.
     * Endpoint: GET /api/v1/risk-calculation/portfolio-analysis/{batchId}/breakdowns
     */
    public ServerResponse getBreakdownsByType(ServerRequest request) {
        String batchId = request.pathVariable("batchId");
        Optional<String> type = request.param("type");
        
        logger.debug("Processing breakdowns request for batchId: {} with type filter: {}", 
            batchId, type.orElse("none"));
        
        Optional<BreakdownDTO> breakdown = portfolioAnalysisChecker.getBreakdownByType(
            batchId, 
            type.orElse(null)
        );
        
        if (breakdown.isEmpty()) {
            logger.warn("Breakdown not found for batchId: {} and type: {}", batchId, type.orElse("none"));
            throw new BatchNotFoundException(batchId, 
                String.format("Breakdown not found for batch: %s", batchId));
        }
        
        logger.debug("Successfully retrieved breakdown for batchId: {} and type: {}", 
            batchId, type.orElse("none"));
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(breakdown.get());
    }
}
