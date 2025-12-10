package com.bcbs239.regtech.app.monitoring;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.common.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Business Observation Handler for adding business-specific context to observations.
 * 
 * This handler adds business context like batch IDs, user IDs, and operation outcomes
 * to Spring Boot 4 observations, enabling better tracing and monitoring of business operations.
 * 
 * Requirements: 1.1, 1.2 - Business context in traces
 */
@Component
public class BusinessObservationHandler implements ObservationHandler<Observation.Context> {

    private static final Logger logger = LoggerFactory.getLogger(BusinessObservationHandler.class);

    @Override
    public void onStart(Observation.Context context) {
        // Add business context when observation starts
        addBusinessContext(context);
        
        logger.debug("Started observation with business context: {}", context.getName());
    }

    @Override
    public void onError(Observation.Context context) {
        // Add error-specific business context
        addErrorContext(context);
        
        logger.debug("Observation error with business context: {}", context.getName());
    }

    @Override
    public void onEvent(Observation.Event event, Observation.Context context) {
        // Add event-specific business context
        addEventContext(event, context);
        
        logger.debug("Observation event '{}' with business context: {}", event.getName(), context.getName());
    }

    @Override
    public void onScopeOpened(Observation.Context context) {
        // Called when observation scope is opened
        logger.debug("Opened observation scope with business context: {}", context.getName());
    }

    @Override
    public void onScopeClosed(Observation.Context context) {
        // Called when observation scope is closed
        logger.debug("Closed observation scope with business context: {}", context.getName());
    }

    @Override
    public void onStop(Observation.Context context) {
        // Add completion context when observation stops
        addCompletionContext(context);
        
        logger.debug("Stopped observation with business context: {}", context.getName());
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        // Support all observation contexts for business enhancement
        return true;
    }

    /**
     * Adds business-specific context to the observation.
     * 
     * @param context The observation context
     */
    private void addBusinessContext(Observation.Context context) {
        // Add service identification
        context.addLowCardinalityKeyValue(KeyValue.of("service.type", "regtech"));
        context.addLowCardinalityKeyValue(KeyValue.of("service.layer", determineServiceLayer(context)));
        
        // Add business domain context
        String businessDomain = determineBusinessDomain(context);
        if (businessDomain != null) {
            context.addLowCardinalityKeyValue(KeyValue.of("business.domain", businessDomain));
        }
        
        // Add operation type context
        String operationType = determineOperationType(context);
        if (operationType != null) {
            context.addLowCardinalityKeyValue(KeyValue.of("business.operation.type", operationType));
        }
        
        // Add tenant/organization context if available
        String tenantId = extractTenantId(context);
        if (tenantId != null) {
            context.addLowCardinalityKeyValue(KeyValue.of("business.tenant.id", tenantId));
        }
        
        // Add user context if available
        String userId = extractUserId(context);
        if (userId != null) {
            context.addHighCardinalityKeyValue(KeyValue.of("business.user.id", userId));
        }
        
        // Add batch context if available
        String batchId = extractBatchId(context);
        if (batchId != null) {
            context.addHighCardinalityKeyValue(KeyValue.of("business.batch.id", batchId));
        }
        
        // Add portfolio context if available
        String portfolioId = extractPortfolioId(context);
        if (portfolioId != null) {
            context.addHighCardinalityKeyValue(KeyValue.of("business.portfolio.id", portfolioId));
        }
    }

