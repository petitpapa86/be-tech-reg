package com.bcbs239.regtech.billing.infrastructure.database.repositories;

import com.bcbs239.regtech.billing.domain.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.infrastructure.database.entities.BillingAccountEntity;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.UserId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

/**
 * JPA repository for BillingAccount using closure-based functional patterns.
 * Provides functional operations for BillingAccount persistence.
 */
@Repository
@Transactional
public class JpaBillingAccountRepository {

    @PersistenceContext
    private EntityManager entityManager;

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
            try {
                BillingAccountEntity entity = BillingAccountEntity.fromDomain(billingAccount);
                
                if (billingAccount.getId() == null) {
                    entityManager.persist(entity);
                } else {
                    entity = entityManager.merge(entity);
                }
                
                entityManager.flush(); // Ensure the entity is persisted
                
                return Result.success(BillingAccountId.fromString(entity.getId()).getValue().get());
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_SAVE_FAILED",
                    "Failed to save billing account: " + e.getMessage(), "billing.account.save.failed"));
            }
        };
    }
}
