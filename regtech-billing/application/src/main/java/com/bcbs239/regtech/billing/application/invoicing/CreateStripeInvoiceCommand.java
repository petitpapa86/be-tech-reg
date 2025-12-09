package com.bcbs239.regtech.billing.application.invoicing;



import com.bcbs239.regtech.core.domain.saga.SagaCommand;
import com.bcbs239.regtech.core.domain.saga.SagaId;

import java.time.Instant;
import java.util.Map;

public class CreateStripeInvoiceCommand extends SagaCommand {

    public CreateStripeInvoiceCommand(
            SagaId sagaId, 
            String customerId,
            String subscriptionId,
            String amount,
            String description) {
        super(sagaId, "CreateStripeInvoiceCommand", Map.of(
            "customerId", customerId,
            "subscriptionId", subscriptionId,
            "amount", amount,
            "description", description
        ), Instant.now());
    }

    public String getCustomerId() {
        return (String) payload().get("customerId");
    }

    public String getSubscriptionId() {
        return (String) payload().get("subscriptionId");
    }
    
    public String getAmount() {
        return (String) payload().get("amount");
    }
    
    public String getDescription() {
        return (String) payload().get("description");
    }
}