    /**
     * Adds error-specific business context to the observation.
     * 
     * @param context The observation context
     */
    private void addErrorContext(Observation.Context context) {
        Throwable error = context.getError();
        if (error != null) {
            // Add error classification
            context.addLowCardinalityKeyValue(KeyValue.of("error.type", error.getClass().getSimpleName()));
            context.addLowCardinalityKeyValue(KeyValue.of("error.category", categorizeError(error)));
            
            // Add business error context for domain exceptions
            if (isBusinessException(error)) {
                context.addLowCardinalityKeyValue(KeyValue.of("error.business", "true"));
                context.addHighCardinalityKeyValue(KeyValue.of("error.business.code", extractBusinessErrorCode(error)));
            }
            
            // Add validation error context
            if (isValidationException(error)) {
                context.addLowCardinalityKeyValue(KeyValue.of("error.validation", "true"));
                context.addHighCardinalityKeyValue(KeyValue.of("error.validation.field", extractValidationField(error)));
            }
            
            // Add security error context
            if (isSecurityException(error)) {
                context.addLowCardinalityKeyValue(KeyValue.of("error.security", "true"));
                context.addLowCardinalityKeyValue(KeyValue.of("error.security.type", extractSecurityErrorType(error)));
            }
        }
    }

    /**
     * Adds event-specific business context to the observation.
     * 
     * @param event The observation event
     * @param context The observation context
     */
    private void addEventContext(Observation.Event event, Observation.Context context) {
        // Add event classification
        context.addLowCardinalityKeyValue(KeyValue.of("event.type", event.getName()));
        
        // Add business milestone context for specific events
        if (isBusinessMilestone(event)) {
            context.addLowCardinalityKeyValue(KeyValue.of("business.milestone", "true"));
            context.addLowCardinalityKeyValue(KeyValue.of("business.milestone.type", event.getName()));
        }
        
        // Add processing stage context
        String processingStage = determineProcessingStage(event, context);
        if (processingStage != null) {
            context.addLowCardinalityKeyValue(KeyValue.of("business.processing.stage", processingStage));
        }
    }

    /**
     * Adds completion-specific business context to the observation.
     * 
     * @param context The observation context
     */
    private void addCompletionContext(Observation.Context context) {
        // Add outcome classification
        String outcome = context.getError() != null ? "failure" : "success";
        context.addLowCardinalityKeyValue(KeyValue.of("business.outcome", outcome));
        
        // Add performance classification
        // Note: Duration is not available in context, would need to be calculated externally
        context.addLowCardinalityKeyValue(KeyValue.of("business.performance.category", "normal"));
    }

    // Helper methods for context extraction and classification

    private String determineServiceLayer(Observation.Context context) {
        String contextName = context.getName();
        if (contextName.contains("controller") || contextName.contains("presentation")) {
            return "presentation";
        } else if (contextName.contains("service") || contextName.contains("application")) {
            return "application";
        } else if (contextName.contains("repository") || contextName.contains("infrastructure")) {
            return "infrastructure";
        } else if (contextName.contains("domain")) {
            return "domain";
        }
        return "unknown";
    }

    private String determineBusinessDomain(Observation.Context context) {
        String contextName = context.getName().toLowerCase();
        
        if (contextName.contains("iam") || contextName.contains("auth") || contextName.contains("user")) {
            return "iam";
        } else if (contextName.contains("billing") || contextName.contains("payment") || contextName.contains("subscription")) {
            return "billing";
        } else if (contextName.contains("ingestion") || contextName.contains("batch") || contextName.contains("upload")) {
            return "ingestion";
        } else if (contextName.contains("quality") || contextName.contains("validation") || contextName.contains("rule")) {
            return "data-quality";
        } else if (contextName.contains("risk") || contextName.contains("calculation") || contextName.contains("exposure")) {
            return "risk-calculation";
        } else if (contextName.contains("report") || contextName.contains("generation") || contextName.contains("template")) {
            return "report-generation";
        }
        
        return null;
    }

    private String determineOperationType(Observation.Context context) {
        String contextName = context.getName().toLowerCase();
        
        if (contextName.contains("create") || contextName.contains("insert")) {
            return "create";
        } else if (contextName.contains("read") || contextName.contains("get") || contextName.contains("find")) {
            return "read";
        } else if (contextName.contains("update") || contextName.contains("modify")) {
            return "update";
        } else if (contextName.contains("delete") || contextName.contains("remove")) {
            return "delete";
        } else if (contextName.contains("process") || contextName.contains("calculate")) {
            return "process";
        } else if (contextName.contains("validate") || contextName.contains("check")) {
            return "validate";
        }
        
        return null;
    }

