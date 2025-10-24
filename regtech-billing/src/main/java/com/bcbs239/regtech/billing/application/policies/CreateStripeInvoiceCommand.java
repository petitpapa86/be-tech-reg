package com.bcbs239.regtech.billing.application.policies;

import com.bcbs239.regtech.core.saga.SagaCommand;
import com.bcbs239.regtech.core.saga.SagaId;

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
}