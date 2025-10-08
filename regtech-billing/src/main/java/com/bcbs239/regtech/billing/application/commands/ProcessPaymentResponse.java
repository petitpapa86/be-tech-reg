package com.bcbs239.regtech.billing.application.commands;

import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.valueobjects.InvoiceId;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;

/**
 * Response for successful payment processing.
 * Contains the created billing account, subscription, and first invoice details.
 */
public record ProcessPaymentResponse(
    BillingAccountId billingAccountId,
    SubscriptionId subscriptionId,
    InvoiceId invoiceId,
    Money totalAmount,
    String correlationId
) {
    
    /**
     * Factory method to create ProcessPaymentResponse
     */
    public static ProcessPaymentResponse of(
            BillingAccountId billingAccountId,
            SubscriptionId subscriptionId,
            InvoiceId invoiceId,
            Money totalAmount,
            String correlationId) {
        return new ProcessPaymentResponse(
            billingAccountId,
            subscriptionId,
            invoiceId,
            totalAmount,
            correlationId
        );
    }
}