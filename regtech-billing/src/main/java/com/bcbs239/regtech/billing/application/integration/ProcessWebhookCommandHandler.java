package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.billing.domain.events.*;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId;
import com.bcbs239.regtech.billing.domain.valueobjects.ProcessedWebhookEvent;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaInvoiceRepository;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.stripe.model.Event;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Command handler for processing Stripe webhook events.
 * Uses functional programming patterns with closure-based dependency injection.
 */
@Component
public class ProcessWebhookCommandHandler {

    private final JpaInvoiceRepository invoiceRepository;
    private final StripeService stripeService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ProcessWebhookCommandHandler(
            JpaInvoiceRepository invoiceRepository,
            StripeService stripeService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.stripeService = stripeService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Check if a webhook event has already been processed (idempotency check)
     */
    private Maybe<ProcessedWebhookEvent> checkIfEventProcessed(String unusedEventId) {
        // TODO: Implement database lookup for processed webhook events
        // For now, return none to indicate event hasn't been processed
        return Maybe.none();
    }

    /**
     * Record that a webhook event has been processed
     */
    private void recordEventProcessed(ProcessedWebhookEvent unusedEvent) {
        // TODO: Implement database persistence for processed webhook events
        // For now, just log the event recording
        // This would typically save to a database table
    }

    /**
     * Handle the ProcessWebhookCommand by injecting repository operations as closures
     */
    public Result<ProcessWebhookResponse> handle(ProcessWebhookCommand command) {
        return processWebhook(
                command,
                this::checkIfEventProcessed,
                this::recordEventProcessed,
                invoiceRepository.invoiceByStripeIdFinder(),
                invoiceRepository.invoiceSaver(),
                verificationData -> stripeService.verifyWebhookSignature(verificationData.payload(), verificationData.signatureHeader()),
                stripeService::synchronizeInvoiceStatus,
                applicationEventPublisher::publishEvent
        );
    }

    /**
     * Pure function for webhook processing with injected dependencies as closures.
     * This function contains no side effects and can be easily tested.
     */
    static Result<ProcessWebhookResponse> processWebhook(
            ProcessWebhookCommand command,
            Function<String, Maybe<ProcessedWebhookEvent>> eventChecker,
            Consumer<ProcessedWebhookEvent> eventRecorder,
            Function<StripeInvoiceId, Maybe<Invoice>> invoiceFinder,
            Function<Invoice, Result<InvoiceId>> invoiceSaver,
            Function<WebhookVerificationData, Result<Event>> webhookVerifier,
            Function<Event, Result<StripeService.InvoiceStatusUpdate>> invoiceStatusSynchronizer,
            Consumer<Object> sagaEventPublisher) {

        // Step 1: Check if event was already processed (idempotency)
        Maybe<ProcessedWebhookEvent> existingEvent = eventChecker.apply(command.eventId());
        if (existingEvent.isPresent()) {
            ProcessedWebhookEvent processed = existingEvent.getValue();
            if (processed.wasSuccessful()) {
                return Result.success(ProcessWebhookResponse.alreadyProcessed(
                        command.eventId(), command.eventType()));
            } else {
                // Event was processed but failed - we can retry
                // Continue with processing
            }
        }

        try {
            // Step 2: Verify webhook signature
            WebhookVerificationData verificationData = new WebhookVerificationData(
                    command.payload(), command.signatureHeader());
            Result<Event> eventResult = webhookVerifier.apply(verificationData);
            if (eventResult.isFailure()) {
                ProcessedWebhookEvent failedEvent = ProcessedWebhookEvent.failure(
                        command.eventId(), command.eventType(),
                        "Signature verification failed: " + eventResult.getError().get().getMessage());
                eventRecorder.accept(failedEvent);
                return Result.failure(eventResult.getError().get());
            }
            Event stripeEvent = eventResult.getValue().get();

            // Step 3: Route event based on type
            Result<String> processingResult = routeEvent(command, stripeEvent,
                    invoiceFinder, invoiceSaver, invoiceStatusSynchronizer, sagaEventPublisher);

            if (processingResult.isSuccess()) {
                // Record successful processing
                ProcessedWebhookEvent successEvent = ProcessedWebhookEvent.success(
                        command.eventId(), command.eventType());
                eventRecorder.accept(successEvent);

                return Result.success(ProcessWebhookResponse.success(
                        command.eventId(), command.eventType(), processingResult.getValue().get()));
            } else {
                // Record failed processing
                ProcessedWebhookEvent failedEvent = ProcessedWebhookEvent.failure(
                        command.eventId(), command.eventType(),
                        processingResult.getError().get().getMessage());
                eventRecorder.accept(failedEvent);

                return Result.failure(processingResult.getError().get());
            }

        } catch (Exception e) {
            // Record unexpected failure
            ProcessedWebhookEvent failedEvent = ProcessedWebhookEvent.failure(
                    command.eventId(), command.eventType(),
                    "Unexpected error: " + e.getMessage());
            eventRecorder.accept(failedEvent);

            return Result.failure(ErrorDetail.of("WEBHOOK_PROCESSING_FAILED",
                    "Unexpected error processing webhook: " + e.getMessage(), "webhook.processing.failed"));
        }
    }

    /**
     * Route webhook event to appropriate handler based on event type
     */
    private static Result<String> routeEvent(
            ProcessWebhookCommand command,
            Event stripeEvent,
            Function<StripeInvoiceId, Maybe<Invoice>> invoiceFinder,
            Function<Invoice, Result<InvoiceId>> invoiceSaver,
            Function<Event, Result<StripeService.InvoiceStatusUpdate>> invoiceStatusSynchronizer,
            Consumer<Object> sagaEventPublisher) {

        if (command.isInvoiceEvent()) {
            return processInvoiceEvent(stripeEvent, invoiceFinder, invoiceSaver, invoiceStatusSynchronizer);
        } else if (command.isSubscriptionEvent()) {
            return processSubscriptionEvent(stripeEvent, sagaEventPublisher);
        } else if (command.isPaymentEvent()) {
            return processPaymentEvent(stripeEvent, sagaEventPublisher);
        } else {
            // Ignore unknown event types
            return Result.success("Event type ignored: " + command.eventType());
        }
    }

    /**
     * Process invoice-related webhook events
     */
    private static Result<String> processInvoiceEvent(
            Event stripeEvent,
            Function<StripeInvoiceId, Maybe<Invoice>> invoiceFinder,
            Function<Invoice, Result<InvoiceId>> invoiceSaver,
            Function<Event, Result<StripeService.InvoiceStatusUpdate>> invoiceStatusSynchronizer) {

        // Synchronize invoice status from Stripe
        Result<StripeService.InvoiceStatusUpdate> statusUpdateResult = invoiceStatusSynchronizer.apply(stripeEvent);
        if (statusUpdateResult.isFailure()) {
            return Result.failure(statusUpdateResult.getError().get());
        }

        StripeService.InvoiceStatusUpdate statusUpdate = statusUpdateResult.getValue().get();

        // Find local invoice
        Maybe<Invoice> invoiceMaybe = invoiceFinder.apply(statusUpdate.invoiceId());
        if (invoiceMaybe.isEmpty()) {
            // Invoice not found locally - this might be expected for some events
            return Result.success("Invoice not found locally: " + statusUpdate.invoiceId());
        }

        Invoice invoice = invoiceMaybe.getValue();

        // Update invoice status based on Stripe event
        Result<Void> updateResult = updateInvoiceFromStripeEvent(invoice, statusUpdate);
        if (updateResult.isFailure()) {
            return Result.failure(updateResult.getError().get());
        }

        // Save updated invoice
        Result<InvoiceId> saveResult = invoiceSaver.apply(invoice);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }

        return Result.success("Invoice status updated: " + statusUpdate.invoiceId() +
                " -> " + statusUpdate.status());
    }

