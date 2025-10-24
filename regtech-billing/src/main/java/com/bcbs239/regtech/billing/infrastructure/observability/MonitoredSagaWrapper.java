package com.bcbs239.regtech.billing.infrastructure.observability;

import com.bcbs239.regtech.billing.application.policies.MonthlyBillingSagaData;
import com.bcbs239.regtech.core.saga.Saga;
import com.bcbs239.regtech.core.saga.SagaData;
import com.bcbs239.regtech.core.saga.SagaResult;
import com.bcbs239.regtech.core.saga.SagaMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper that adds monitoring and audit logging to saga execution.
 * Implements the decorator pattern to add observability without modifying saga logic.
 */
public class MonitoredSagaWrapper<T extends SagaData> implements Saga<T> {

    private final Saga<T> wrappedSaga;
    private final BillingSagaAuditService auditService;
    private final BillingPerformanceMetricsService metricsService;
    private final ObjectMapper objectMapper;

    public MonitoredSagaWrapper(Saga<T> wrappedSaga,
                               BillingSagaAuditService auditService,
                               BillingPerformanceMetricsService metricsService,
                               ObjectMapper objectMapper) {
        this.wrappedSaga = wrappedSaga;
        this.auditService = auditService;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public SagaResult execute(T sagaData) {
        String operationId = sagaData.getSagaId() + "-execute-" + System.currentTimeMillis();
        Instant startTime = Instant.now();
        
        // Start performance monitoring
        metricsService.startOperation(operationId, "billing.saga.execution");
        
        // Log saga execution start
        logSagaExecutionStart(sagaData);
        
        try {
            // Capture state before execution
            String previousState = getCurrentSagaState(sagaData);
            
            // Execute the wrapped saga
            SagaResult result = wrappedSaga.execute(sagaData);
            
            // Calculate execution time
            Duration executionTime = Duration.between(startTime, Instant.now());
            
            // Capture state after execution
            String newState = getCurrentSagaState(sagaData);
            
            // Record performance metrics
            metricsService.endOperation(operationId, "billing.saga.execution", result.isSuccess());
            metricsService.recordSagaExecution(wrappedSaga.getSagaType(), executionTime, result.isSuccess());
            
            // Log state change for audit trail
            if (!previousState.equals(newState)) {
                auditService.logSagaStateChange(
                    sagaData.getSagaId(),
                    wrappedSaga.getSagaType(),
                    previousState,
                    newState,
                    sagaData,
                    extractUserId(sagaData),
                    extractBillingAccountId(sagaData)
                );
            }
            
            // Log billing calculations if this is a billing saga
            if (sagaData instanceof MonthlyBillingSagaData billingData) {
                logBillingCalculations(billingData);
            }
            
            // Log saga completion
            auditService.logSagaCompletion(
                sagaData.getSagaId(),
                wrappedSaga.getSagaType(),
                extractUserId(sagaData),
                extractBillingAccountId(sagaData),
                result.isSuccess(),
                result.isSuccess() ? "Saga completed successfully" : (result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown error"),
                sagaData
            );
            
            return result;
            
        } catch (Exception e) {
            // Calculate execution time for failed execution
            Duration executionTime = Duration.between(startTime, Instant.now());
            
            // Record failure metrics
            metricsService.endOperation(operationId, "billing.saga.execution", false);
            metricsService.recordSagaExecution(wrappedSaga.getSagaType(), executionTime, false);
            
            // Log saga failure
            auditService.logSagaCompletion(
                sagaData.getSagaId(),
                wrappedSaga.getSagaType(),
                extractUserId(sagaData),
                extractBillingAccountId(sagaData),
                false,
                "Saga execution failed: " + e.getMessage(),
                sagaData
            );
            
            return SagaResult.failure("Monitored saga execution failed: " + e.getMessage());
        }
    }

    @Override
    public SagaResult handleMessage(T sagaData, SagaMessage message) {
        String operationId = sagaData.getSagaId() + "-message-" + System.currentTimeMillis();
        Instant startTime = Instant.now();
        
        // Start performance monitoring
        metricsService.startOperation(operationId, "billing.saga.message-handling");
        
        try {
            // Log message handling start
            logMessageHandlingStart(sagaData, message);
            
            // Handle the message with the wrapped saga
            SagaResult result = wrappedSaga.handleMessage(sagaData, message);
            
            // Calculate execution time
            Duration executionTime = Duration.between(startTime, Instant.now());
            
            // Record performance metrics
            metricsService.endOperation(operationId, "billing.saga.message-handling", result.isSuccess());
            
            // Log message handling completion
            logMessageHandlingCompletion(sagaData, message, result.isSuccess());
            
            return result;
            
        } catch (Exception e) {
            // Calculate execution time for failed message handling
            Duration executionTime = Duration.between(startTime, Instant.now());
            
            // Record failure metrics
            metricsService.endOperation(operationId, "billing.saga.message-handling", false);
            
            // Log message handling failure
            logMessageHandlingCompletion(sagaData, message, false);
            
            return SagaResult.failure("Monitored message handling failed: " + e.getMessage());
        }
    }

    @Override
    public SagaResult compensate(T sagaData) {
        String operationId = sagaData.getSagaId() + "-compensate-" + System.currentTimeMillis();
        Instant startTime = Instant.now();
        
        // Start performance monitoring
        metricsService.startOperation(operationId, "billing.saga.compensation");
        
        try {
            // Log compensation start
            logCompensationStart(sagaData);
            
            // Execute compensation with the wrapped saga
            SagaResult result = wrappedSaga.compensate(sagaData);
            
            // Calculate execution time
            Duration executionTime = Duration.between(startTime, Instant.now());
            
            // Record performance metrics
            metricsService.endOperation(operationId, "billing.saga.compensation", result.isSuccess());
            
            // Log compensation completion
            auditService.logSagaCompletion(
                sagaData.getSagaId(),
                wrappedSaga.getSagaType(),
                extractUserId(sagaData),
                extractBillingAccountId(sagaData),
                result.isSuccess(),
                result.isSuccess() ? "Saga compensation completed successfully" : "Saga compensation failed",
                sagaData
            );
            
            return result;
            
        } catch (Exception e) {
            // Calculate execution time for failed compensation
            Duration executionTime = Duration.between(startTime, Instant.now());
            
            // Record failure metrics
            metricsService.endOperation(operationId, "billing.saga.compensation", false);
            
            // Log compensation failure
            auditService.logSagaCompletion(
                sagaData.getSagaId(),
                wrappedSaga.getSagaType(),
                extractUserId(sagaData),
                extractBillingAccountId(sagaData),
                false,
                "Saga compensation failed: " + e.getMessage(),
                sagaData
            );
            
            return SagaResult.failure("Monitored compensation failed: " + e.getMessage());
        }
    }

    @Override
    public String getSagaType() {
        return wrappedSaga.getSagaType();
    }

    // Helper methods for audit logging

    private void logSagaExecutionStart(T sagaData) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("sagaId", sagaData.getSagaId());
            eventData.put("sagaType", wrappedSaga.getSagaType());
            eventData.put("correlationId", sagaData.getCorrelationId());
            eventData.put("timestamp", Instant.now().toString());
            eventData.put("action", "SAGA_EXECUTION_STARTED");

            auditService.logSagaStateChange(
                sagaData.getSagaId(),
                wrappedSaga.getSagaType(),
                "INITIALIZED",
                "EXECUTING",
                eventData,
                extractUserId(sagaData),
                extractBillingAccountId(sagaData)
            );
        } catch (Exception e) {
            // Log error but don't fail saga execution
            System.err.println("Failed to log saga execution start: " + e.getMessage());
        }
    }

