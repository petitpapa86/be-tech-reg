package com.bcbs239.regtech.billing.domain.invoices;

import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.core.events.BaseEvent;

/**
 * Domain event published when an invoice is generated.
 * This event can be consumed by notification systems or other contexts.
 */
public class InvoiceGeneratedEvent extends BaseEvent {
    
    private final InvoiceId invoiceId;
    private final BillingAccountId billingAccountId;
    private final Money totalAmount;
    
    public InvoiceGeneratedEvent(InvoiceId invoiceId, BillingAccountId billingAccountId, 
                               Money totalAmount, String correlationId) {
        super(correlationId, "billing");
        this.invoiceId = invoiceId;
        this.billingAccountId = billingAccountId;
        this.totalAmount = totalAmount;
    }
    
    public InvoiceId getInvoiceId() {
        return invoiceId;
    }
    
    public BillingAccountId getBillingAccountId() {
        return billingAccountId;
    }
    
    public Money getTotalAmount() {
        return totalAmount;
    }
    
    @Override
    public String toString() {
        return String.format("InvoiceGeneratedEvent{invoiceId=%s, billingAccountId=%s, totalAmount=%s, correlationId=%s}", 
            invoiceId, billingAccountId, totalAmount, getCorrelationId());
    }
}