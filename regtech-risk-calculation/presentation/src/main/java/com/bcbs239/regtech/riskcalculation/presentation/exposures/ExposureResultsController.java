package com.bcbs239.regtech.riskcalculation.presentation.exposures;

import com.bcbs239.regtech.core.presentation.controllers.BaseController;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ClassifiedExposureDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.PagedResponse;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ProtectedExposureDTO;
import com.bcbs239.regtech.riskcalculation.presentation.services.ExposureQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for exposure results queries.
 * Provides paginated endpoints to retrieve classified and protected exposures.
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3
 */
@RestController
@RequestMapping("/api/v1/risk-calculation/exposures")
@Validated
public class ExposureResultsController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExposureResultsController.class);
    
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    
    private final ExposureQueryService exposureQueryService;
    
    public ExposureResultsController(ExposureQueryService exposureQueryService) {
        this.exposureQueryService = exposureQueryService;
    }
    
    /**
     * Get classified exposures for a batch with pagination and optional sector filtering.
     * 
     * Endpoint: GET /api/v1/risk-calculation/exposures/{batchId}/classified
     * 
     * @param batchId the batch identifier
     * @param sector optional economic sector filter (e.g., "RETAIL_MORTGAGE", "SOVEREIGN", "CORPORATE")
     * @param page the page number (0-indexed), defaults to 0
     * @param size the page size, defaults to 20, max 100
     * @return ResponseEntity containing paged classified exposures
     * @throws IllegalArgumentException if parameters are invalid
     */
    @GetMapping("/{batchId}/classified")
    public ResponseEntity<PagedResponse<ClassifiedExposureDTO>> getClassifiedExposures(
            @PathVariable String batchId,
            @RequestParam(required = false) String sector,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        
        logger.debug("Retrieving classified exposures for batchId: {}, sector: {}, page: {}, size: {}", 
            batchId, sector, page, size);
        
        // Validate parameters
        if (page == null || page < 0) {
            page = DEFAULT_PAGE;
        }
        if (size == null || size < 1 || size > MAX_PAGE_SIZE) {
            size = DEFAULT_SIZE;
        }
        
        PagedResponse<ClassifiedExposureDTO> response;
        
        if (sector != null && !sector.isBlank()) {
            // Filter by sector
            logger.debug("Filtering by sector: {}", sector);
            response = exposureQueryService.getClassifiedExposuresBySector(
                batchId, sector, page, size
            );
        } else {
            // No filter, return all
            response = exposureQueryService.getClassifiedExposures(
                batchId, page, size
            );
        }
        
        logger.debug("Successfully retrieved {} classified exposures for batchId: {}", 
            response.getContent().size(), batchId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get protected exposures for a batch with pagination.
     * 
     * Endpoint: GET /api/v1/risk-calculation/exposures/{batchId}/protected
     * 
     * @param batchId the batch identifier
     * @param page the page number (0-indexed), defaults to 0
     * @param size the page size, defaults to 20, max 100
     * @return ResponseEntity containing paged protected exposures
     */
    @GetMapping("/{batchId}/protected")
    public ResponseEntity<PagedResponse<ProtectedExposureDTO>> getProtectedExposures(
            @PathVariable String batchId,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        
        logger.debug("Retrieving protected exposures for batchId: {}, page: {}, size: {}", 
            batchId, page, size);
        
        // Validate parameters
        if (page == null || page < 0) {
            page = DEFAULT_PAGE;
        }
        if (size == null || size < 1 || size > MAX_PAGE_SIZE) {
            size = DEFAULT_SIZE;
        }
        
        PagedResponse<ProtectedExposureDTO> response = 
            exposureQueryService.getProtectedExposures(batchId, page, size);
        
        logger.debug("Successfully retrieved {} protected exposures for batchId: {}", 
            response.getContent().size(), batchId);
        return ResponseEntity.ok(response);
    }
}
