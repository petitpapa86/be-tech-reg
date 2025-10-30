package com.bcbs239.regtech.billing.application.policies;

import com.bcbs239.regtech.billing.domain.billing.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.application.policies.createstripecustomer.CreateStripeCustomerCommand;
import com.bcbs239.regtech.billing.domain.events.StripeCustomerCreatedEvent;
import com.bcbs239.regtech.billing.domain.events.StripeSubscriptionCreatedEvent;
import com.bcbs239.regtech.billing.domain.events.StripeSubscriptionWebhookReceivedEvent;
import com.bcbs239.regtech.billing.domain.events.StripeInvoiceCreatedEvent;
import com.bcbs239.regtech.billing.domain.events.StripePaymentSucceededEvent;
import com.bcbs239.regtech.billing.domain.events.StripePaymentFailedEvent;
import com.bcbs239.regtech.core.saga.SagaStartedEvent;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.core.saga.AbstractSaga;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaClosures;
import com.bcbs239.regtech.core.saga.SagaStatus;
import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.core.shared.ErrorDetail;

import java.time.Instant;
import java.util.Map;

public class PaymentVerificationSaga extends AbstractSaga<PaymentVerificationSagaData> {

    public PaymentVerificationSaga(SagaId id, PaymentVerificationSagaData data, SagaClosures.TimeoutScheduler timeoutScheduler) {
        super(id, "PaymentVerificationSaga", data, timeoutScheduler);
        registerHandlers();
    }

    private void registerHandlers() {
        onEvent(SagaStartedEvent.class, this::handleSagaStarted);
        onEvent(StripeCustomerCreatedEvent.class, this::handleStripeCustomerCreated);
        onEvent(StripeSubscriptionCreatedEvent.class, this::handleStripeSubscriptionCreated);
        onEvent(StripeSubscriptionWebhookReceivedEvent.class, this::handleStripeSubscriptionWebhookReceived);
        onEvent(StripeInvoiceCreatedEvent.class, this::handleStripeInvoiceCreated);
        onEvent(StripePaymentSucceededEvent.class, this::handleStripePaymentSucceeded);
        onEvent(StripePaymentFailedEvent.class, this::handleStripePaymentFailed);
    }

    private void handleSagaStarted(SagaStartedEvent event) {
        // Dispatch CreateStripeCustomerCommand to start the process
        var createResult = CreateStripeCustomerCommand.create(
                event.getSagaId(),
                data.getUserEmail(),
                data.getUserName(),
                data.getPaymentMethodId()
        );
        if (createResult.isSuccess()) {
            dispatchCommand(createResult.getValue().orElseThrow());
        } else {
            // Fail the saga due to validation error
            String reason = createResult.getError().map(ErrorDetail::getMessage).orElse("Invalid CreateStripeCustomerCommand");
            fail(reason);
            return;
        }

        LoggingConfiguration.createStructuredLog("PAYMENT_VERIFICATION_SAGA_DISPATCHED_COMMAND", Map.of(
            "sagaId", event.getSagaId(),
            "commandType", "CreateStripeCustomerCommand",
            "userEmail", data.getUserEmail()
        ));

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
            data.getUserId()
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
                data.getUserId()
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
                data.getUserId()
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
            // Dispatch command to delete Stripe customer
            // dispatchCommand(new DeleteStripeCustomerCommand(getId(), data.getStripeCustomerId()));
        }
        if (data.getStripeInvoiceId() != null) {
            // Dispatch command to void invoice
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
