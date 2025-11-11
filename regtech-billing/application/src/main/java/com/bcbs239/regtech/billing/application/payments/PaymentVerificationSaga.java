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
        data.setBillingAccountId(event.getBillingAccountId());
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
            // Create invoice with subscription amount (50000 cents = €500.00)
            dispatchCommand(new CreateStripeInvoiceCommand(
                getId(), 
                data.getStripeCustomerId(),
                data.getSubscriptionId(),
                "50000", // STARTER tier amount in cents
                "Subscription payment for STARTER tier"
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
                data.getStripeCustomerId(),
                data.getSubscriptionId(),
                "50000", // STARTER tier amount in cents
                "Subscription payment for STARTER tier"
            ));
        }
        updateStatus();
    }

    private void handleStripeInvoiceCreated(StripeInvoiceCreatedEvent event) {
        data.setStripeInvoiceId(event.getStripeInvoiceId());
        
        // Finalize billing account now that all setup is complete
        dispatchCommand(new FinalizeBillingAccountCommand(
            getId(),
            data.getStripeCustomerId(),
            data.getStripeSubscriptionId(),
            data.getStripeInvoiceId(),
            data.getBillingAccountId(),
            data.getCorrelationId()
        ));
        
        // Don't call updateStatus() here - let payment webhook complete the saga
        // This ensures finalization command has time to execute
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
            // Complete saga only after payment is confirmed via webhook
            // This ensures finalization command has time to execute
            setStatus(SagaStatus.COMPLETED);
            setCompletedAt(Instant.now());
        }
    }

    @Override
    protected void compensate() {
        // COMPENSATION STRATEGY:
        // This saga follows an "orchestration" pattern where each step is designed to be idempotent.
        // 
        // IMPORTANT: Stripe charges happen immediately when invoice is created/finalized.
        // If any step fails AFTER payment, we need to handle it properly:
        //
        // 1. Customer Creation - Safe to rollback (delete customer)
        // 2. Subscription Creation - Safe to rollback (cancel subscription)
        // 3. Invoice Creation - CRITICAL: Invoice may already be paid!
        //    - If invoice is unpaid: Void the invoice
        //    - If invoice is paid: Issue a refund via Stripe API
        // 4. Local DB operations - Use database transactions (REQUIRES_NEW propagation)
        //
        // Current implementation uses compensation commands (commented out):
        // - These would be implemented in production to handle rollback
        // - Payment refunds should be handled via RefundPaymentCommand
        // - Customer notifications via NotifyCustomerCommand
        
        if (data.getStripeCustomerId() != null) {
            // TODO: Implement customer deletion or marking as inactive
            // dispatchCommand(new DeleteStripeCustomerCommand(getId(), data.getStripeCustomerId()));
        }
        
        if (data.getStripeInvoiceId() != null) {
            // TODO: Implement invoice voiding or refund based on payment status
            // Check invoice payment status first:
            // - If unpaid: dispatchCommand(new VoidStripeInvoiceCommand(getId(), data.getStripeInvoiceId()));
            // - If paid: dispatchCommand(new RefundStripePaymentCommand(getId(), data.getStripePaymentIntentId()));
        }
        
        if (data.getStripeSubscriptionId() != null) {
            // TODO: Implement subscription cancellation
            // dispatchCommand(new CancelStripeSubscriptionCommand(getId(), data.getStripeSubscriptionId()));
        }
        
        // TODO: Notify customer about failed transaction
        // dispatchCommand(new NotifyCustomerCommand(getId(), data.getUserId(), 
        //     "Your subscription setup encountered an error. Any charges will be refunded."));
    }

    private void handlePaymentTimeout() {
        // Handle payment timeout: notify customer and fail the saga
        // Dispatch a notification command or event
        // For now, fail the saga
        fail("Payment timeout after " + PaymentVerificationSagaData.PAYMENT_TIMEOUT_SLA.toMinutes() + " minutes");
    }
}

