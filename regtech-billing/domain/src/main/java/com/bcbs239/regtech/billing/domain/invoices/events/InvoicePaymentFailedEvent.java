package com.bcbs239.regtech.billing.domain.invoices.events;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;

/**
 * Domain event published when an invoice payment fails
 */
public record InvoicePaymentFailedEvent(
    InvoiceId invoiceId,
    BillingAccountId billingAccountId,
    Money amount
) {
}