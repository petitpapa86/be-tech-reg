package com.bcbs239.regtech.modules.ingestion.presentation.compliance.lifecycle;

import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ResponseUtils;
import com.bcbs239.regtech.modules.ingestion.infrastructure.compliance.DataCleanupService;
import com.bcbs239.regtech.modules.ingestion.infrastructure.compliance.DataRetentionService;
import com.bcbs239.regtech.modules.ingestion.presentation.common.IEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Functional endpoint for lifecycle policy management operations.
 * Handles S3 lifecycle configuration and data cleanup operations.
 */
@Component
@Slf4j
public class LifecyclePoliciesController extends BaseController implements IEndpoint {
    
    private final DataRetentionService dataRetentionService;
    private final DataCleanupService dataCleanupService;
    
    public LifecyclePoliciesController(DataRetentionService dataRetentionService,
                                     DataCleanupService dataCleanupService) {
        this.dataRetentionService = dataRetentionService;
        this.dataCleanupService = dataCleanupService;
    }
    
    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        return route(POST("/api/v1/ingestion/compliance/configure-lifecycle-policies"), this::configureLifecyclePolicies)
            .and(route(POST("/api/v1/ingestion/compliance/cleanup"), this::performDataCleanup))
            .withAttribute("tags", new String[]{"Lifecycle Policies", "Ingestion"})
            .withAttribute("permissions", new String[]{"ingestion:compliance:manage"});
    }
    
    private ServerResponse configureLifecyclePolicies(ServerRequest request) {
        try {
            log.info("Configuring S3 lifecycle policies");
            
            Result<Void> result = dataRetentionService.configureS3LifecyclePolicies();
            
            if (result.isSuccess()) {
                log.info("Successfully configured S3 lifecycle policies");
                return ServerResponse.ok()
                    .body(ResponseUtils.success(null, "S3 lifecycle policies configured successfully"));
            } else {
                log.error("Failed to configure S3 lifecycle policies: {}", result.getError().orElse(null));
                ResponseEntity<?> responseEntity = handleResult(result, 
                    "S3 lifecycle policies configured", "compliance.lifecycle.success");
                return ServerResponse.status(responseEntity.getStatusCode())
                    .body(responseEntity.getBody());
            }
        } catch (Exception e) {
            log.error("Error configuring S3 lifecycle policies: {}", e.getMessage(), e);
            return ServerResponse.status(500)
                .body(ResponseUtils.systemError("Failed to configure S3 lifecycle policies: " + e.getMessage()));
        }
    }
    
    private ServerResponse performDataCleanup(ServerRequest request) {
        try {
            log.info("Manual data cleanup requested");
            
            Result<DataCleanupService.CleanupSummary> result = dataCleanupService.performAutomatedCleanup();
            
            if (result.isSuccess()) {
                DataCleanupService.CleanupSummary summary = result.getValue();
                log.info("Manual data cleanup completed: {}", summary.getSummary());
                return ServerResponse.ok()
                    .body(ResponseUtils.success(summary, "Data cleanup completed successfully"));
            } else {
                log.error("Failed to perform manual data cleanup: {}", result.getError().orElse(null));
                ResponseEntity<?> responseEntity = handleResult(result, 
                    "Data cleanup completed", "compliance.cleanup.success");
                return ServerResponse.status(responseEntity.getStatusCode())
                    .body(responseEntity.getBody());
            }
        } catch (Exception e) {
            log.error("Error performing data cleanup: {}", e.getMessage(), e);
            return ServerResponse.status(500)
                .body(ResponseUtils.systemError("Failed to perform data cleanup: " + e.getMessage()));
        }
    }
}