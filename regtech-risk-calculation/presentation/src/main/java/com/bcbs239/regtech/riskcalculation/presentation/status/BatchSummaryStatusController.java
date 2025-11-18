package com.bcbs239.regtech.riskcalculation.presentation.status;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.calculation.BatchSummary;
import com.bcbs239.regtech.riskcalculation.domain.calculation.IBatchSummaryRepository;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.presentation.common.IEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for querying batch summary status and statistics.
 * Provides endpoints to retrieve batch calculation results and status information.
 * 
 * Requirements: 9.5
 */
@Component
public class BatchSummaryStatusController implements IEndpoint {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchSummaryStatusController.class);
    
    private final IBatchSummaryRepository batchSummaryRepository;
    
    public BatchSummaryStatusController(IBatchSummaryRepository batchSummaryRepository) {
        this.batchSummaryRepository = batchSummaryRepository;
    }
    
    /**
     * Maps the batch summary status endpoints.
     * Note: This method is implemented for the IEndpoint interface but routing is 
     * handled by BatchSummaryStatusRoutes to avoid circular dependencies.
     */
    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        // This is handled by BatchSummaryStatusRoutes to avoid circular dependency
        throw new UnsupportedOperationException(
            "Endpoint mapping is handled by BatchSummaryStatusRoutes component"
        );
    }
    
    /**
     * Get batch summary by batch ID.
     * Endpoint: GET /api/v1/risk-calculation/batches/{batchId}
     */
    public ServerResponse getBatchSummary(ServerRequest request) {
        String batchIdParam = request.pathVariable("batchId");
        logger.debug("Processing batch summary request for batchId: {}", batchIdParam);
        
        try {
            BatchId batchId = new BatchId(batchIdParam);
            Optional<BatchSummary> batchSummary = batchSummaryRepository.findByBatchId(batchId);
            
            if (batchSummary.isEmpty()) {
                return ServerResponse.status(404)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                        "error", "Batch summary not found",
                        "batchId", batchIdParam
                    ));
            }
            
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toBatchSummaryResponse(batchSummary.get()));
                
        } catch (Exception e) {
            logger.error("Error retrieving batch summary for batchId {}: {}", batchIdParam, e.getMessage(), e);
            return ServerResponse.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "error", "Failed to retrieve batch summary: " + e.getMessage(),
                    "batchId", batchIdParam
                ));
        }
    }
    
    /**
     * Get all batch summaries for a bank.
     * Endpoint: GET /api/v1/risk-calculation/banks/{bankId}/batches
     */
    public ServerResponse getBatchSummariesByBank(ServerRequest request) {
        String bankIdParam = request.pathVariable("bankId");
        logger.debug("Processing batch summaries request for bankId: {}", bankIdParam);
        
        try {
            BankId bankId = new BankId(bankIdParam);
            Result<List<BatchSummary>> result = batchSummaryRepository.findByBankId(bankId);
            
            if (result.isFailure()) {
                return ServerResponse.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                        "error", "Failed to retrieve batch summaries",
                        "bankId", bankIdParam,
                        "details", result.getError()
                    ));
            }
            
            List<BatchSummary> batches = result.getValue().get();
            List<Map<String, Object>> batchResponses = batches.stream()
                .map(this::toBatchSummaryResponse)
                .toList();
            
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "bankId", bankIdParam,
                    "totalBatches", batches.size(),
                    "batches", batchResponses
                ));
                
        } catch (Exception e) {
            logger.error("Error retrieving batch summaries for bankId {}: {}", bankIdParam, e.getMessage(), e);
            return ServerResponse.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "error", "Failed to retrieve batch summaries: " + e.getMessage(),
                    "bankId", bankIdParam
                ));
        }
    }
    
    /**
     * Check if a batch has been processed.
     * Endpoint: GET /api/v1/risk-calculation/batches/{batchId}/exists
     */
    public ServerResponse checkBatchExists(ServerRequest request) {
        String batchIdParam = request.pathVariable("batchId");
        logger.debug("Checking if batch exists: {}", batchIdParam);
        
        try {
            BatchId batchId = new BatchId(batchIdParam);
            boolean exists = batchSummaryRepository.existsByBatchId(batchId);
            
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "batchId", batchIdParam,
                    "exists", exists,
                    "processed", exists
                ));
                
        } catch (Exception e) {
            logger.error("Error checking batch existence for batchId {}: {}", batchIdParam, e.getMessage(), e);
            return ServerResponse.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "error", "Failed to check batch existence: " + e.getMessage(),
                    "batchId", batchIdParam
                ));
        }
    }
    
    /**
     * Converts a BatchSummary domain object to a response map.
     */
    private Map<String, Object> toBatchSummaryResponse(BatchSummary batchSummary) {
        return Map.of(
            "batchId", batchSummary.getBatchId().value(),
            "bankId", batchSummary.getBankId().value(),
            "status", batchSummary.getStatus().toString(),
            "totalExposures", batchSummary.getTotalExposures().count(),
            "totalAmountEur", batchSummary.getTotalAmountEur().value(),
            "geographicBreakdown", Map.of(
                "italy", Map.of(
                    "amount", batchSummary.getGeographicBreakdown().italyAmount().value(),
                    "percentage", batchSummary.getGeographicBreakdown().italyPercentage().value(),
                    "count", batchSummary.getGeographicBreakdown().italyCount()
                ),
                "euOther", Map.of(
                    "amount", batchSummary.getGeographicBreakdown().euOtherAmount().value(),
                    "percentage", batchSummary.getGeographicBreakdown().euOtherPercentage().value(),
                    "count", batchSummary.getGeographicBreakdown().euOtherCount()
                ),
                "nonEuropean", Map.of(
                    "amount", batchSummary.getGeographicBreakdown().nonEuropeanAmount().value(),
                    "percentage", batchSummary.getGeographicBreakdown().nonEuropeanPercentage().value(),
                    "count", batchSummary.getGeographicBreakdown().nonEuropeanCount()
                )
            ),
            "sectorBreakdown", Map.of(
                "retailMortgage", Map.of(
                    "amount", batchSummary.getSectorBreakdown().retailMortgageAmount().value(),
                    "percentage", batchSummary.getSectorBreakdown().retailMortgagePercentage().value(),
                    "count", batchSummary.getSectorBreakdown().retailMortgageCount()
                ),
                "sovereign", Map.of(
                    "amount", batchSummary.getSectorBreakdown().sovereignAmount().value(),
                    "percentage", batchSummary.getSectorBreakdown().sovereignPercentage().value(),
                    "count", batchSummary.getSectorBreakdown().sovereignCount()
                ),
                "corporate", Map.of(
                    "amount", batchSummary.getSectorBreakdown().corporateAmount().value(),
                    "percentage", batchSummary.getSectorBreakdown().corporatePercentage().value(),
                    "count", batchSummary.getSectorBreakdown().corporateCount()
                ),
                "banking", Map.of(
                    "amount", batchSummary.getSectorBreakdown().bankingAmount().value(),
                    "percentage", batchSummary.getSectorBreakdown().bankingPercentage().value(),
                    "count", batchSummary.getSectorBreakdown().bankingCount()
                ),
                "other", Map.of(
                    "amount", batchSummary.getSectorBreakdown().otherAmount().value(),
                    "percentage", batchSummary.getSectorBreakdown().otherPercentage().value(),
                    "count", batchSummary.getSectorBreakdown().otherCount()
                )
            ),
            "concentrationIndices", Map.of(
                "geographic", batchSummary.getConcentrationIndices().geographicHerfindahl().value(),
                "sector", batchSummary.getConcentrationIndices().sectorHerfindahl().value()
            ),
            "resultFileUri", batchSummary.getResultFileUri().uri(),
            "timestamps", Map.of(
                "startedAt", batchSummary.getTimestamps().startedAt(),
                "completedAt", batchSummary.getTimestamps().completedAt(),
                "failedAt", batchSummary.getTimestamps().failedAt()
            )
        );
    }
}
