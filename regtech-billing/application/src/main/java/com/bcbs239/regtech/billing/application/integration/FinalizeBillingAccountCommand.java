package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.core.saga.SagaCommand;
import com.bcbs239.regtech.core.saga.SagaId;

import java.time.Instant;
import java.util.Map;

public class FinalizeBillingAccountCommand extends SagaCommand {

    public FinalizeBillingAccountCommand(SagaId sagaId, String stripeCustomerId, String stripeSubscriptionId, String stripeInvoiceId, String billingAccountId, String correlationId) {
        super(sagaId, "FinalizeBillingAccountCommand", Map.of(
            "stripeCustomerId", stripeCustomerId,
            "stripeSubscriptionId", stripeSubscriptionId,
            "stripeInvoiceId", stripeInvoiceId,
            "billingAccountId", billingAccountId,
            "correlationId", correlationId
        ), Instant.now());
    }

    public String getStripeCustomerId() {
        return (String) payload().get("stripeCustomerId");
    }

    public String getStripeSubscriptionId() {
        return (String) payload().get("stripeSubscriptionId");
    }

    public String getStripeInvoiceId() {
        return (String) payload().get("stripeInvoiceId");
    }

    public String getBillingAccountId() {
        return (String) payload().get("billingAccountId");
    }

    public String getCorrelationId() {
        return (String) payload().get("correlationId");
    }
}