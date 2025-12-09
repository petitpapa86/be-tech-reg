package com.bcbs239.regtech.billing.domain.invoices;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;

/**
 * (Deprecated) Local invoice-related interface kept in invoices package for compatibility.
 * The canonical repository interface lives in `com.bcbs239.regtech.billing.domain.repositories.InvoiceRepository`.
 * This avoids package/path mismatch compilation errors.
 */
@Deprecated
public interface InvoiceRepository {

    Maybe<Invoice> findById(InvoiceId invoiceId);

    Maybe<Invoice> findByStripeInvoiceId(StripeInvoiceId stripeInvoiceId);

    Result<InvoiceId> save(Invoice invoice);
}