    private String extractTenantId(Observation.Context context) {
        // Extract tenant ID from context if available
        // This would typically come from security context or request headers
        KeyValue kv = context.getHighCardinalityKeyValue("tenant.id");
        return kv != null ? kv.getValue() : null;
    }

    private String extractUserId(Observation.Context context) {
        // Extract user ID from context if available
        // This would typically come from security context
        KeyValue kv = context.getHighCardinalityKeyValue("user.id");
        return kv != null ? kv.getValue() : null;
    }

    private String extractBatchId(Observation.Context context) {
        // Extract batch ID from context if available
        KeyValue kv = context.getHighCardinalityKeyValue("batch.id");
        return kv != null ? kv.getValue() : null;
    }

    private String extractPortfolioId(Observation.Context context) {
        // Extract portfolio ID from context if available
        KeyValue kv = context.getHighCardinalityKeyValue("portfolio.id");
        return kv != null ? kv.getValue() : null;
    }

    private String categorizeError(Throwable error) {
        String errorClass = error.getClass().getSimpleName().toLowerCase();
        
        if (errorClass.contains("validation")) {
            return "validation";
        } else if (errorClass.contains("security") || errorClass.contains("auth")) {
            return "security";
        } else if (errorClass.contains("business") || errorClass.contains("domain")) {
            return "business";
        } else if (errorClass.contains("data") || errorClass.contains("sql") || errorClass.contains("persistence")) {
            return "data";
        } else if (errorClass.contains("network") || errorClass.contains("timeout") || errorClass.contains("connection")) {
            return "infrastructure";
        }
        
        return "system";
    }

    private boolean isBusinessException(Throwable error) {
        String errorClass = error.getClass().getSimpleName().toLowerCase();
        return errorClass.contains("business") || errorClass.contains("domain");
    }

    private String extractBusinessErrorCode(Throwable error) {
        // Extract business error code if the exception supports it
        // This would need to be implemented based on your business exception structure
        return "UNKNOWN";
    }

    private boolean isValidationException(Throwable error) {
        String errorClass = error.getClass().getSimpleName().toLowerCase();
        return errorClass.contains("validation") || errorClass.contains("constraint");
    }

    private String extractValidationField(Throwable error) {
        // Extract validation field if the exception supports it
        // This would need to be implemented based on your validation exception structure
        return "unknown";
    }

    private boolean isSecurityException(Throwable error) {
        String errorClass = error.getClass().getSimpleName().toLowerCase();
        return errorClass.contains("security") || errorClass.contains("auth") || errorClass.contains("access");
    }

    private String extractSecurityErrorType(Throwable error) {
        String errorClass = error.getClass().getSimpleName().toLowerCase();
        
        if (errorClass.contains("auth")) {
            return "authentication";
        } else if (errorClass.contains("access")) {
            return "authorization";
        }
        
        return "security";
    }

    private boolean isBusinessMilestone(Observation.Event event) {
        String eventName = event.getName().toLowerCase();
        return eventName.contains("milestone") || 
               eventName.contains("completed") || 
               eventName.contains("started") || 
               eventName.contains("validated");
    }

    private String determineProcessingStage(Observation.Event event, Observation.Context context) {
        String eventName = event.getName().toLowerCase();
        
        if (eventName.contains("start") || eventName.contains("begin")) {
            return "start";
        } else if (eventName.contains("validate") || eventName.contains("check")) {
            return "validation";
        } else if (eventName.contains("process") || eventName.contains("calculate")) {
            return "processing";
        } else if (eventName.contains("complete") || eventName.contains("finish")) {
            return "completion";
        }
        
        return null;
    }
}