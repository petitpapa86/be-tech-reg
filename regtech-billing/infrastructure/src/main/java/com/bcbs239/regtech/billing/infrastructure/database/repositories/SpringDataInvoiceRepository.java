package com.bcbs239.regtech.billing.infrastructure.database.repositories;

import com.bcbs239.regtech.billing.infrastructure.database.entities.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for InvoiceEntity.
 * Provides automatic CRUD operations and transaction management.
 */
@Repository
public interface SpringDataInvoiceRepository extends JpaRepository<InvoiceEntity, String> {
    
    /**
     * Find invoice by Stripe invoice ID
     */
    Optional<InvoiceEntity> findByStripeInvoiceId(String stripeInvoiceId);
}
