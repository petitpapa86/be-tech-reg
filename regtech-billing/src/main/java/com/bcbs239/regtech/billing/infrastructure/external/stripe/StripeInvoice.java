package com.bcbs239.regtech.billing.infrastructure.external.stripe;

import com.bcbs239.regtech.billing.domain.valueobjects.StripeInvoiceId;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;

/**
 * DTO representing a Stripe invoice response
 */
public record StripeInvoice(
    StripeInvoiceId invoiceId,
    StripeCustomerId customerId,
    Money amount,
    String status,
    String hostedInvoiceUrl
) {
}