package com.bcbs239.regtech.billing.infrastructure.database.repositories;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.subscriptions.*;
import com.bcbs239.regtech.billing.infrastructure.database.entities.SubscriptionEntity;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.infrastructure.persistence.LoggingConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JPA repository implementation for Subscription with direct method signatures.
 * Provides clean, straightforward persistence operations for Subscription entities.
 */
@Repository
public class JpaSubscriptionRepository implements com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Maybe<Subscription> findById(SubscriptionId subscriptionId) {
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
            return Maybe.none();
        }
    }

    @Override
    public Maybe<Subscription> findActiveByBillingAccountId(BillingAccountId billingAccountId) {
        try {
            // Find subscriptions that are either PENDING or ACTIVE (for backward compatibility during transition)
            SubscriptionEntity entity = entityManager.createQuery(
                "SELECT s FROM SubscriptionEntity s WHERE s.billingAccountId = :billingAccountId AND s.status IN (:statuses) ORDER BY s.createdAt DESC", 
                SubscriptionEntity.class)
                .setParameter("billingAccountId", billingAccountId.value())
                .setParameter("statuses", java.util.List.of(SubscriptionStatus.PENDING, SubscriptionStatus.ACTIVE))
                .setMaxResults(1)
                .getSingleResult();
            return Maybe.some(entity.toDomain());
        } catch (NoResultException e) {
            return Maybe.none();
        } catch (Exception e) {
            return Maybe.none();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<SubscriptionId> save(Subscription subscription) {
        if (subscription.getId() != null) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_SAVE_FAILED", ErrorType.BUSINESS_RULE_ERROR,
                "Cannot save subscription with existing ID", "subscription.save.existing.id"));
        }
        
        try {
            SubscriptionEntity entity = SubscriptionEntity.fromDomain(subscription);
            entityManager.persist(entity);
            entityManager.flush();
            return Result.success(SubscriptionId.fromString(entity.getId()).getValue().orElseThrow());
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_SAVE_FAILED", ErrorType.BUSINESS_RULE_ERROR,
                "Failed to save subscription: " + e.getMessage(), "subscription.save.failed"));
        }
    }

    // Additional methods for specific use cases
    public Maybe<Subscription> findByBillingAccountAndTier(BillingAccountId billingAccountId, SubscriptionTier tier) {
        try {
            SubscriptionEntity entity = entityManager.createQuery(
                "SELECT s FROM SubscriptionEntity s WHERE s.billingAccountId = :billingAccountId AND s.tier = :tier AND s.stripeSubscriptionId IS NULL", 
                SubscriptionEntity.class)
                .setParameter("billingAccountId", billingAccountId.value())
                .setParameter("tier", tier)
                .getSingleResult();
            return Maybe.some(entity.toDomain());
        } catch (NoResultException e) {
            return Maybe.none();
        } catch (Exception e) {
            return Maybe.none();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<SubscriptionId> update(Subscription subscription) {
        if (subscription.getId() == null) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_UPDATE_FAILED", ErrorType.BUSINESS_RULE_ERROR,
                "Cannot update subscription without ID", "subscription.update.missing.id"));
        }
        
        final int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            final int currentAttempt = attempt;
            try {
                // Fetch the latest version from database instead of merging stale entity
                SubscriptionEntity managed = entityManager.find(
                    SubscriptionEntity.class, subscription.getId().value(), LockModeType.OPTIMISTIC);
                
                if (managed == null) {
                    return Result.failure(ErrorDetail.of("SUBSCRIPTION_NOT_FOUND", ErrorType.BUSINESS_RULE_ERROR,
                        "Subscription not found: " + subscription.getId().value(), "subscription.not.found"));
                }
                
                // Update managed entity with values from domain object
                managed.setStripeSubscriptionId(subscription.getStripeSubscriptionId() != null ? 
                    subscription.getStripeSubscriptionId().value() : null);
                managed.setStatus(subscription.getStatus());
                managed.setTier(subscription.getTier());
                managed.setStartDate(subscription.getStartDate());
                managed.setEndDate(subscription.getEndDate());
                managed.setUpdatedAt(subscription.getUpdatedAt());
                
                entityManager.flush();
                
                return Result.success(SubscriptionId.fromString(managed.getId()).getValue().orElseThrow());
            } catch (OptimisticLockException ole) {
                LoggingConfiguration.logStructured("Optimistic lock on subscription update",
                    Map.of("subscriptionId", subscription.getId().value(), 
                           "eventType", "SUBSCRIPTION_UPDATE_CONFLICT", "attempt", currentAttempt), ole);
                
                // Retry on optimistic lock conflicts
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(50L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return Result.failure(ErrorDetail.of("SUBSCRIPTION_UPDATE_INTERRUPTED", ErrorType.BUSINESS_RULE_ERROR,
                            "Update interrupted during retry", "subscription.update.interrupted"));
                    }
                    // Clear the entity manager to avoid stale state
                    entityManager.clear();
                    continue;
                }
                return Result.failure(ErrorDetail.of("SUBSCRIPTION_UPDATE_CONFLICT", ErrorType.BUSINESS_RULE_ERROR,
                    ole.getMessage(), "subscription.update.conflict"));
            } catch (Exception e) {
                LoggingConfiguration.logStructured("Error updating subscription",
                    Map.of("subscriptionId", subscription.getId().value(), "eventType", "SUBSCRIPTION_UPDATE_ERROR"), e);
                return Result.failure(ErrorDetail.of("SUBSCRIPTION_UPDATE_FAILED", ErrorType.BUSINESS_RULE_ERROR,
                    "Failed to update subscription: " + e.getMessage(), "subscription.update.failed"));
            }
        }
        
        return Result.failure(ErrorDetail.of("SUBSCRIPTION_UPDATE_FAILED", ErrorType.BUSINESS_RULE_ERROR,
            "Failed to update subscription after " + maxAttempts + " attempts", "subscription.update.max.attempts"));
    }

    public List<Subscription> findByStatus(SubscriptionStatus status) {
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
            return List.of();
        }
    }

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
            return List.of();
        }
    }

    // Backward compatibility methods for existing functional patterns
    @Deprecated
    public Function<SubscriptionId, Maybe<Subscription>> subscriptionFinder() {
        return this::findById;
    }

    @Deprecated
    public Function<BillingAccountId, Maybe<Subscription>> activeSubscriptionFinder() {
        return this::findActiveByBillingAccountId;
    }

    @Deprecated
    public Function<BillingAccountId, Function<SubscriptionTier, Maybe<Subscription>>> subscriptionByBillingAccountAndTierFinder() {
        return billingAccountId -> tier -> findByBillingAccountAndTier(billingAccountId, tier);
    }

    @Deprecated
    public Function<Subscription, Result<SubscriptionId>> subscriptionSaver() {
        return this::save;
    }

    @Deprecated
    public Function<Subscription, Result<SubscriptionId>> subscriptionUpdater() {
        return this::update;
    }

    @Deprecated
    public Function<SubscriptionStatus, List<Subscription>> subscriptionsByStatusFinder() {
        return this::findByStatus;
    }
}