    /**
     * Update invoice status based on Stripe webhook event data
     */
    private static Result<Void> updateInvoiceFromStripeEvent(
            Invoice invoice,
            StripeService.InvoiceStatusUpdate statusUpdate) {

        // Map Stripe status to Invoice status updates
        switch (statusUpdate.status().toLowerCase()) {
            case "paid":
                // Invoice has been paid
                if (statusUpdate.paid() != null && statusUpdate.paid()) {
                    Instant paidAt = Instant.now(); // In production, extract from Stripe event
                    return invoice.markAsPaid(paidAt, () -> Instant.now());
                }
                break;

            case "void":
                // Invoice has been voided/cancelled
                return invoice.voidInvoice(() -> Instant.now());

            case "open":
                // Invoice is still open/pending - no status change needed
                return Result.success(null);

            case "uncollectible":
            case "draft":
                // Invoice is in a state that doesn't require action
                return Result.success(null);

            default:
                // Unknown status - log but don't fail
                return Result.success(null);
        }

        return Result.success(null);
    }

    /**
     * Process subscription-related webhook events
     */
    private static Result<String> processSubscriptionEvent(Event stripeEvent, Consumer<Object> sagaEventPublisher) {
        // Extract subscription data from the event
        String eventType = stripeEvent.getType();

        switch (eventType) {
            case "customer.subscription.created":
                // Publish saga event for subscription creation webhook
                // TODO: Extract saga ID from correlation data
                SagaId sagaId = findSagaIdForSubscription(stripeEvent);
                if (sagaId != null) {
                    sagaEventPublisher.accept(new StripeSubscriptionWebhookReceivedEvent(
                            sagaId,
                            extractSubscriptionId(stripeEvent),
                            extractInvoiceId(stripeEvent)
                    ));
                }
                break;

            case "customer.subscription.updated":
            case "customer.subscription.deleted":
                // Handle other subscription events if needed
                break;

            default:
                // Unknown subscription event
                break;
        }

        return Result.success("Subscription event processed: " + stripeEvent.getType());
    }

