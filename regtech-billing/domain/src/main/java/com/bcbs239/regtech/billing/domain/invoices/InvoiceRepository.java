package com.bcbs239.regtech.billing.domain.repositories;

import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;

/**
 * Domain repository interface for Invoice aggregate operations.
 * Clean interface with direct method signatures.
 */
public interface InvoiceRepository {
    
    /**
     * Find an invoice by ID
     */
    Maybe<Invoice> findById(InvoiceId invoiceId);
    
    /**
     * Find an invoice by Stripe invoice ID
     */
    Maybe<Invoice> findByStripeInvoiceId(StripeInvoiceId stripeInvoiceId);
    
    /**
     * Save an invoice
     */
    Result<InvoiceId> save(Invoice invoice);
}

