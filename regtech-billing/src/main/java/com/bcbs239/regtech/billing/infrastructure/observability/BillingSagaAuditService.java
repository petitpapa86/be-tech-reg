package com.bcbs239.regtech.billing.infrastructure.observability;

import com.bcbs239.regtech.billing.infrastructure.database.entities.SagaAuditLogEntity;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaSagaAuditLogRepository;
import com.bcbs239.regtech.core.shared.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for saga audit logging and monitoring.
 * Implements compliance requirements for billing saga audit trails.
 */
@Service
public class BillingSagaAuditService {

    private final JpaSagaAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public BillingSagaAuditService(JpaSagaAuditLogRepository auditLogRepository,
                                  ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Log saga state change for compliance audit trail
     */
    public Result<String> logSagaStateChange(String sagaId, String sagaType, String previousState, 
                                           String newState, Object sagaData, String userId, 
                                           String billingAccountId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("previousState", previousState);
            eventData.put("newState", newState);
            eventData.put("sagaData", sagaData);
            eventData.put("timestamp", Instant.now().toString());
            eventData.put("changeReason", "Saga step progression");

            String eventDataJson = objectMapper.writeValueAsString(eventData);

            SagaAuditLogEntity auditLog = new SagaAuditLogEntity(
                UUID.randomUUID().toString(),
                sagaId,
                sagaType,
                "STATE_CHANGE",
                eventDataJson,
                userId,
                billingAccountId
            );

            return auditLogRepository.sagaAuditLogSaver().apply(auditLog);

        } catch (JsonProcessingException e) {
            return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of(
                "AUDIT_SERIALIZATION_FAILED",
                "Failed to serialize audit data: " + e.getMessage(),
                "audit.serialization.failed"
            ));
        }
    }

    /**
     * Log billing calculation details for audit trail
     */
    public Result<String> logBillingCalculation(String sagaId, String sagaType, String userId,
                                              String billingAccountId, Map<String, Object> calculationDetails) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("calculationType", "BILLING_CALCULATION");
            eventData.put("calculationDetails", calculationDetails);
            eventData.put("timestamp", Instant.now().toString());
            eventData.put("methodology", "Tier-based pricing with overage calculation");

            String eventDataJson = objectMapper.writeValueAsString(eventData);

            SagaAuditLogEntity auditLog = new SagaAuditLogEntity(
                UUID.randomUUID().toString(),
                sagaId,
                sagaType,
                "BILLING_CALCULATION",
                eventDataJson,
                userId,
                billingAccountId
            );

            return auditLogRepository.sagaAuditLogSaver().apply(auditLog);

        } catch (JsonProcessingException e) {
            return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of(
                "AUDIT_SERIALIZATION_FAILED",
                "Failed to serialize calculation audit data: " + e.getMessage(),
                "audit.serialization.failed"
            ));
        }
    }

    /**
     * Log invoice amount determination for audit trail
     */
    public Result<String> logInvoiceAmountDetermination(String sagaId, String sagaType, String userId,
                                                       String billingAccountId, String invoiceId,
                                                       Map<String, Object> amountDetails) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("invoiceId", invoiceId);
            eventData.put("amountDetails", amountDetails);
            eventData.put("timestamp", Instant.now().toString());
            eventData.put("calculationMethodology", "Base subscription + usage overage");

            String eventDataJson = objectMapper.writeValueAsString(eventData);

            SagaAuditLogEntity auditLog = new SagaAuditLogEntity(
                UUID.randomUUID().toString(),
                sagaId,
                sagaType,
                "INVOICE_AMOUNT_DETERMINATION",
                eventDataJson,
                userId,
                billingAccountId
            );

            return auditLogRepository.sagaAuditLogSaver().apply(auditLog);

        } catch (JsonProcessingException e) {
            return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of(
                "AUDIT_SERIALIZATION_FAILED",
                "Failed to serialize invoice amount audit data: " + e.getMessage(),
                "audit.serialization.failed"
            ));
        }
    }

    /**
     * Log billing account status change for audit trail
     */
    public Result<String> logBillingAccountStatusChange(String sagaId, String sagaType, String userId,
                                                       String billingAccountId, String previousStatus,
                                                       String newStatus, String reason) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("billingAccountId", billingAccountId);
            eventData.put("previousStatus", previousStatus);
            eventData.put("newStatus", newStatus);
            eventData.put("reason", reason);
            eventData.put("timestamp", Instant.now().toString());

            String eventDataJson = objectMapper.writeValueAsString(eventData);

            SagaAuditLogEntity auditLog = new SagaAuditLogEntity(
                UUID.randomUUID().toString(),
                sagaId,
                sagaType,
                "BILLING_ACCOUNT_STATUS_CHANGE",
                eventDataJson,
                userId,
                billingAccountId
            );

            return auditLogRepository.sagaAuditLogSaver().apply(auditLog);

        } catch (JsonProcessingException e) {
            return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of(
                "AUDIT_SERIALIZATION_FAILED",
                "Failed to serialize status change audit data: " + e.getMessage(),
                "audit.serialization.failed"
            ));
        }
    }

    /**
     * Log saga completion for compliance reporting
     */
    public Result<String> logSagaCompletion(String sagaId, String sagaType, String userId,
                                          String billingAccountId, boolean successful, 
                                          String completionReason, Object finalState) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("successful", successful);
            eventData.put("completionReason", completionReason);
            eventData.put("finalState", finalState);
            eventData.put("timestamp", Instant.now().toString());
            eventData.put("complianceReportingReady", true);

            String eventDataJson = objectMapper.writeValueAsString(eventData);

            SagaAuditLogEntity auditLog = new SagaAuditLogEntity(
                UUID.randomUUID().toString(),
                sagaId,
                sagaType,
                successful ? "SAGA_COMPLETED_SUCCESS" : "SAGA_COMPLETED_FAILURE",
                eventDataJson,
                userId,
                billingAccountId
            );

            return auditLogRepository.sagaAuditLogSaver().apply(auditLog);

        } catch (JsonProcessingException e) {
            return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of(
                "AUDIT_SERIALIZATION_FAILED",
                "Failed to serialize saga completion audit data: " + e.getMessage(),
                "audit.serialization.failed"
            ));
        }
    }

    /**
     * Get audit trail for a specific saga
     */
    public List<SagaAuditLogEntity> getSagaAuditTrail(String sagaId) {
        return auditLogRepository.sagaAuditLogsBySagaIdFinder().apply(sagaId);
    }

    /**
     * Get audit trail for a specific user
     */
    public List<SagaAuditLogEntity> getUserAuditTrail(String userId) {
        return auditLogRepository.sagaAuditLogsByUserIdFinder().apply(userId);
    }

    /**
     * Get audit trail for a specific billing account
     */
    public List<SagaAuditLogEntity> getBillingAccountAuditTrail(String billingAccountId) {
        return auditLogRepository.sagaAuditLogsByBillingAccountIdFinder().apply(billingAccountId);
    }

    /**
     * Get recent saga events for monitoring
     */
    public List<SagaAuditLogEntity> getRecentSagaEvents(String sagaType, int hours) {
        return auditLogRepository.getRecentSagaEvents(sagaType, hours);
    }

    /**
     * Get saga statistics for monitoring dashboard
     */
    public List<Object[]> getSagaStatistics(String sagaType, int days) {
        return auditLogRepository.getSagaStatistics(sagaType, days);
    }

    /**
     * Generate compliance report for a specific period
     */
    public Map<String, Object> generateComplianceReport(String sagaType, int days) {
        List<Object[]> statistics = getSagaStatistics(sagaType, days);
        List<SagaAuditLogEntity> recentEvents = getRecentSagaEvents(sagaType, days * 24);

        Map<String, Object> report = new HashMap<>();
        report.put("sagaType", sagaType);
        report.put("reportPeriodDays", days);
        report.put("generatedAt", Instant.now().toString());
        report.put("totalEvents", recentEvents.size());
        report.put("eventStatistics", statistics);
        
        // Calculate success/failure rates
        long successfulCompletions = recentEvents.stream()
            .filter(event -> "SAGA_COMPLETED_SUCCESS".equals(event.getEventType()))
            .count();
        long failedCompletions = recentEvents.stream()
            .filter(event -> "SAGA_COMPLETED_FAILURE".equals(event.getEventType()))
            .count();
        
        report.put("successfulCompletions", successfulCompletions);
        report.put("failedCompletions", failedCompletions);
        
        if (successfulCompletions + failedCompletions > 0) {
            double successRate = (double) successfulCompletions / (successfulCompletions + failedCompletions) * 100;
            report.put("successRate", String.format("%.2f%%", successRate));
        } else {
            report.put("successRate", "N/A");
        }

        return report;
    }
}