    private void logMessageHandlingStart(T sagaData, SagaMessage message) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("messageType", message.getType());
            eventData.put("messageSource", message.getSource());
            eventData.put("messageTarget", message.getTarget());
            eventData.put("timestamp", Instant.now().toString());
            eventData.put("action", "MESSAGE_HANDLING_STARTED");

            auditService.logSagaStateChange(
                sagaData.getSagaId(),
                wrappedSaga.getSagaType(),
                "EXECUTING",
                "HANDLING_MESSAGE",
                eventData,
                extractUserId(sagaData),
                extractBillingAccountId(sagaData)
            );
        } catch (Exception e) {
            // Log error but don't fail message handling
            System.err.println("Failed to log message handling start: " + e.getMessage());
        }
    }

    private void logMessageHandlingCompletion(T sagaData, SagaMessage message, boolean successful) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("messageType", message.getType());
            eventData.put("successful", successful);
            eventData.put("timestamp", Instant.now().toString());
            eventData.put("action", "MESSAGE_HANDLING_COMPLETED");

            auditService.logSagaStateChange(
                sagaData.getSagaId(),
                wrappedSaga.getSagaType(),
                "HANDLING_MESSAGE",
                "EXECUTING",
                eventData,
                extractUserId(sagaData),
                extractBillingAccountId(sagaData)
            );
        } catch (Exception e) {
            // Log error but don't fail message handling
            System.err.println("Failed to log message handling completion: " + e.getMessage());
        }
    }

    private void logCompensationStart(T sagaData) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("timestamp", Instant.now().toString());
            eventData.put("action", "COMPENSATION_STARTED");
            eventData.put("reason", "Saga compensation triggered");

            auditService.logSagaStateChange(
                sagaData.getSagaId(),
                wrappedSaga.getSagaType(),
                "FAILED",
                "COMPENSATING",
                eventData,
                extractUserId(sagaData),
                extractBillingAccountId(sagaData)
            );
        } catch (Exception e) {
            // Log error but don't fail compensation
            System.err.println("Failed to log compensation start: " + e.getMessage());
        }
    }

    private void logBillingCalculations(MonthlyBillingSagaData billingData) {
        try {
            if (billingData.getSubscriptionCharges() != null && billingData.getOverageCharges() != null) {
                Map<String, Object> calculationDetails = new HashMap<>();
                calculationDetails.put("subscriptionAmount", billingData.getSubscriptionCharges().amount());
                calculationDetails.put("overageAmount", billingData.getOverageCharges().amount());
                calculationDetails.put("totalAmount", billingData.getTotalCharges().amount());
                calculationDetails.put("totalExposures", billingData.getTotalExposures());
                calculationDetails.put("exposureLimit", 10000); // STARTER tier limit
                calculationDetails.put("billingPeriod", billingData.getBillingPeriodId());
                calculationDetails.put("currency", "EUR");
                calculationDetails.put("tier", "STARTER");

                auditService.logBillingCalculation(
                    billingData.getSagaId(),
                    wrappedSaga.getSagaType(),
                    billingData.getUserId().getValue(),
                    extractBillingAccountId(billingData),
                    calculationDetails
                );

                // Also log invoice amount determination if invoice was generated
                if (billingData.getGeneratedInvoiceId() != null) {
                    Map<String, Object> amountDetails = new HashMap<>();
                    amountDetails.put("baseSubscription", billingData.getSubscriptionCharges().amount());
                    amountDetails.put("usageOverage", billingData.getOverageCharges().amount());
                    amountDetails.put("finalAmount", billingData.getTotalCharges().amount());
                    amountDetails.put("calculationMethod", "Base subscription + usage overage");
                    amountDetails.put("overageRate", "0.05 EUR per exposure over limit");

                    auditService.logInvoiceAmountDetermination(
                        billingData.getSagaId(),
                        wrappedSaga.getSagaType(),
                        billingData.getUserId().getValue(),
                        extractBillingAccountId(billingData),
                        billingData.getGeneratedInvoiceId().getValue(),
                        amountDetails
                    );
                }
            }
        } catch (Exception e) {
            // Log error but don't fail saga execution
            System.err.println("Failed to log billing calculations: " + e.getMessage());
        }
    }

    private String getCurrentSagaState(T sagaData) {
        try {
            if (sagaData instanceof MonthlyBillingSagaData billingData) {
                return billingData.getCurrentStep().toString();
            }
            return "UNKNOWN_STATE";
        } catch (Exception e) {
            return "ERROR_GETTING_STATE";
        }
    }

    private String extractUserId(SagaData sagaData) {
        try {
            if (sagaData instanceof MonthlyBillingSagaData billingData) {
                return billingData.getUserId().getValue();
            }
            return "unknown-user";
        } catch (Exception e) {
            return "error-extracting-user";
        }
    }

    private String extractBillingAccountId(SagaData sagaData) {
        try {
            if (sagaData instanceof MonthlyBillingSagaData billingData) {
                // In a real implementation, this would be extracted from the saga data
                // For now, we'll construct it based on user ID
                return "billing-account-" + billingData.getUserId().getValue();
            }
            return "unknown-billing-account";
        } catch (Exception e) {
            return "error-extracting-billing-account";
        }
    }

    /**
     * Factory method to create a monitored wrapper for a saga
     */
    public static <T extends SagaData> MonitoredSagaWrapper<T> wrap(
            Saga<T> saga,
            BillingSagaAuditService auditService,
            BillingPerformanceMetricsService metricsService,
            ObjectMapper objectMapper) {
        return new MonitoredSagaWrapper<>(saga, auditService, metricsService, objectMapper);
    }
}
