package com.bcbs239.regtech.billing.infrastructure.database.repositories;

import com.bcbs239.regtech.billing.domain.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.infrastructure.database.entities.BillingAccountEntity;
import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.UserId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.function.Function;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;

/**
 * JPA repository for BillingAccount using closure-based functional patterns.
 * Provides functional operations for BillingAccount persistence.
 */
@Repository
@Transactional
@SuppressWarnings("unused")
public class JpaBillingAccountRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public Function<BillingAccountId, Maybe<BillingAccount>> billingAccountFinder() {
        return id -> {
            try {
                BillingAccountEntity entity = entityManager.find(BillingAccountEntity.class, id.value());
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

    public Function<UserId, Maybe<BillingAccount>> billingAccountByUserFinder() {
        return userId -> {
            try {
                BillingAccountEntity entity = entityManager.createQuery(
                    "SELECT ba FROM BillingAccountEntity ba WHERE ba.userId = :userId", 
                    BillingAccountEntity.class)
                    .setParameter("userId", userId.getValue())
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

    public Function<BillingAccount, Result<BillingAccountId>> billingAccountSaver() {
        return billingAccount -> {
            if (billingAccount.getId() != null) {
                return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_SAVE_FAILED",
                    "Cannot save billing account with existing ID", "billing.account.save.existing.id"));
            }
            // Execute persistence inside a new transaction to ensure EntityManager has an active transaction
            return transactionTemplate.execute(status -> {
                try {
                    BillingAccountEntity entity = BillingAccountEntity.fromDomain(billingAccount);
                    entityManager.persist(entity);
                    entityManager.flush(); // Ensure the entity is persisted
                    return Result.success(BillingAccountId.fromString(entity.getId()).getValue().orElseThrow());
                } catch (Exception e) {
                    LoggingConfiguration.logStructured("Error saving billing account",
                        Map.of("eventType", "BILLING_ACCOUNT_SAVE_ERROR"), e);
                    // Propagate as runtime to ensure caller sees the failure and transaction is rolled back
                    throw new RuntimeException("Failed to save billing account: " + e.getMessage(), e);
                }
            });
        };
    }

    public Function<BillingAccount, Result<BillingAccountId>> billingAccountUpdater() {
        return billingAccount -> {
            if (billingAccount.getId() == null) {
                return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_UPDATE_FAILED",
                    "Cannot update billing account without ID", "billing.account.update.missing.id"));
            }

            final int maxAttempts = 3;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                final int currentAttempt = attempt;
                Result<BillingAccountId> result = transactionTemplate.execute(status -> {
                    try {
                        // Load the managed entity with optimistic lock to ensure we operate on the current version
                        BillingAccountEntity managed = entityManager.find(
                            BillingAccountEntity.class, billingAccount.getId().value(), LockModeType.OPTIMISTIC);

                        if (managed == null) {
                            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_NOT_FOUND",
                                "Billing account not found: " + billingAccount.getId().value(), "billing.account.not.found"));
                        }

                        // Copy updatable fields from domain into the managed entity
                        managed.setStripeCustomerId(billingAccount.getStripeCustomerId().isPresent() ? billingAccount.getStripeCustomerId().getValue().value() : null);
                        managed.setStatus(billingAccount.getStatus());
                        managed.setDefaultPaymentMethodId(billingAccount.getDefaultPaymentMethodId().isPresent() ? billingAccount.getDefaultPaymentMethodId().getValue().value() : null);

                        if (billingAccount.getAccountBalance() != null) {
                            managed.setAccountBalanceAmount(billingAccount.getAccountBalance().amount());
                            managed.setAccountBalanceCurrency(billingAccount.getAccountBalance().currency().getCurrencyCode());
                        } else {
                            managed.setAccountBalanceAmount(null);
                            managed.setAccountBalanceCurrency(null);
                        }

                        // Preserve createdAt; update updatedAt from domain
                        managed.setUpdatedAt(billingAccount.getUpdatedAt());

                        // Flush to detect optimistic lock conflicts now
                        entityManager.flush();

                        return Result.success(BillingAccountId.fromString(managed.getId()).getValue().orElseThrow());
                    } catch (OptimisticLockException ole) {
                        // Return a failure result marking conflict so outer loop can retry
                        LoggingConfiguration.logStructured("Optimistic lock on billing account update",
                            Map.of("billingAccountId", billingAccount.getId().value(), "eventType", "BILLING_ACCOUNT_UPDATE_CONFLICT", "attempt", currentAttempt), ole);
                        return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_UPDATE_CONFLICT", ole.getMessage(), "billing.account.update.conflict"));
                    } catch (Exception e) {
                        // Propagate to ensure transaction rollback
                        throw new RuntimeException("Failed to update billing account: " + e.getMessage(), e);
                    }
                });

                if (result == null) {
                    // transactionTemplate can return null if execution fails; treat as generic failure
                    return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_UPDATE_FAILED", "Transaction failed during update", "billing.account.update.transaction.failed"));
                }

                if (result.isSuccess()) {
                    return result;
                }

                // If conflict and we have attempts left, sleep a bit and retry
                boolean isConflict = result.getError().map(err -> "BILLING_ACCOUNT_UPDATE_CONFLICT".equals(err.getCode())).orElse(false);
                if (isConflict && attempt < maxAttempts) {
                    try {
                        Thread.sleep(50L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue; // retry
                }

                // otherwise return the failure
                return result;
            }

            // should not reach here, but return generic failure
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_UPDATE_FAILED", "Failed to update billing account after retries", "billing.account.update.failed"));
        };
    }
}
