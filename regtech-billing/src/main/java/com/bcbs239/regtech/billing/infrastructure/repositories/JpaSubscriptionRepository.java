package com.bcbs239.regtech.billing.infrastructure.repositories;

import com.bcbs239.regtech.billing.domain.aggregates.Subscription;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.SubscriptionId;
import com.bcbs239.regtech.billing.domain.valueobjects.SubscriptionStatus;
import com.bcbs239.regtech.billing.infrastructure.entities.SubscriptionEntity;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JPA repository for Subscription using closure-based functional patterns.
 * Provides functional operations for Subscription persistence.
 */
@Repository
@Transactional
public class JpaSubscriptionRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Function<SubscriptionId, Maybe<Subscription>> subscriptionFinder() {
        return subscriptionId -> {
            try {
                SubscriptionEntity entity = entityManager.createQuery(
                    "SELECT s FROM SubscriptionEntity s WHERE s.id = :subscriptionId", 
                    SubscriptionEntity.class)
                    .setParameter("subscriptionId", subscriptionId.value())
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

    public Function<BillingAccountId, Maybe<Subscription>> activeSubscriptionFinder() {
        return billingAccountId -> {
            try {
                SubscriptionEntity entity = entityManager.createQuery(
                    "SELECT s FROM SubscriptionEntity s WHERE s.billingAccountId = :billingAccountId AND s.status = :status", 
                    SubscriptionEntity.class)
                    .setParameter("billingAccountId", billingAccountId.value())
                    .setParameter("status", SubscriptionStatus.ACTIVE)
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

    public Function<Subscription, Result<SubscriptionId>> subscriptionSaver() {
        return subscription -> {
            try {
                SubscriptionEntity entity = SubscriptionEntity.fromDomain(subscription);
                
                if (subscription.getId() == null) {
                    entityManager.persist(entity);
                } else {
                    entity = entityManager.merge(entity);
                }
                
                entityManager.flush(); // Ensure the entity is persisted
                
                return Result.success(SubscriptionId.fromString(entity.getId()).getValue().get());
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("SUBSCRIPTION_SAVE_FAILED",
                    "Failed to save subscription: " + e.getMessage()));
            }
        };
    }

    public Function<SubscriptionStatus, List<Subscription>> subscriptionsByStatusFinder() {
        return status -> {
            try {
                List<SubscriptionEntity> entities = entityManager.createQuery(
                    "SELECT s FROM SubscriptionEntity s WHERE s.status = :status", 
                    SubscriptionEntity.class)
                    .setParameter("status", status)
                    .getResultList();
                
                return entities.stream()
                    .map(SubscriptionEntity::toDomain)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                // Log error but return empty list for functional pattern
                return List.of();
            }
        };
    }

    /**
     * Find subscriptions by multiple statuses (for saga orchestration)
     */
    public List<Subscription> findByStatusIn(List<SubscriptionStatus> statuses) {
        try {
            List<SubscriptionEntity> entities = entityManager.createQuery(
                "SELECT s FROM SubscriptionEntity s WHERE s.status IN :statuses", 
                SubscriptionEntity.class)
                .setParameter("statuses", statuses)
                .getResultList();
            
            return entities.stream()
                .map(SubscriptionEntity::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            // Log error but return empty list for functional pattern
            return List.of();
        }
    }
}