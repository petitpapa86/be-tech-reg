package com.bcbs239.regtech.billing.infrastructure.database.repositories;

import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceStatus;
import com.bcbs239.regtech.billing.infrastructure.database.entities.InvoiceEntity;
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

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JPA repository for Invoice using closure-based functional patterns.
 * Provides functional operations for Invoice persistence.
 */
@Repository
@Transactional
public class JpaInvoiceRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public Function<InvoiceId, Maybe<Invoice>> invoiceFinder() {
        return id -> {
            try {
                InvoiceEntity entity = entityManager.find(InvoiceEntity.class, id.value());
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

    public Function<StripeInvoiceId, Maybe<Invoice>> invoiceByStripeIdFinder() {
        return stripeId -> {
            try {
                InvoiceEntity entity = entityManager.createQuery(
                    "SELECT i FROM InvoiceEntity i WHERE i.stripeInvoiceId = :stripeId", 
                    InvoiceEntity.class)
                    .setParameter("stripeId", stripeId.value())
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

    public Function<Void, List<Invoice>> overdueInvoicesFinder() {
        return unused -> {
            try {
                List<InvoiceEntity> entities = entityManager.createQuery(
                    "SELECT i FROM InvoiceEntity i WHERE i.status = :status AND i.dueDate < :currentDate", 
                    InvoiceEntity.class)
                    .setParameter("status", InvoiceStatus.PENDING)
                    .setParameter("currentDate", LocalDate.now())
                    .getResultList();
                
                return entities.stream()
                    .map(InvoiceEntity::toDomain)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                // Log error but return empty list for functional pattern
                return List.of();
            }
        };
    }

    public Function<BillingAccountId, List<Invoice>> invoicesByAccountFinder() {
        return billingAccountId -> {
            try {
                List<InvoiceEntity> entities = entityManager.createQuery(
                    "SELECT i FROM InvoiceEntity i WHERE i.billingAccountId = :billingAccountId ORDER BY i.createdAt DESC", 
                    InvoiceEntity.class)
                    .setParameter("billingAccountId", billingAccountId.value())
                    .getResultList();
                
                return entities.stream()
                    .map(InvoiceEntity::toDomain)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                // Log error but return empty list for functional pattern
                return List.of();
            }
        };
    }

    public Function<Invoice, Result<InvoiceId>> invoiceSaver() {
        return invoice -> {
            if (invoice.getId() != null) {
                return Result.failure(ErrorDetail.of("INVOICE_SAVE_FAILED",
                    "Cannot save invoice with existing ID", "invoice.save.existing.id"));
            }
            return transactionTemplate.execute(status -> {
                try {
                    InvoiceEntity entity = InvoiceEntity.fromDomain(invoice);
                    entityManager.persist(entity);
                    entityManager.flush(); // Ensure the entity is persisted
                    return Result.success(InvoiceId.fromString(entity.getId()).getValue().orElseThrow());
                } catch (Exception e) {
                    return Result.failure(ErrorDetail.of("INVOICE_SAVE_FAILED",
                        "Failed to save invoice: " + e.getMessage(), "invoice.save.failed"));
                }
            });
        };
    }

    public Function<Invoice, Result<InvoiceId>> invoiceUpdater() {
        return invoice -> {
            if (invoice.getId() == null) {
                return Result.failure(ErrorDetail.of("INVOICE_UPDATE_FAILED",
                    "Cannot update invoice without ID", "invoice.update.missing.id"));
            }
            return transactionTemplate.execute(status -> {
                try {
                    InvoiceEntity entity = InvoiceEntity.fromDomain(invoice);
                    entity = entityManager.merge(entity);
                    entityManager.flush(); // Ensure the entity is updated
                    return Result.success(InvoiceId.fromString(entity.getId()).getValue().orElseThrow());
                } catch (Exception e) {
                    return Result.failure(ErrorDetail.of("INVOICE_UPDATE_FAILED",
                        "Failed to update invoice: " + e.getMessage(), "invoice.update.failed"));
                }
            });
        };
    }

    /**
     * Find overdue invoices that don't have existing dunning cases
     */
    public List<Invoice> findOverdueInvoicesWithoutDunningCases() {
        try {
            List<InvoiceEntity> entities = entityManager.createQuery(
                "SELECT i FROM InvoiceEntity i WHERE i.status = :status AND i.dueDate < :currentDate " +
                "AND NOT EXISTS (SELECT dc FROM DunningCaseEntity dc WHERE dc.invoiceId = i.id)", 
                InvoiceEntity.class)
                .setParameter("status", InvoiceStatus.PENDING)
                .setParameter("currentDate", LocalDate.now())
                .getResultList();
            
            return entities.stream()
                .map(InvoiceEntity::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            // Log error but return empty list for functional pattern
            return List.of();
        }
    }
}
