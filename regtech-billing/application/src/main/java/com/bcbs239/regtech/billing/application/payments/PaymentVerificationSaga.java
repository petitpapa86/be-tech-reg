package com.bcbs239.regtech.billing.application.payments;

import com.bcbs239.regtech.billing.application.integration.FinalizeBillingAccountCommand;
import com.bcbs239.regtech.billing.application.invoicing.CreateStripeInvoiceCommand;
import com.bcbs239.regtech.billing.application.payments.compensation.*;
import com.bcbs239.regtech.billing.application.policies.createstripecustomer.CreateStripeCustomerCommand;
import com.bcbs239.regtech.billing.application.subscriptions.CreateStripeSubscriptionCommand;
import com.bcbs239.regtech.billing.domain.invoices.events.StripeInvoiceCreatedEvent;
import com.bcbs239.regtech.billing.domain.payments.PaymentMethodId;
import com.bcbs239.regtech.billing.domain.payments.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.domain.payments.events.StripeCustomerCreatedEvent;
import com.bcbs239.regtech.billing.domain.payments.events.StripeCustomerCreationFailedEvent;
import com.bcbs239.regtech.billing.domain.payments.events.StripePaymentFailedEvent;
import com.bcbs239.regtech.billing.domain.payments.events.StripePaymentSucceededEvent;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.subscriptions.events.StripeSubscriptionCreatedEvent;
import com.bcbs239.regtech.billing.domain.subscriptions.events.StripeSubscriptionWebhookReceivedEvent;
import com.bcbs239.regtech.core.domain.saga.AbstractSaga;
import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaStatus;
import com.bcbs239.regtech.core.domain.saga.TimeoutScheduler;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import com.bcbs239.regtech.core.domain.shared.valueobjects.UserId;
import com.bcbs239.regtech.core.domain.saga.SagaStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

public class PaymentVerificationSaga extends AbstractSaga<PaymentVerificationSagaData> {
    private final ApplicationEventPublisher eventPublisher;
    private static final Logger log = LoggerFactory.getLogger(PaymentVerificationSaga.class);

