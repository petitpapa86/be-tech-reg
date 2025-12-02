package com.bcbs239.regtech.riskcalculation.presentation.status;

import com.bcbs239.regtech.core.presentation.routing.RouterAttributes;
import com.bcbs239.regtech.riskcalculation.presentation.common.Tags;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Router configuration for batch status query endpoints.
 * Defines URL mappings, permissions, and documentation tags.
 * 
 * Requirements: 6.1, 6.2, 6.3
 */
@Component
public class BatchStatusRoutes {
    
    private final BatchStatusController controller;
    
    public BatchStatusRoutes(BatchStatusController controller) {
        this.controller = controller;
    }
    
    /**
     * Maps the batch status query endpoints.
     * Requires RISK_CALCULATION_READ permission to view batch status.
     */
    public RouterFunction<ServerResponse> createRoutes() {
        return RouterAttributes.withAttributes(
            route(GET("/api/v1/risk-calculation/batches/{batchId}/status"), controller::getBatchStatus),
            new String[]{"risk-calculation:batches:view"},
            new String[]{Tags.RISK_CALCULATION, Tags.BATCH_STATUS},
            "Get batch status including processing state and available results"
        ).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/risk-calculation/batches/{batchId}/progress"), controller::getProcessingProgress),
            new String[]{"risk-calculation:batches:view"},
            new String[]{Tags.RISK_CALCULATION, Tags.BATCH_STATUS},
            "Get detailed processing progress for a batch"
        )).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/risk-calculation/batches/active"), controller::getActiveBatches),
            new String[]{"risk-calculation:batches:view"},
            new String[]{Tags.RISK_CALCULATION, Tags.BATCH_STATUS},
            "Get all active batches with optional bank filter"
        ));
    }
}
