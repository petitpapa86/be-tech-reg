package com.bcbs239.regtech.billing.application.payments;

import com.bcbs239.regtech.core.domain.saga.AbstractSaga;

import com.bcbs239.regtech.billing.application.integration.FinalizeBillingAccountCommand;
import com.bcbs239.regtech.billing.application.invoicing.CreateStripeInvoiceCommand;
import com.bcbs239.regtech.billing.application.policies.createstripecustomer.CreateStripeCustomerCommand;
import com.bcbs239.regtech.billing.application.subscriptions.CreateStripeSubscriptionCommand;
import com.bcbs239.regtech.billing.domain.invoices.events.StripeInvoiceCreatedEvent;
import com.bcbs239.regtech.billing.domain.payments.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.domain.payments.events.StripeCustomerCreatedEvent;
import com.bcbs239.regtech.billing.domain.payments.events.StripeCustomerCreationFailedEvent;
import com.bcbs239.regtech.billing.domain.payments.events.StripePaymentFailedEvent;
import com.bcbs239.regtech.billing.domain.payments.events.StripePaymentSucceededEvent;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.subscriptions.events.StripeSubscriptionCreatedEvent;
import com.bcbs239.regtech.billing.domain.subscriptions.events.StripeSubscriptionWebhookReceivedEvent;
import com.bcbs239.regtech.core.domain.saga.SagaStatus;
import com.bcbs239.regtech.core.infrastructure.saga.SagaStartedEvent;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.TimeoutScheduler;

import java.time.Instant;

public class PaymentVerificationSaga extends AbstractSaga<PaymentVerificationSagaData> {
    public PaymentVerificationSaga(SagaId id, PaymentVerificationSagaData data, TimeoutScheduler timeoutScheduler, ILogger logger) {
        super(id, "PaymentVerificationSaga", data, timeoutScheduler, logger);
        registerHandlers();
    }

    private void registerHandlers() {
        onEvent(SagaStartedEvent.class, this::handleSagaStarted);
        onEvent(StripeCustomerCreatedEvent.class, this::handleStripeCustomerCreated);
        onEvent(StripeCustomerCreationFailedEvent.class, this::handleStripeCustomerCreationFailed);
        onEvent(StripeSubscriptionCreatedEvent.class, this::handleStripeSubscriptionCreated);
        onEvent(StripeSubscriptionWebhookReceivedEvent.class, this::handleStripeSubscriptionWebhookReceived);
        onEvent(StripeInvoiceCreatedEvent.class, this::handleStripeInvoiceCreated);
        onEvent(StripePaymentSucceededEvent.class, this::handleStripePaymentSucceeded);
        onEvent(StripePaymentFailedEvent.class, this::handleStripePaymentFailed);
    }

    private void handleSagaStarted(SagaStartedEvent event) {
        // Dispatch CreateStripeCustomerCommand to start the process
        CreateStripeCustomerCommand createCommand = new CreateStripeCustomerCommand(
                event.sagaId(),
                data.getUserId(),
                data.getUserEmail(),
                data.getUserName(),
                data.getPaymentMethodId()
        );
        dispatchCommand(createCommand);


        // Schedule payment timeout using SLA
        timeoutScheduler.schedule(
            getId().id() + "-payment-timeout",
            PaymentVerificationSagaData.PAYMENT_TIMEOUT_SLA.toMillis(),
            this::handlePaymentTimeout
        );
    }

    private void handleStripeCustomerCreated(StripeCustomerCreatedEvent event) {
        data.setStripeCustomerId(event.getStripeCustomerId());
        data.setBillingAccountId(data.getUserId());
        dispatchCommand(new CreateStripeSubscriptionCommand(
            getId(),
            data.getStripeCustomerId(),
            SubscriptionTier.STARTER,
            data.getUserId(),
            data.getPaymentMethodId()
        ));


        updateStatus();
    }

