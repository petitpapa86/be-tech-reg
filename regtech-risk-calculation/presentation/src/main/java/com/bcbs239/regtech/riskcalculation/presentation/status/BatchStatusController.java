package com.bcbs239.regtech.riskcalculation.presentation.status;

import com.bcbs239.regtech.core.presentation.controllers.BaseController;
import com.bcbs239.regtech.riskcalculation.presentation.dto.BatchStatusResponseDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ProcessingProgressDTO;
import com.bcbs239.regtech.riskcalculation.presentation.services.BatchStatusQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for querying batch status and processing progress.
 * Provides endpoints to retrieve batch calculation status and active batches.
 * 
 * Requirements: 2.1, 2.2, 5.1
 */
@Component("riskCalculationBatchStatusController")
public class BatchStatusController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchStatusController.class);
    
    private final BatchStatusQueryService batchStatusQueryService;
    
    public BatchStatusController(BatchStatusQueryService batchStatusQueryService) {
        this.batchStatusQueryService = batchStatusQueryService;
    }
    
    /**
     * Get batch status including processing state and available results.
     * Endpoint: GET /api/v1/risk-calculation/batches/{batchId}/status
     * 
     * @param request the server request containing batchId path variable
     * @return ServerResponse with batch status or error
     */
    public ServerResponse getBatchStatus(ServerRequest request) {
        String batchId = request.pathVariable("batchId");
        logger.debug("Processing batch status request for batchId: {}", batchId);
        
        try {
            Optional<BatchStatusResponseDTO> statusOpt = batchStatusQueryService.getBatchStatus(batchId);
            
            if (statusOpt.isEmpty()) {
                logger.warn("Batch not found: {}", batchId);
                return ServerResponse.status(404)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                        "error", "Batch not found",
                        "batch_id", batchId,
                        "message", "No batch exists with the specified ID"
                    ));
            }
            
            BatchStatusResponseDTO status = statusOpt.get();
            logger.debug("Batch status retrieved successfully for batchId: {}, state: {}", 
                batchId, status.getProcessingState());
            
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(status);
                
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid batch ID format: {}", batchId, e);
            return ServerResponse.status(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "error", "Invalid batch ID format",
                    "batch_id", batchId,
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            logger.error("Error retrieving batch status for batchId {}: {}", batchId, e.getMessage(), e);
            return ServerResponse.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "error", "Failed to retrieve batch status",
                    "batch_id", batchId,
                    "message", "An internal error occurred while processing your request"
                ));
        }
    }
    
    /**
     * Get detailed processing progress for a batch.
     * Endpoint: GET /api/v1/risk-calculation/batches/{batchId}/progress
     * 
     * @param request the server request containing batchId path variable
     * @return ServerResponse with processing progress or error
     */
    public ServerResponse getProcessingProgress(ServerRequest request) {
        String batchId = request.pathVariable("batchId");
        logger.debug("Processing progress request for batchId: {}", batchId);
        
        try {
            Optional<ProcessingProgressDTO> progressOpt = batchStatusQueryService.getProcessingProgress(batchId);
            
            if (progressOpt.isEmpty()) {
                logger.warn("No processing progress found for batch: {}", batchId);
                return ServerResponse.status(404)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                        "error", "Processing progress not found",
                        "batch_id", batchId,
                        "message", "No processing progress available for the specified batch"
                    ));
            }
            
            ProcessingProgressDTO progress = progressOpt.get();
            logger.debug("Processing progress retrieved successfully for batchId: {}, completion: {}%", 
                batchId, progress.getPercentageComplete());
            
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(progress);
                
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid batch ID format: {}", batchId, e);
            return ServerResponse.status(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "error", "Invalid batch ID format",
                    "batch_id", batchId,
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            logger.error("Error retrieving processing progress for batchId {}: {}", batchId, e.getMessage(), e);
            return ServerResponse.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "error", "Failed to retrieve processing progress",
                    "batch_id", batchId,
                    "message", "An internal error occurred while processing your request"
                ));
        }
    }
    
    /**
     * Get all active batches with optional bank filter.
     * Endpoint: GET /api/v1/risk-calculation/batches/active
     * Query parameter: bankId (optional)
     * 
     * @param request the server request with optional bankId query parameter
     * @return ServerResponse with list of active batches or error
     */
    public ServerResponse getActiveBatches(ServerRequest request) {
        Optional<String> bankIdParam = request.param("bankId");
        logger.debug("Processing active batches request, bankId filter: {}", bankIdParam.orElse("none"));
        
        try {
            List<BatchStatusResponseDTO> activeBatches = batchStatusQueryService.getActiveBatches(bankIdParam);
            
            logger.debug("Retrieved {} active batches", activeBatches.size());
            
            Map<String, Object> response = Map.of(
                "total_count", activeBatches.size(),
                "bank_id_filter", bankIdParam.orElse(null),
                "batches", activeBatches
            );
            
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
                
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid bank ID format: {}", bankIdParam.orElse(""), e);
            return ServerResponse.status(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "error", "Invalid bank ID format",
                    "bank_id", bankIdParam.orElse(""),
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            logger.error("Error retrieving active batches: {}", e.getMessage(), e);
            return ServerResponse.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "error", "Failed to retrieve active batches",
                    "message", "An internal error occurred while processing your request"
                ));
        }
    }
}