    public PaymentVerificationSaga(
            SagaId id, 
            PaymentVerificationSagaData data, 
            TimeoutScheduler timeoutScheduler, 
            ApplicationEventPublisher eventPublisher) {
        super(id, "PaymentVerificationSaga", data, timeoutScheduler);
        this.eventPublisher = eventPublisher;
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
        // Validate and convert to value objects
        UserId userId = UserId.fromString(data.getUserId());
        
        Result<Email> emailResult = Email.create(data.getUserEmail());
        if (emailResult.isFailure()) {
            data.setFailureReason("Invalid email: " + emailResult.getError().get().getMessage());
            updateStatus();
            compensate();
            return;
        }
        
        Result<PaymentMethodId> paymentMethodResult = PaymentMethodId.fromString(data.getPaymentMethodId());
        if (paymentMethodResult.isFailure()) {
            data.setFailureReason("Invalid payment method: " + paymentMethodResult.getError().get().getMessage());
            updateStatus();
            compensate();
            return;
        }
        
        // Dispatch CreateStripeCustomerCommand to start the process
        CreateStripeCustomerCommand createCommand = new CreateStripeCustomerCommand(
                event.sagaId(),
                userId,
                emailResult.getValue().get(),
                data.getUserName(),
                paymentMethodResult.getValue().get()
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
        
        // Trigger compensation for payment failure
        if (getStatus() == SagaStatus.FAILED) {
            compensate();
        }
    }

    private void handleStripeCustomerCreationFailed(StripeCustomerCreationFailedEvent event) {
        data.setFailureReason("Stripe customer creation failed: " + event.getErrorMessage());
        // Cancel the payment timeout since we're failing
        timeoutScheduler.cancel(getId().id() + "-payment-timeout");
        updateStatus();
        
        // Trigger compensation for customer creation failure
        if (getStatus() == SagaStatus.FAILED) {
            compensate();
        }
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
        // COMPENSATION STRATEGY - Fully Automated:
        // Publishes events to Spring's event bus for asynchronous compensation handling.
        // Each event is processed by dedicated handlers that execute compensation actions.
        
        String failureReason = data.getFailureReason() != null ? data.getFailureReason() : "Unknown saga failure";
        
        log.info("SAGA_COMPENSATION_STARTED; details={}", java.util.Map.of(
            "sagaId", getId().id(),
            "failureReason", failureReason,
            "userId", String.valueOf(data.getUserId()),
            "billingAccountId", String.valueOf(data.getBillingAccountId()),
            "hasCustomer", String.valueOf(data.getStripeCustomerId() != null),
            "hasSubscription", String.valueOf(data.getStripeSubscriptionId() != null),
            "hasInvoice", String.valueOf(data.getStripeInvoiceId() != null),
            "hasPayment", String.valueOf(data.getStripePaymentIntentId() != null)
        ));
        
        // Step 1: Refund payment if payment was made
        if (data.getStripePaymentIntentId() != null) {
            RefundPaymentEvent refundEvent = new RefundPaymentEvent(
                getId().id(),
                data.getStripePaymentIntentId(),
                data.getUserId(),
                failureReason
            );
            eventPublisher.publishEvent(refundEvent);
            
            log.info("COMPENSATION_EVENT_PUBLISHED; details={}", java.util.Map.of(
                "sagaId", getId().id(),
                "eventType", "RefundPaymentEvent",
                "paymentIntentId", data.getStripePaymentIntentId()
            ));
        } 
        // Step 2: Void invoice if created but not paid
        else if (data.getStripeInvoiceId() != null && !data.getStripeInvoiceId().equals("default")) {
            VoidInvoiceEvent voidEvent = new VoidInvoiceEvent(
                getId().id(),
                data.getStripeInvoiceId(),
                data.getUserId(),
                failureReason
            );
            eventPublisher.publishEvent(voidEvent);
            
            log.info("COMPENSATION_EVENT_PUBLISHED; details={}", java.util.Map.of(
                "sagaId", getId().id(),
                "eventType", "VoidInvoiceEvent",
                "invoiceId", data.getStripeInvoiceId()
            ));
        }
        
        // Step 3: Cancel subscription if created
        if (data.getStripeSubscriptionId() != null && !data.getStripeSubscriptionId().equals("default")) {
            CancelSubscriptionEvent cancelEvent = new CancelSubscriptionEvent(
                getId().id(),
                data.getStripeSubscriptionId(),
                data.getUserId(),
                failureReason
            );
            eventPublisher.publishEvent(cancelEvent);
            
            log.info("COMPENSATION_EVENT_PUBLISHED; details={}", java.util.Map.of(
                "sagaId", getId().id(),
                "eventType", "CancelSubscriptionEvent",
                "subscriptionId", data.getStripeSubscriptionId()
            ));
        }
        
        // Step 4: Suspend billing account if created
        if (data.getBillingAccountId() != null) {
            SuspendBillingAccountEvent suspendEvent = new SuspendBillingAccountEvent(
                getId().id(),
                data.getBillingAccountId(),
                data.getUserId(),
                failureReason
            );
            eventPublisher.publishEvent(suspendEvent);
            
            log.info("COMPENSATION_EVENT_PUBLISHED; details={}", java.util.Map.of(
                "sagaId", getId().id(),
                "eventType", "SuspendBillingAccountEvent",
                "billingAccountId", data.getBillingAccountId()
            ));
        }
        
        // Step 5: Notify user about the failure
        if (data.getUserId() != null) {
            String notificationMessage = buildCompensationMessage(failureReason);
            NotifyUserEvent.NotificationType notificationType;
            
            if (data.getStripePaymentIntentId() != null) {
                notificationType = NotifyUserEvent.NotificationType.PAYMENT_REFUNDED;
            } else if (data.getStripeSubscriptionId() != null) {
                notificationType = NotifyUserEvent.NotificationType.SUBSCRIPTION_CANCELLED;
            } else {
                notificationType = NotifyUserEvent.NotificationType.SETUP_FAILED;
            }
            
            NotifyUserEvent notifyEvent = new NotifyUserEvent(
                getId().id(),
                data.getUserId(),
                "Subscription Setup Issue",
                notificationMessage,
                notificationType
            );
            eventPublisher.publishEvent(notifyEvent);
            
            log.info("COMPENSATION_EVENT_PUBLISHED; details={}", java.util.Map.of(
                "sagaId", getId().id(),
                "eventType", "NotifyUserEvent",
                "userId", data.getUserId(),
                "notificationType", notificationType.name()
            ));
        }
        
        log.info("SAGA_COMPENSATION_COMPLETED; details={}", java.util.Map.of(
            "sagaId", getId().id(),
            "failureReason", failureReason,
            "compensationStatus", "EVENTS_PUBLISHED",
            "note", "All compensation events published. Handlers will execute asynchronously."
        ));
    }
    
    /**
     * Build a user-friendly compensation message based on the failure reason and saga state.
     * This message would be sent to the user via email/notification in production.
     */
    private String buildCompensationMessage(String failureReason) {
        StringBuilder message = new StringBuilder();
        message.append("We encountered an issue setting up your subscription.\n\n");
        
        if (data.getStripePaymentIntentId() != null) {
            message.append("Your payment has been fully refunded and should appear in your account within 5-10 business days. ");
        } else if (data.getStripeInvoiceId() != null) {
            message.append("No charges were made to your payment method. ");
        }
        
        message.append("You can try again by visiting your account settings.\n\n");
        message.append("If you continue to experience issues, please contact our support team with reference: ");
        message.append(getId().id()).append("\n\n");
        message.append("Technical details: ").append(failureReason);
        
        return message.toString();
    }

    private void handlePaymentTimeout() {
        // Handle payment timeout: notify customer and fail the saga
        // Dispatch a notification command or event
        // For now, fail the saga
        fail("Payment timeout after " + PaymentVerificationSagaData.PAYMENT_TIMEOUT_SLA.toMinutes() + " minutes");
    }
}