    private void handleStripeSubscriptionCreated(StripeSubscriptionCreatedEvent event) {
        data.setStripeSubscriptionId(event.getStripeSubscriptionId());
        //need to review this part
        data.setStripeInvoiceId(event.getStripeInvoiceId());
        
        if (event.getSubscriptionId().isPresent()) {
            // Subscription already exists (from command handler)
            data.setSubscriptionId(event.getSubscriptionId().get().value());
            dispatchCommand(new CreateStripeInvoiceCommand(
                getId(), 
                data.getStripeInvoiceId(),
                data.getBillingAccountId(),
                data.getSubscriptionId()
            ));
        } else {
            // Subscription doesn't exist yet (from webhook) - create it first
            dispatchCommand(new CreateStripeSubscriptionCommand(
                getId(),
                data.getStripeCustomerId(),
                SubscriptionTier.STARTER,
                data.getUserId(),
                data.getPaymentMethodId()
            ));
        }
        updateStatus();
    }

    private void handleStripeSubscriptionWebhookReceived(StripeSubscriptionWebhookReceivedEvent event) {
        data.setStripeSubscriptionId(event.getStripeSubscriptionId());
        data.setStripeInvoiceId(event.getStripeInvoiceId());
        
        // Webhook indicates subscription was created in Stripe - ensure we have local subscription
        if (data.getSubscriptionId() == null) {
            dispatchCommand(new CreateStripeSubscriptionCommand(
                getId(),
                data.getStripeCustomerId(),
                SubscriptionTier.STARTER,
                data.getUserId(),
                data.getPaymentMethodId()
            ));
        } else {
            // We already have a subscription, create invoice
            dispatchCommand(new CreateStripeInvoiceCommand(
                getId(), 
                data.getStripeInvoiceId(),
                data.getBillingAccountId(),
                data.getSubscriptionId()
            ));
        }
        updateStatus();
    }

    private void handleStripeInvoiceCreated(StripeInvoiceCreatedEvent event) {
        data.setStripeInvoiceId(event.getStripeInvoiceId());
        updateStatus();
    }

    private void handleStripePaymentSucceeded(StripePaymentSucceededEvent event) {
        data.setStripePaymentIntentId(event.getStripePaymentIntentId());
        // Cancel the payment timeout since payment succeeded
        timeoutScheduler.cancel(getId().id() + "-payment-timeout");
        dispatchCommand(new FinalizeBillingAccountCommand(
            getId(),
            data.getStripeCustomerId(),
            data.getStripeSubscriptionId(),
            data.getStripeInvoiceId(),
            data.getBillingAccountId(),
            data.getCorrelationId()
        ));
        updateStatus();
    }

    private void handleStripePaymentFailed(StripePaymentFailedEvent event) {
        data.setFailureReason(event.getFailureReason());
        updateStatus();
    }

    private void handleStripeCustomerCreationFailed(StripeCustomerCreationFailedEvent event) {
        data.setFailureReason("Stripe customer creation failed: " + event.getErrorMessage());
        // Cancel the payment timeout since we're failing
        timeoutScheduler.cancel(getId().id() + "-payment-timeout");
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

    @Override
    protected void compensate() {
        // Compensation: Reverse successful operations
        if (data.getStripeCustomerId() != null) {
            // Log compensation intent for customer deletion

            // dispatchCommand(new DeleteStripeCustomerCommand(getId(), data.getStripeCustomerId()));
        }
        if (data.getStripeInvoiceId() != null) {
            // Log compensation intent for invoice void

            // dispatchCommand(new VoidStripeInvoiceCommand(getId(), data.getStripeInvoiceId()));
        }
        // Dispatch notification to customer

        // dispatchCommand(new NotifyCustomerCommand(getId(), data.getUserId(), "Order cancelled due to payment timeout"));
    }

    private void handlePaymentTimeout() {
        // Handle payment timeout: notify customer and fail the saga
        // Dispatch a notification command or event
        // For now, fail the saga
        fail("Payment timeout after " + PaymentVerificationSagaData.PAYMENT_TIMEOUT_SLA.toMinutes() + " minutes");
    }
}

