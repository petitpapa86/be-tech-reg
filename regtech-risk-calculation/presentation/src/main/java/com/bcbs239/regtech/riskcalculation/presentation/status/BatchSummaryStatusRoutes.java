package com.bcbs239.regtech.riskcalculation.presentation.status;

import com.bcbs239.regtech.core.presentation.routing.RouterAttributes;
import com.bcbs239.regtech.riskcalculation.presentation.common.Tags;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Router configuration for batch summary status query endpoints.
 * Defines URL mappings, permissions, and documentation tags.
 */
@Component
public class BatchSummaryStatusRoutes {
    
    private final BatchSummaryStatusController controller;
    
    public BatchSummaryStatusRoutes(BatchSummaryStatusController controller) {
        this.controller = controller;
    }
    
    /**
     * Maps the batch summary status query endpoints.
     * Requires appropriate permissions to view batch summaries.
     */
    public RouterFunction<ServerResponse> createRoutes() {
        return RouterAttributes.withAttributes(
            route(GET("/api/v1/risk-calculation/batches/{batchId}"), controller::getBatchSummary),
            new String[]{"risk-calculation:batches:view"},
            new String[]{Tags.RISK_CALCULATION, Tags.BATCH_SUMMARIES},
            "Get batch summary by batch ID"
        ).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/risk-calculation/banks/{bankId}/batches"), controller::getBatchSummariesByBank),
            new String[]{"risk-calculation:batches:view"},
            new String[]{Tags.RISK_CALCULATION, Tags.BATCH_SUMMARIES},
            "Get all batch summaries for a bank"
        )).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/risk-calculation/batches/{batchId}/exists"), controller::checkBatchExists),
            new String[]{"risk-calculation:batches:view"},
            new String[]{Tags.RISK_CALCULATION, Tags.BATCH_SUMMARIES},
            "Check if a batch has been processed"
        ));
    }
}
