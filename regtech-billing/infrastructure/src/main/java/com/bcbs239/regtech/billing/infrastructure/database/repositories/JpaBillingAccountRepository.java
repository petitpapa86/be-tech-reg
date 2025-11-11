package com.bcbs239.regtech.billing.infrastructure.database.repositories;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.billing.domain.valueobjects.UserId;
import com.bcbs239.regtech.billing.infrastructure.database.entities.BillingAccountEntity;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.infrastructure.persistence.LoggingConfiguration;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * JPA repository implementation for BillingAccount with direct method signatures.
 * Provides clean, straightforward persistence operations for BillingAccount entities.
 */
@Repository
public class JpaBillingAccountRepository implements BillingAccountRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Maybe<BillingAccount> findById(BillingAccountId id) {
        try {
            BillingAccountEntity entity = entityManager.find(BillingAccountEntity.class, id.value());
            if (entity == null) {
                return Maybe.none();
            }
            return Maybe.some(entity.toDomain());
        } catch (Exception e) {
            LoggingConfiguration.logStructured("Error finding billing account by ID",
                Map.of("billingAccountId", id.value(), "eventType", "BILLING_ACCOUNT_FIND_ERROR"), e);
            return Maybe.none();
        }
    }

    @Override
    public Maybe<BillingAccount> findByUserId(UserId userId) {
        try {
            // Order by createdAt descending to get the most recent if duplicates exist
            List<BillingAccountEntity> entities = entityManager.createQuery(
                "SELECT ba FROM BillingAccountEntity ba WHERE ba.userId = :userId ORDER BY ba.createdAt DESC", 
                BillingAccountEntity.class)
                .setParameter("userId", userId.getValue())
                .getResultList();
            
            if (entities.isEmpty()) {
                return Maybe.none();
            }
            
            if (entities.size() > 1) {
                LoggingConfiguration.logStructured("Multiple billing accounts found for user - returning most recent",
                    Map.of("userId", userId.getValue(), 
                           "count", entities.size(),
                           "eventType", "DUPLICATE_BILLING_ACCOUNTS_WARNING"));
            }
            
            return Maybe.some(entities.get(0).toDomain());
        } catch (Exception e) {
            LoggingConfiguration.logStructured("Error finding billing account by user ID",
                Map.of("userId", userId.getValue(), "eventType", "BILLING_ACCOUNT_FIND_BY_USER_ERROR"), e);
            return Maybe.none();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<BillingAccountId> save(BillingAccount billingAccount) {
        if (billingAccount.getId() != null) {
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_SAVE_FAILED", ErrorType.BUSINESS_RULE_ERROR,
                "Cannot save billing account with existing ID", "billing.account.save.existing.id"));
        }
        
        try {
            BillingAccountEntity entity = BillingAccountEntity.fromDomain(billingAccount);
            entityManager.persist(entity);
            entityManager.flush();
            return Result.success(BillingAccountId.fromString(entity.getId()).getValue().orElseThrow());
        } catch (Exception e) {
            LoggingConfiguration.logStructured("Error saving billing account",
                Map.of("eventType", "BILLING_ACCOUNT_SAVE_ERROR"), e);
            throw new RuntimeException("Failed to save billing account: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<BillingAccountId> update(BillingAccount billingAccount) {
        if (billingAccount.getId() == null) {
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_UPDATE_FAILED", ErrorType.BUSINESS_RULE_ERROR,
                "Cannot update billing account without ID", "billing.account.update.missing.id"));
        }

        final int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            final int currentAttempt = attempt;
            try {
                BillingAccountEntity managed = entityManager.find(
                    BillingAccountEntity.class, billingAccount.getId().value(), LockModeType.OPTIMISTIC);

                if (managed == null) {
                    return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_NOT_FOUND", ErrorType.BUSINESS_RULE_ERROR,
                        "Billing account not found: " + billingAccount.getId().value(), "billing.account.not.found"));
                }

                // Update entity fields from domain object
                managed.setStripeCustomerId(billingAccount.getStripeCustomerId().isPresent() ? 
                    billingAccount.getStripeCustomerId().getValue().value() : null);
                managed.setStatus(billingAccount.getStatus());
                managed.setDefaultPaymentMethodId(billingAccount.getDefaultPaymentMethodId().isPresent() ? 
                    billingAccount.getDefaultPaymentMethodId().getValue().value() : null);

                if (billingAccount.getAccountBalance() != null) {
                    managed.setAccountBalanceAmount(billingAccount.getAccountBalance().amount());
                    managed.setAccountBalanceCurrency(billingAccount.getAccountBalance().currency().getCurrencyCode());
                } else {
                    managed.setAccountBalanceAmount(null);
                    managed.setAccountBalanceCurrency(null);
                }

                managed.setUpdatedAt(billingAccount.getUpdatedAt());
                entityManager.flush();

                return Result.success(BillingAccountId.fromString(managed.getId()).getValue().orElseThrow());
            } catch (OptimisticLockException ole) {
                LoggingConfiguration.logStructured("Optimistic lock on billing account update",
                    Map.of("billingAccountId", billingAccount.getId().value(), 
                           "eventType", "BILLING_ACCOUNT_UPDATE_CONFLICT", "attempt", currentAttempt), ole);
                
                // Retry on optimistic lock conflicts
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(50L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_UPDATE_INTERRUPTED", ErrorType.BUSINESS_RULE_ERROR,
                            "Update interrupted during retry", "billing.account.update.interrupted"));
                    }
                    continue;
                }
                return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_UPDATE_CONFLICT", ErrorType.BUSINESS_RULE_ERROR,
                    ole.getMessage(), "billing.account.update.conflict"));
            } catch (Exception e) {
                throw new RuntimeException("Failed to update billing account: " + e.getMessage(), e);
            }
        }
        
        return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_UPDATE_FAILED", ErrorType.BUSINESS_RULE_ERROR,
            "Failed to update billing account after " + maxAttempts + " attempts", "billing.account.update.max.attempts"));
    }

    // Backward compatibility methods for existing functional patterns
    @Deprecated
    public Function<BillingAccountId, Maybe<BillingAccount>> billingAccountFinder() {
        return this::findById;
    }

    @Deprecated
    public Function<UserId, Maybe<BillingAccount>> billingAccountByUserFinder() {
        return this::findByUserId;
    }

    @Deprecated
    public Function<BillingAccount, Result<BillingAccountId>> billingAccountSaver() {
        return this::save;
    }

    @Deprecated
    public Function<BillingAccount, Result<BillingAccountId>> billingAccountUpdater() {
        return this::update;
    }
}
