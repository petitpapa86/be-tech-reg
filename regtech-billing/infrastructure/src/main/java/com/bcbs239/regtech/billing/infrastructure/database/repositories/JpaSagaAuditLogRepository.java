package com.bcbs239.regtech.billing.infrastructure.database.repositories;

import com.bcbs239.regtech.billing.infrastructure.database.entities.SagaAuditLogEntity;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * JPA repository for SagaAuditLog using closure-based functional patterns.
 * Provides functional operations for saga audit trail persistence.
 */
@Repository
@Transactional
public class JpaSagaAuditLogRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Function<SagaAuditLogEntity, Result<String>> sagaAuditLogSaver() {
        return auditLog -> {
            if (auditLog.getId() != null) {
                return Result.failure(ErrorDetail.of("SAGA_AUDIT_LOG_SAVE_FAILED",
                    ErrorType.SYSTEM_ERROR,
                    "Cannot save saga audit log with existing ID", "saga.audit.log.save.existing.id"));
            }
            try {
                auditLog.setId(UUID.randomUUID().toString());
                entityManager.persist(auditLog);
                entityManager.flush(); // Ensure the entity is persisted
                
                return Result.success(auditLog.getId());
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("SAGA_AUDIT_LOG_SAVE_FAILED",
                    ErrorType.SYSTEM_ERROR,
                    "Failed to save saga audit log: " + e.getMessage(),
                    "saga.audit.log.save.failed"));
            }
        };
    }

    public Function<String, List<SagaAuditLogEntity>> sagaAuditLogsBySagaIdFinder() {
        return sagaId -> {
            try {
                return entityManager.createQuery(
                    "SELECT sal FROM SagaAuditLogEntity sal WHERE sal.sagaId = :sagaId ORDER BY sal.createdAt ASC", 
                    SagaAuditLogEntity.class)
                    .setParameter("sagaId", sagaId)
                    .getResultList();
            } catch (Exception e) {
                // Log error but return empty list for functional pattern
                return List.of();
            }
        };
    }

    public Function<String, List<SagaAuditLogEntity>> sagaAuditLogsBySagaTypeFinder() {
        return sagaType -> {
            try {
                return entityManager.createQuery(
                    "SELECT sal FROM SagaAuditLogEntity sal WHERE sal.sagaType = :sagaType ORDER BY sal.createdAt DESC", 
                    SagaAuditLogEntity.class)
                    .setParameter("sagaType", sagaType)
                    .getResultList();
            } catch (Exception e) {
                // Log error but return empty list for functional pattern
                return List.of();
            }
        };
    }

    public Function<String, List<SagaAuditLogEntity>> sagaAuditLogsByUserIdFinder() {
        return userId -> {
            try {
                return entityManager.createQuery(
                    "SELECT sal FROM SagaAuditLogEntity sal WHERE sal.userId = :userId ORDER BY sal.createdAt DESC", 
                    SagaAuditLogEntity.class)
                    .setParameter("userId", userId)
                    .getResultList();
            } catch (Exception e) {
                // Log error but return empty list for functional pattern
                return List.of();
            }
        };
    }

    public Function<String, List<SagaAuditLogEntity>> sagaAuditLogsByBillingAccountIdFinder() {
        return billingAccountId -> {
            try {
                return entityManager.createQuery(
                    "SELECT sal FROM SagaAuditLogEntity sal WHERE sal.billingAccountId = :billingAccountId ORDER BY sal.createdAt DESC", 
                    SagaAuditLogEntity.class)
                    .setParameter("billingAccountId", billingAccountId)
                    .getResultList();
            } catch (Exception e) {
                // Log error but return empty list for functional pattern
                return List.of();
            }
        };
    }

    /**
     * Log a saga event for audit trail
     */
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
            
            entityManager.persist(auditLog);
            entityManager.flush();
            
            return Result.success(auditLog.getId());
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("SAGA_AUDIT_LOG_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to log saga event: " + e.getMessage(),
                "saga.audit.log.failed"));
        }
    }

    /**
     * Get recent saga events for monitoring
     */
    public List<SagaAuditLogEntity> getRecentSagaEvents(String sagaType, int hours) {
        try {
            Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
            return entityManager.createQuery(
                "SELECT sal FROM SagaAuditLogEntity sal WHERE sal.sagaType = :sagaType AND sal.createdAt >= :since ORDER BY sal.createdAt DESC", 
                SagaAuditLogEntity.class)
                .setParameter("sagaType", sagaType)
                .setParameter("since", since)
                .getResultList();
        } catch (Exception e) {
            // Log error but return empty list for functional pattern
            return List.of();
        }
    }

    /**
     * Clean up old audit logs (for maintenance)
     */
    public int cleanupOldAuditLogs(int daysToKeep) {
        try {
            Instant cutoffDate = Instant.now().minus(daysToKeep, ChronoUnit.DAYS);
            return entityManager.createQuery(
                "DELETE FROM SagaAuditLogEntity sal WHERE sal.createdAt < :cutoffDate")
                .setParameter("cutoffDate", cutoffDate)
                .executeUpdate();
        } catch (Exception e) {
            // Log error but return 0
            return 0;
        }
    }

    /**
     * Get saga statistics for monitoring
     */
    public List<Object[]> getSagaStatistics(String sagaType, int days) {
        try {
            Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
            return entityManager.createQuery(
                "SELECT sal.eventType, COUNT(sal) FROM SagaAuditLogEntity sal WHERE sal.sagaType = :sagaType AND sal.createdAt >= :since GROUP BY sal.eventType ORDER BY COUNT(sal) DESC")
                .setParameter("sagaType", sagaType)
                .setParameter("since", since)
                .getResultList();
        } catch (Exception e) {
            // Log error but return empty list for functional pattern
            return List.of();
        }
    }
}
