package com.bcbs239.regtech.billing.application.invoicing;

import com.bcbs239.regtech.core.domain.saga.SagaCommand;
import com.bcbs239.regtech.core.domain.saga.SagaId;

import java.time.Instant;
import java.util.Map;

public class CreateStripeInvoiceCommand extends SagaCommand {

    public CreateStripeInvoiceCommand(SagaId sagaId, String stripeInvoiceId, String billingAccountId, String subscriptionId) {
        super(sagaId, "CreateStripeInvoiceCommand", Map.of(
            "stripeInvoiceId", stripeInvoiceId,
            "billingAccountId", billingAccountId,
            "subscriptionId", subscriptionId
        ), Instant.now());
    }

    public String getStripeInvoiceId() {
        return (String) payload().get("stripeInvoiceId");
    }

    public String getBillingAccountId() {
        return (String) payload().get("billingAccountId");
    }

    public String getSubscriptionId() {
        return (String) payload().get("subscriptionId");
    }
    
    public String getCustomerId() {
        return (String) payload().get("customerId");
    }
    
    public String getAmount() {
        return (String) payload().get("amount");
    }
    
    public String getDescription() {
        return (String) payload().get("description");
    }
}