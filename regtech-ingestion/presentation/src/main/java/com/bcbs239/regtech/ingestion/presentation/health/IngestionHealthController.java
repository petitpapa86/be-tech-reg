package com.bcbs239.regtech.ingestion.presentation.health;

import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.web.RouterAttributes;
import com.bcbs239.regtech.ingestion.infrastructure.health.IngestionModuleHealthIndicator;
import com.bcbs239.regtech.ingestion.presentation.common.IEndpoint;
import com.bcbs239.regtech.ingestion.presentation.constants.Tags;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.RequestPredicates.GET;

/**
 * Health check controller for the ingestion module.
 * Provides detailed health information about ingestion components.
 */
@Component
public class IngestionHealthController extends BaseController implements IEndpoint {

    private final IngestionModuleHealthIndicator healthIndicator;

    public IngestionHealthController(IngestionModuleHealthIndicator healthIndicator) {
        this.healthIndicator = healthIndicator;
    }

    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        return RouterAttributes.withAttributes(
            route(GET("/api/v1/ingestion/health"), this::getIngestionHealth),
            null, // No permissions required for health checks
            new String[]{Tags.INGESTION, "Health"},
            "Get detailed health information for the ingestion module"
        );
    }

    /**
     * Get detailed health information for the ingestion module.
     */
    private ServerResponse getIngestionHealth(ServerRequest request) {
        try {
            Health health = healthIndicator.health();
            
            Map<String, Object> response = Map.of(
                "module", "ingestion",
                "status", health.getStatus().getCode(),
                "details", health.getDetails()
            );
            
            // Return appropriate HTTP status based on health
            if (health.getStatus().getCode().equals("UP")) {
                return ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);
            } else {
                return ServerResponse.status(503) // Service Unavailable
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "module", "ingestion",
                "status", "DOWN",
                "error", "Failed to check health: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ServerResponse.status(503)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
    }
}