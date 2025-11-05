package com.bcbs239.regtech.billing.domain.invoices.events;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.shared.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.core.events.BaseEvent;

/**
 * Domain event published when an invoice is generated.
 */
public class InvoiceGeneratedEvent extends BaseEvent {

    private final InvoiceId invoiceId;
    private final BillingAccountId billingAccountId;
    private final BillingPeriod billingPeriod;
    private final Money totalAmount;

    public InvoiceGeneratedEvent(InvoiceId invoiceId,
                               BillingAccountId billingAccountId,
                               BillingPeriod billingPeriod,
                               Money totalAmount,
                               String correlationId) {
        super(correlationId, "billing");
        this.invoiceId = invoiceId;
        this.billingAccountId = billingAccountId;
        this.billingPeriod = billingPeriod;
        this.totalAmount = totalAmount;
    }

    public InvoiceId getInvoiceId() {
        return invoiceId;
    }

    public BillingAccountId getBillingAccountId() {
        return billingAccountId;
    }

    public BillingPeriod getBillingPeriod() {
        return billingPeriod;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    @Override
    public String toString() {
        return String.format("InvoiceGeneratedEvent{invoiceId=%s, billingAccountId=%s, billingPeriod=%s, totalAmount=%s, correlationId=%s}",
            invoiceId, billingAccountId, billingPeriod, totalAmount, getCorrelationId());
    }
}