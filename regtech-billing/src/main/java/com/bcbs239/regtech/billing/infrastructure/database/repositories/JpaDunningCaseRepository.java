package com.bcbs239.regtech.billing.infrastructure.database.repositories;

import com.bcbs239.regtech.billing.domain.billing.DunningCase;
import com.bcbs239.regtech.billing.domain.valueobjects.DunningCaseId;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.DunningCaseStatus;
import com.bcbs239.regtech.billing.domain.valueobjects.DunningStep;
import com.bcbs239.regtech.billing.infrastructure.database.entities.DunningCaseEntity;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JPA repository for DunningCase using closure-based functional patterns.
 * Provides functional operations for DunningCase persistence.
 */
@Repository
@Transactional
public class JpaDunningCaseRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public Function<DunningCaseId, Maybe<DunningCase>> dunningCaseFinder() {
        return id -> {
            try {
                DunningCaseEntity entity = entityManager.find(DunningCaseEntity.class, id.value());
                if (entity == null) {
                    return Maybe.none();
                }
                return Maybe.some(entity.toDomain());
            } catch (Exception e) {
                // Log error but return none for functional pattern
                return Maybe.none();
            }
        };
    }

    public Function<InvoiceId, Maybe<DunningCase>> dunningCaseByInvoiceIdFinder() {
        return invoiceId -> {
            try {
                DunningCaseEntity entity = entityManager.createQuery(
                    "SELECT dc FROM DunningCaseEntity dc WHERE dc.invoiceId = :invoiceId", 
                    DunningCaseEntity.class)
                    .setParameter("invoiceId", invoiceId.value())
                    .getSingleResult();
                return Maybe.some(entity.toDomain());
            } catch (NoResultException e) {
                return Maybe.none();
            } catch (Exception e) {
                // Log error but return none for functional pattern
                return Maybe.none();
            }
        };
    }

    public Function<BillingAccountId, List<DunningCase>> dunningCasesByAccountFinder() {
        return billingAccountId -> {
            try {
                List<DunningCaseEntity> entities = entityManager.createQuery(
                    "SELECT dc FROM DunningCaseEntity dc WHERE dc.billingAccountId = :billingAccountId ORDER BY dc.createdAt DESC", 
                    DunningCaseEntity.class)
                    .setParameter("billingAccountId", billingAccountId.value())
                    .getResultList();
                
                return entities.stream()
                    .map(DunningCaseEntity::toDomain)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                // Log error but return empty list for functional pattern
                return List.of();
            }
        };
    }

    public Function<DunningCaseStatus, List<DunningCase>> dunningCasesByStatusFinder() {
        return status -> {
            try {
                List<DunningCaseEntity> entities = entityManager.createQuery(
                    "SELECT dc FROM DunningCaseEntity dc WHERE dc.status = :status ORDER BY dc.createdAt ASC", 
                    DunningCaseEntity.class)
                    .setParameter("status", status)
                    .getResultList();
                
                return entities.stream()
                    .map(DunningCaseEntity::toDomain)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                // Log error but return empty list for functional pattern
                return List.of();
            }
        };
    }

    public Function<DunningStep, List<DunningCase>> dunningCasesByStepFinder() {
        return step -> {
            try {
                List<DunningCaseEntity> entities = entityManager.createQuery(
                    "SELECT dc FROM DunningCaseEntity dc WHERE dc.currentStep = :step AND dc.status = :status ORDER BY dc.updatedAt ASC", 
                    DunningCaseEntity.class)
                    .setParameter("step", step)
                    .setParameter("status", DunningCaseStatus.IN_PROGRESS)
                    .getResultList();
                
                return entities.stream()
                    .map(DunningCaseEntity::toDomain)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                // Log error but return empty list for functional pattern
                return List.of();
            }
        };
    }

    public Function<DunningCase, Result<DunningCaseId>> dunningCaseSaver() {
        return dunningCase -> {
            return transactionTemplate.execute(status -> {
                try {
                    DunningCaseEntity entity = DunningCaseEntity.fromDomain(dunningCase);

                    if (dunningCase.getId() == null) {
                        entityManager.persist(entity);
                    } else {
                        entity = entityManager.merge(entity);
                    }

                    entityManager.flush(); // Ensure the entity is persisted

                    return Result.success(DunningCaseId.fromString(entity.getId()).getValue().orElseThrow());
                } catch (Exception e) {
                    return Result.failure(ErrorDetail.of("DUNNING_CASE_SAVE_FAILED",
                        "Failed to save dunning case: " + e.getMessage(), "dunning.case.save.failed"));
                }
            });
         };
     }

    /**
     * Find active dunning cases that need processing (for scheduled jobs)
     */
    public List<DunningCase> findActiveDunningCases() {
        try {
            List<DunningCaseEntity> entities = entityManager.createQuery(
                "SELECT dc FROM DunningCaseEntity dc WHERE dc.status = :status ORDER BY dc.updatedAt ASC", 
                DunningCaseEntity.class)
                .setParameter("status", DunningCaseStatus.IN_PROGRESS)
                .getResultList();
            
            return entities.stream()
                .map(DunningCaseEntity::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            // Log error but return empty list for functional pattern
            return List.of();
        }
    }

    /**
     * Find dunning cases ready for their next action
     */
    public List<DunningCase> findReadyForAction() {
        try {
            List<DunningCaseEntity> entities = entityManager.createQuery(
                "SELECT dc FROM DunningCaseEntity dc WHERE dc.status = :status AND dc.nextActionDate <= CURRENT_DATE ORDER BY dc.nextActionDate ASC", 
                DunningCaseEntity.class)
                .setParameter("status", DunningCaseStatus.IN_PROGRESS)
                .getResultList();
            
            return entities.stream()
                .map(DunningCaseEntity::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            // Log error but return empty list for functional pattern
            return List.of();
        }
    }

    /**
     * Find dunning cases by status
     */
    public List<DunningCase> findByStatus(DunningCaseStatus status) {
        try {
            List<DunningCaseEntity> entities = entityManager.createQuery(
                "SELECT dc FROM DunningCaseEntity dc WHERE dc.status = :status ORDER BY dc.createdAt DESC", 
                DunningCaseEntity.class)
                .setParameter("status", status)
                .getResultList();
            
            return entities.stream()
                .map(DunningCaseEntity::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            // Log error but return empty list for functional pattern
            return List.of();
        }
    }
}
