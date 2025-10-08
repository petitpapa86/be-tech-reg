package com.bcbs239.regtech.billing.infrastructure.database.repositories;

import com.bcbs239.regtech.billing.domain.aggregates.Invoice;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.InvoiceId;
import com.bcbs239.regtech.billing.domain.valueobjects.InvoiceStatus;
import com.bcbs239.regtech.billing.infrastructure.entities.InvoiceEntity;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
            try {
                InvoiceEntity entity = InvoiceEntity.fromDomain(invoice);
                
                if (invoice.getId() == null) {
                    entityManager.persist(entity);
                } else {
                    entity = entityManager.merge(entity);
                }
                
                entityManager.flush(); // Ensure the entity is persisted
                
                return Result.success(InvoiceId.fromString(entity.getId()).getValue().get());
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("INVOICE_SAVE_FAILED",
                    "Failed to save invoice: " + e.getMessage()));
            }
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