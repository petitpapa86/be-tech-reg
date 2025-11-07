package com.bcbs239.regtech.billing.domain.invoices;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.core.domain.events.BaseEvent;
import lombok.Getter;

/**
 * Domain event published when an invoice is generated.
 * This event can be consumed by notification systems or other contexts.
 */
@Getter
public class InvoiceGeneratedEvent extends BaseEvent {
    
    private final InvoiceId invoiceId;
    private final BillingAccountId billingAccountId;
    private final Money totalAmount;
    
    public InvoiceGeneratedEvent(InvoiceId invoiceId, BillingAccountId billingAccountId, 
                               Money totalAmount, String correlationId) {
        this.invoiceId = invoiceId;
        this.billingAccountId = billingAccountId;
        this.totalAmount = totalAmount;
    }

    @Override
    public String toString() {
        return String.format("InvoiceGeneratedEvent{invoiceId=%s, billingAccountId=%s, totalAmount=%s, correlationId=%s}", 
            invoiceId, billingAccountId, totalAmount, getCorrelationId());
    }
}

