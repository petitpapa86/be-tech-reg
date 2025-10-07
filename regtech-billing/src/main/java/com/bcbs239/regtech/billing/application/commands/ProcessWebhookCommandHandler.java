package com.bcbs239.regtech.billing.application.commands;

import com.bcbs239.regtech.billing.domain.aggregates.Invoice;
import com.bcbs239.regtech.billing.domain.valueobjects.*;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaInvoiceRepository;
import com.bcbs239.regtech.billing.infrastructure.stripe.StripeService;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.stripe.model.Event;
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

    public ProcessWebhookCommandHandler(
            JpaInvoiceRepository invoiceRepository,
            StripeService stripeService) {
        this.invoiceRepository = invoiceRepository;
        this.stripeService = stripeService;
    }

    /**
     * Handle the ProcessWebhookCommand by injecting repository operations as closures
     */
    public Result<ProcessWebhookResponse> handle(ProcessWebhookCommand command) {
        return processWebhook(
            command,
            this::checkIfEventProcessed,
            this::recordEventProcessed,
            invoiceRepository.invoiceFinder(),
            invoiceRepository.invoiceSaver(),
            stripeService::verifyWebhookSignature,
            stripeService::synchronizeInvoiceStatus
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
            Function<Event, Result<StripeService.InvoiceStatusUpdate>> invoiceStatusSynchronizer) {

        // Step 1: Check if event was already processed (idempotency)
        Maybe<ProcessedWebhookEvent> existingEvent = eventChecker.apply(command.eventId());
        if (existingEvent.isPresent()) {
            ProcessedWebhookEvent processed = existingEvent.get();
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
                invoiceFinder, invoiceSaver, invoiceStatusSynchronizer);
            
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
            Function<Event, Result<StripeService.InvoiceStatusUpdate>> invoiceStatusSynchronizer) {

        if (command.isInvoiceEvent()) {
            return processInvoiceEvent(stripeEvent, invoiceFinder, invoiceSaver, invoiceStatusSynchronizer);
        } else if (command.isSubscriptionEvent()) {
            return processSubscriptionEvent(stripeEvent);
        } else if (command.isPaymentEvent()) {
            return processPaymentEvent(stripeEvent);
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
        
        Invoice invoice = invoiceMaybe.get();
        
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
     * Process subscription-related webhook events
     */
    private static Result<String> processSubscriptionEvent(Event stripeEvent) {
        // TODO: Implement subscription event processing
        // For now, just acknowledge the event
        return Result.success("Subscription event processed: " + stripeEvent.getType());
    }

    /**
     * Process payment-related webhook events
     */
    private static Result<String> processPaymentEvent(Event stripeEvent) {
        // TODO: Implement payment event processing
        // For now, just acknowledge the event
        return Result.success("Payment event processed: " + stripeEvent.getType());
    }

    /**
     * Update invoice status based on Stripe webhook event
     */
    private static Result<Void> updateInvoiceFromStripeEvent(
            Invoice invoice, 
            StripeService.InvoiceStatusUpdate statusUpdate) {
        
        switch (statusUpdate.eventType()) {
            case "invoice.paid":
            case "payment_intent.succeeded":
                if (statusUpdate.paid() != null && statusUpdate.paid()) {
                    return invoice.markAsPaid(Instant.now(), () -> Instant.now());
                }
                break;
                
            case "invoice.payment_failed":
            case "payment_intent.payment_failed":
                return invoice.markAsFailed(() -> Instant.now());
                
            case "invoice.voided":
                return invoice.voidInvoice(() -> Instant.now());
                
            default:
                // For other events, just acknowledge without changing status
                return Result.success(null);
        }
        
        return Result.success(null);
    }

    /**
     * Check if webhook event was already processed (mock implementation)
     */
    private Maybe<ProcessedWebhookEvent> checkIfEventProcessed(String eventId) {
        // TODO: Implement actual database lookup
        // For now, return none (event not processed)
        return Maybe.none();
    }

    /**
     * Record that webhook event was processed (mock implementation)
     */
    private void recordEventProcessed(ProcessedWebhookEvent event) {
        // TODO: Implement actual database storage
        // For now, just log the event
        System.out.println("Recording processed webhook event: " + event.eventId() + 
            " -> " + event.result());
    }

    // Helper record for function parameters
    public record WebhookVerificationData(String payload, String signatureHeader) {}
}