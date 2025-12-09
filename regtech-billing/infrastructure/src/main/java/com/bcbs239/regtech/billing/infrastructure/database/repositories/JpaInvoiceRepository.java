package com.bcbs239.regtech.billing.infrastructure.database.repositories;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceStatus;
import com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId;
import com.bcbs239.regtech.billing.domain.repositories.InvoiceRepository;
import com.bcbs239.regtech.billing.infrastructure.database.entities.InvoiceEntity;
import com.bcbs239.regtech.billing.infrastructure.database.entities.InvoiceLineItemEntity;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA repository for Invoice using Spring Data JPA.
 * Provides clean transaction management without EntityManager headaches.
 */
@Repository
public class JpaInvoiceRepository implements InvoiceRepository {

    private final SpringDataInvoiceRepository springDataRepository;

    @Autowired
    public JpaInvoiceRepository(SpringDataInvoiceRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<InvoiceId> save(Invoice invoice) {
        try {
            InvoiceEntity entity = InvoiceEntity.fromDomain(invoice);
            
            if (invoice.getId() == null && entity.getLineItems() != null && !entity.getLineItems().isEmpty()) {
                List<InvoiceLineItemEntity> lineItems = new ArrayList<>(entity.getLineItems());
                entity.getLineItems().clear();
                entity = springDataRepository.saveAndFlush(entity);
                
                for (InvoiceLineItemEntity lineItem : lineItems) {
                    lineItem.setInvoiceId(entity.getId());
                }
                
                entity.getLineItems().addAll(lineItems);
            }
            
            entity = springDataRepository.saveAndFlush(entity);
            return Result.success(InvoiceId.fromString(entity.getId()).getValue().orElseThrow());

        } catch (Exception e) {
            return Result.failure(ErrorDetail.of(
                "INVOICE_SAVE_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to save invoice: " + e.getMessage(),
                "invoice.save.failed"
            ));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Maybe<Invoice> findById(InvoiceId id) {
        return springDataRepository.findById(id.getValue())
            .map(entity -> Maybe.some(entity.toDomain()))
            .orElseGet(Maybe::none);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Maybe<Invoice> findByStripeInvoiceId(StripeInvoiceId stripeInvoiceId) {
        return springDataRepository.findByStripeInvoiceId(stripeInvoiceId.getValue())
            .map(entity -> Maybe.some(entity.toDomain()))
            .orElseGet(Maybe::none);
    }

    // Additional methods not in interface
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<Invoice> findByBillingAccountId(BillingAccountId accountId) {
        return springDataRepository.findAll().stream()
            .filter(entity -> entity.getBillingAccountId().equals(accountId.getValue()))
            .map(InvoiceEntity::toDomain)
            .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<Invoice> findByStatus(InvoiceStatus status) {
        return springDataRepository.findAll().stream()
            .filter(entity -> entity.getStatus() == status)
            .map(InvoiceEntity::toDomain)
            .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteById(InvoiceId id) {
        springDataRepository.deleteById(id.getValue());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<Invoice> findAll() {
        return springDataRepository.findAll().stream()
            .map(InvoiceEntity::toDomain)
            .toList();
    }

    // Legacy methods for compatibility
    public java.util.function.Function<InvoiceId, Maybe<Invoice>> invoiceFinder() {
        return this::findById;
    }

    public List<Invoice> findOverdueInvoicesWithoutDunningCases() {
        return springDataRepository.findAll().stream()
            .map(InvoiceEntity::toDomain)
            .toList();
    }
}
