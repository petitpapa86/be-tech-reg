package com.bcbs239.regtech.billing.infrastructure.observability;

import com.bcbs239.regtech.billing.infrastructure.database.entities.SagaAuditLogEntity;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaSagaAuditLogRepository;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Test implementation of saga audit log repository for unit testing.
 * Provides in-memory storage for testing audit logging functionality.
 */
public class TestSagaAuditLogRepository extends JpaSagaAuditLogRepository {

    private final List<SagaAuditLogEntity> savedAuditLogs = new ArrayList<>();
    private List<SagaAuditLogEntity> recentEvents = new ArrayList<>();
    private List<Object[]> statistics = new ArrayList<>();

    @Override
    public Function<SagaAuditLogEntity, Result<String>> sagaAuditLogSaver() {
        return auditLog -> {
            try {
                if (auditLog.getId() == null) {
                    auditLog.setId(UUID.randomUUID().toString());
                }
                savedAuditLogs.add(auditLog);
                return Result.success(auditLog.getId());
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("SAGA_AUDIT_LOG_SAVE_FAILED",
                    "Failed to save saga audit log: " + e.getMessage(),
                    "saga.audit.log.save.failed"));
            }
        };
    }

    @Override
    public Function<String, List<SagaAuditLogEntity>> sagaAuditLogsBySagaIdFinder() {
        return sagaId -> savedAuditLogs.stream()
            .filter(log -> sagaId.equals(log.getSagaId()))
            .collect(Collectors.toList());
    }

    @Override
    public Function<String, List<SagaAuditLogEntity>> sagaAuditLogsBySagaTypeFinder() {
        return sagaType -> savedAuditLogs.stream()
            .filter(log -> sagaType.equals(log.getSagaType()))
            .collect(Collectors.toList());
    }

    @Override
    public Function<String, List<SagaAuditLogEntity>> sagaAuditLogsByUserIdFinder() {
        return userId -> savedAuditLogs.stream()
            .filter(log -> userId.equals(log.getUserId()))
            .collect(Collectors.toList());
    }

    @Override
    public Function<String, List<SagaAuditLogEntity>> sagaAuditLogsByBillingAccountIdFinder() {
        return billingAccountId -> savedAuditLogs.stream()
            .filter(log -> billingAccountId.equals(log.getBillingAccountId()))
            .collect(Collectors.toList());
    }

    @Override
    public List<SagaAuditLogEntity> getRecentSagaEvents(String sagaType, int hours) {
        return recentEvents.stream()
            .filter(event -> sagaType.equals(event.getSagaType()))
            .collect(Collectors.toList());
    }

    @Override
    public List<Object[]> getSagaStatistics(String sagaType, int days) {
        return statistics;
    }

    @Override
    public Result<String> logSagaEvent(String sagaId, String sagaType, String eventType, 
                                      String eventData, String userId, String billingAccountId) {
        try {
            SagaAuditLogEntity auditLog = new SagaAuditLogEntity(
                UUID.randomUUID().toString(),
                sagaId,
                sagaType,
                eventType,
                eventData,
                userId,
                billingAccountId
            );
            
            savedAuditLogs.add(auditLog);
            return Result.success(auditLog.getId());
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("SAGA_AUDIT_LOG_FAILED",
                "Failed to log saga event: " + e.getMessage(),
                "saga.audit.log.failed"));
        }
    }

    // Test helper methods
    public List<SagaAuditLogEntity> getSavedAuditLogs() {
        return new ArrayList<>(savedAuditLogs);
    }

    public void setRecentEvents(List<SagaAuditLogEntity> recentEvents) {
        this.recentEvents = new ArrayList<>(recentEvents);
    }

    public void setStatistics(List<Object[]> statistics) {
        this.statistics = new ArrayList<>(statistics);
    }

    public void clear() {
        savedAuditLogs.clear();
        recentEvents.clear();
        statistics.clear();
    }
}
