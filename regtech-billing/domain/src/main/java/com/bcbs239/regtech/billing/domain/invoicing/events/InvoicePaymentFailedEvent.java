package com.bcbs239.regtech.billing.domain.invoicing.events;

import com.bcbs239.regtech.billing.domain.invoicing.InvoiceId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.shared.valueobjects.Money;

/**
 * Domain event published when an invoice payment fails
 */
public record InvoicePaymentFailedEvent(
    InvoiceId invoiceId,
    BillingAccountId billingAccountId,
    Money amount
) {
}