    /**
     * Process payment-related webhook events
     */
    private static Result<String> processPaymentEvent(Event stripeEvent, Consumer<Object> sagaEventPublisher) {
        // Extract payment data from the event
        String eventType = stripeEvent.getType();

        switch (eventType) {
            case "invoice.payment_succeeded":
                // Publish saga event for successful payment
                // TODO: Extract saga ID from correlation data
                SagaId sagaId = findSagaIdForPayment(stripeEvent);
                if (sagaId != null) {
                    sagaEventPublisher.accept(new StripePaymentSucceededEvent(
                            sagaId,
                            extractPaymentIntentId(stripeEvent)
                    ));
                }
                break;

            case "invoice.payment_failed":
                // Publish saga event for failed payment
                // TODO: Extract saga ID from correlation data
                SagaId sagaIdFailed = findSagaIdForPayment(stripeEvent);
                if (sagaIdFailed != null) {
                    sagaEventPublisher.accept(new StripePaymentFailedEvent(
                            sagaIdFailed,
                            extractFailureReason(stripeEvent)
                    ));
                }
                break;

            default:
                // Unknown payment event
                break;
        }

        return Result.success("Payment event processed: " + stripeEvent.getType());
    }

    /**
     * Find saga ID for subscription events (placeholder implementation)
     * TODO: Implement correlation logic based on Stripe IDs stored in saga data
     */
    private static SagaId findSagaIdForSubscription(Event unusedStripeEvent) {
        // TODO: Extract customer ID from event and look up saga ID from correlation data
        // For now, return null to indicate saga ID not found
        return null;
    }

    /**
     * Find saga ID for payment events (placeholder implementation)
     * TODO: Implement correlation logic based on Stripe IDs stored in saga data
     */
    private static SagaId findSagaIdForPayment(@SuppressWarnings("unused") Event stripeEvent) {
        // TODO: Extract invoice/customer ID from event and look up saga ID from correlation data
        // For now, return null to indicate saga ID not found
        return null;
    }

    /**
     * Extract subscription ID from Stripe event (placeholder)
     */
    private static String extractSubscriptionId(Event unusedStripeEvent) {
        // TODO: Extract from stripeEvent.getData().getObject()
        return "placeholder-subscription-id";
    }

    /**
     * Extract invoice ID from Stripe event (placeholder)
     */
    private static String extractInvoiceId(@SuppressWarnings("unused") Event stripeEvent) {
        // TODO: Extract from stripeEvent.getData().getObject()
        return "placeholder-invoice-id";
    }

    /**
     * Extract payment intent ID from Stripe event (placeholder)
     */
    private static String extractPaymentIntentId(@SuppressWarnings("unused") Event stripeEvent) {
        // TODO: Extract from stripeEvent.getData().getObject()
        return "placeholder-payment-intent-id";
    }

    /**
     * Extract failure reason from Stripe event (placeholder)
     */
    private static String extractFailureReason(Event unusedStripeEvent) {
        // TODO: Extract from stripeEvent.getData().getObject()
        return "placeholder-failure-reason";
    }
}