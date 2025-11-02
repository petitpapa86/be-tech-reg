package com.bcbs239.regtech.modules.ingestion.presentation.compliance.policies;

import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ResponseUtils;
// import com.bcbs239.regtech.modules.ingestion.infrastructure.compliance.DataRetentionPolicy;
// import com.bcbs239.regtech.modules.ingestion.infrastructure.compliance.DataRetentionService;
import com.bcbs239.regtech.modules.ingestion.presentation.common.IEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RequestPredicates.PUT;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Functional endpoint for retention policy management operations.
 * Handles CRUD operations for data retention policies.
 * 
 * Note: Disabled until compliance services are implemented.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "regtech.ingestion.compliance.enabled", havingValue = "true", matchIfMissing = false)
public class RetentionPoliciesController extends BaseController implements IEndpoint {
    
    // TODO: Implement when compliance services are available
    // private final DataRetentionService dataRetentionService;
    
    public RetentionPoliciesController() {
        // Empty constructor for now
    }
    
    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        return route(GET("/api/v1/ingestion/compliance/retention-policies"), this::getRetentionPolicies)
            .and(route(PUT("/api/v1/ingestion/compliance/retention-policies/{policyId}"), this::updateRetentionPolicy))
            .withAttribute("tags", new String[]{"Retention Policies", "Ingestion"})
            .withAttribute("permissions", new String[]{"ingestion:compliance:manage"});
    }
    
    private ServerResponse getRetentionPolicies(ServerRequest request) {
        try {
            log.info("Retrieving current retention policies");
            
            Map<String, DataRetentionPolicy> policies = dataRetentionService.getRetentionPolicies();
            return ServerResponse.ok()
                .body(ResponseUtils.success(policies, "Retention policies retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving retention policies: {}", e.getMessage(), e);
            return ServerResponse.status(500)
                .body(ResponseUtils.systemError("Failed to retrieve retention policies: " + e.getMessage()));
        }
    }
    
    private ServerResponse updateRetentionPolicy(ServerRequest request) {
        try {
            String policyId = request.pathVariable("policyId");
            log.info("Updating retention policy: {}", policyId);
            
            // Parse request body
            DataRetentionPolicy policy = request.body(DataRetentionPolicy.class);
            
            // Ensure the policy ID in the path matches the policy object
            if (!policyId.equals(policy.getPolicyId())) {
                return ServerResponse.badRequest()
                    .body(ResponseUtils.validationError(
                        java.util.List.of(),
                        "Policy ID in path does not match policy object"
                    ));
            }
            
            Result<Void> result = dataRetentionService.updateRetentionPolicy(policy);
            
            if (result.isSuccess()) {
                log.info("Successfully updated retention policy: {}", policyId);
                return ServerResponse.ok()
                    .body(ResponseUtils.success(null, "Retention policy updated successfully"));
            } else {
                log.error("Failed to update retention policy {}: {}", policyId, result.getError().orElse(null));
                ResponseEntity<?> responseEntity = handleResult(result, 
                    "Retention policy updated", "compliance.policy.success");
                return ServerResponse.status(responseEntity.getStatusCode())
                    .body(responseEntity.getBody());
            }
        } catch (Exception e) {
            log.error("Error updating retention policy: {}", e.getMessage(), e);
            return ServerResponse.status(500)
                .body(ResponseUtils.systemError("Failed to update retention policy: " + e.getMessage()));
        }
    }
}