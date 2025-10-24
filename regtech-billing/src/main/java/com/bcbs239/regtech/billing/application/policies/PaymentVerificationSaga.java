package com.bcbs239.regtech.billing.application.policies;

import com.bcbs239.regtech.billing.domain.billing.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.domain.events.StripeCustomerCreatedEvent;
import com.bcbs239.regtech.billing.domain.events.StripeSubscriptionCreatedEvent;
import com.bcbs239.regtech.billing.domain.events.StripeInvoiceCreatedEvent;
import com.bcbs239.regtech.billing.domain.events.StripePaymentSucceededEvent;
import com.bcbs239.regtech.billing.domain.events.StripePaymentFailedEvent;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.core.saga.AbstractSaga;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaStatus;

import java.time.Instant;

public class PaymentVerificationSaga extends AbstractSaga<PaymentVerificationSagaData> {

    public PaymentVerificationSaga(SagaId id, PaymentVerificationSagaData data) {
        super(id, "PaymentVerificationSaga", data);
        registerHandlers();
    }

    private void registerHandlers() {
        onEvent(StripeCustomerCreatedEvent.class, this::handleStripeCustomerCreated);
        onEvent(StripeSubscriptionCreatedEvent.class, this::handleStripeSubscriptionCreated);
        onEvent(StripeInvoiceCreatedEvent.class, this::handleStripeInvoiceCreated);
        onEvent(StripePaymentSucceededEvent.class, this::handleStripePaymentSucceeded);
        onEvent(StripePaymentFailedEvent.class, this::handleStripePaymentFailed);
    }

    private void handleStripeCustomerCreated(StripeCustomerCreatedEvent event) {
        data.setStripeCustomerId(event.getStripeCustomerId());
        dispatchCommand(new CreateStripeSubscriptionCommand(
            getId(), 
            data.getStripeCustomerId(), 
            SubscriptionTier.STARTER,
            data.getUserId().value()
        ));
        updateStatus();
    }

    private void handleStripeSubscriptionCreated(StripeSubscriptionCreatedEvent event) {
        data.setStripeSubscriptionId(event.getStripeSubscriptionId());
        data.setStripeInvoiceId(event.getStripeInvoiceId());
        data.setSubscriptionId(event.getSubscriptionId());
        dispatchCommand(new CreateStripeInvoiceCommand(
            getId(), 
            data.getStripeInvoiceId(),
            data.getBillingAccountId().getValue(),
            data.getSubscriptionId().value()
        ));
        updateStatus();
    }

    private void handleStripeInvoiceCreated(StripeInvoiceCreatedEvent event) {
        data.setStripeInvoiceId(event.getStripeInvoiceId());
        updateStatus();
    }

    private void handleStripePaymentSucceeded(StripePaymentSucceededEvent event) {
        data.setStripePaymentIntentId(event.getStripePaymentIntentId());
        dispatchCommand(new FinalizeBillingAccountCommand(
            getId(),
            data.getStripeCustomerId(),
            data.getStripeSubscriptionId(),
            data.getStripeInvoiceId(),
            data.getBillingAccountId().getValue(),
            data.getCorrelationId()
        ));
        updateStatus();
    }

    private void handleStripePaymentFailed(StripePaymentFailedEvent event) {
        data.setFailureReason(event.getFailureReason());
        updateStatus();
    }

    @Override
    protected void updateStatus() {
        if (data.getFailureReason() != null) {
            setStatus(SagaStatus.FAILED);
            setCompletedAt(Instant.now());
        } else if (data.getUserId() != null &&
                   data.getStripeCustomerId() != null &&
                   data.getStripeSubscriptionId() != null &&
                   data.getStripeInvoiceId() != null &&
                   data.getStripePaymentIntentId() != null) {
            setStatus(SagaStatus.COMPLETED);
            setCompletedAt(Instant.now());
        }
    }
}