package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.billing.domain.events.*;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId;
import com.bcbs239.regtech.billing.domain.valueobjects.ProcessedWebhookEvent;
import com.bcbs239.regtech.billing.domain.repositories.InvoiceRepository;
import com.bcbs239.regtech.billing.domain.services.PaymentService;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Command handler for processing Stripe webhook events.
 * Simplified version using domain interfaces.
 */
@Component
public class ProcessWebhookCommandHandler {

    private final InvoiceRepository invoiceRepository;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ProcessWebhookCommandHandler(
            InvoiceRepository invoiceRepository,
            PaymentService paymentService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.paymentService = paymentService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Handle webhook processing command
     */
    public Result<ProcessWebhookResponse> handle(ProcessWebhookCommand command) {
        // Verify webhook signature and parse event
        Result<WebhookEvent> webhookResult = paymentService.verifyAndParseWebhook(
            command.payload(), command.signatureHeader());
        
        if (webhookResult.isFailure()) {
            return Result.failure(webhookResult.getError().get());
        }
        
        WebhookEvent webhookEvent = webhookResult.getValue().get();
        
        // Process based on event type
        return processWebhookEvent(webhookEvent);
    }
    
    private Result<ProcessWebhookResponse> processWebhookEvent(WebhookEvent event) {
        try {
            switch (event.type()) {
                case "invoice.payment_succeeded" -> {
                    return handleInvoicePaymentSucceeded(event);
                }
                case "invoice.payment_failed" -> {
                    return handleInvoicePaymentFailed(event);
                }
                case "customer.subscription.created" -> {
                    return handleSubscriptionCreated(event);
                }
                case "customer.subscription.updated" -> {
                    return handleSubscriptionUpdated(event);
                }
                case "customer.subscription.deleted" -> {
                    return handleSubscriptionDeleted(event);
                }
                default -> {
                    // Unsupported event type - log and return success to avoid retries
                    return Result.success(ProcessWebhookResponse.ignored(event.id(), event.type()));
                }
            }
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("WEBHOOK_PROCESSING_ERROR",
                "Failed to process webhook event: " + e.getMessage(),
                "webhook.processing.error"));
        }
    }
    
    private Result<ProcessWebhookResponse> handleInvoicePaymentSucceeded(WebhookEvent event) {
        // Extract invoice ID from event data
        String invoiceId = event.getDataObject().get("id").asText();
        
        // Find invoice in our system
        Result<StripeInvoiceId> stripeInvoiceIdResult = StripeInvoiceId.fromString(invoiceId);
        if (stripeInvoiceIdResult.isFailure()) {
            return Result.failure(stripeInvoiceIdResult.getError().get());
        }
        
        Maybe<Invoice> invoiceMaybe = invoiceRepository.findByStripeInvoiceId(stripeInvoiceIdResult.getValue().get());
        if (invoiceMaybe.isEmpty()) {
            // Invoice not found - might be from external system, ignore
            return Result.success(ProcessWebhookResponse.ignored(event.id(), event.type()));
        }
        
        Invoice invoice = invoiceMaybe.getValue();
        
        // Mark invoice as paid
        Result<Invoice> updatedInvoiceResult = invoice.markAsPaid();
        if (updatedInvoiceResult.isFailure()) {
            return Result.failure(updatedInvoiceResult.getError().get());
        }
        
        // Save updated invoice
        Result<InvoiceId> saveResult = invoiceRepository.save(updatedInvoiceResult.getValue().get());
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        // Publish domain event
        InvoicePaymentSucceededEvent domainEvent = new InvoicePaymentSucceededEvent(
            saveResult.getValue().get(),
            invoice.getBillingAccountId().orElse(null),
            invoice.getTotalAmount()
        );
        applicationEventPublisher.publishEvent(domainEvent);
        
        return Result.success(ProcessWebhookResponse.processed(event.id(), event.type()));
    }
    
    private Result<ProcessWebhookResponse> handleInvoicePaymentFailed(WebhookEvent event) {
        // Similar to payment succeeded but mark as failed
        String invoiceId = event.getDataObject().get("id").asText();
        
        Result<StripeInvoiceId> stripeInvoiceIdResult = StripeInvoiceId.fromString(invoiceId);
        if (stripeInvoiceIdResult.isFailure()) {
            return Result.failure(stripeInvoiceIdResult.getError().get());
        }
        
        Maybe<Invoice> invoiceMaybe = invoiceRepository.findByStripeInvoiceId(stripeInvoiceIdResult.getValue().get());
        if (invoiceMaybe.isEmpty()) {
            return Result.success(ProcessWebhookResponse.ignored(event.id(), event.type()));
        }
        
        Invoice invoice = invoiceMaybe.getValue();
        
        // Mark invoice as failed
        Result<Invoice> updatedInvoiceResult = invoice.markAsPaymentFailed();
        if (updatedInvoiceResult.isFailure()) {
            return Result.failure(updatedInvoiceResult.getError().get());
        }
        
        // Save updated invoice
        Result<InvoiceId> saveResult = invoiceRepository.save(updatedInvoiceResult.getValue().get());
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        // Publish domain event
        InvoicePaymentFailedEvent domainEvent = new InvoicePaymentFailedEvent(
            saveResult.getValue().get(),
            invoice.getBillingAccountId().orElse(null),
            invoice.getTotalAmount()
        );
        applicationEventPublisher.publishEvent(domainEvent);
        
        return Result.success(ProcessWebhookResponse.processed(event.id(), event.type()));
    }
    
    private Result<ProcessWebhookResponse> handleSubscriptionCreated(WebhookEvent event) {
        // Handle subscription creation
        // For now, just return processed
        return Result.success(ProcessWebhookResponse.processed(event.id(), event.type()));
    }
    
    private Result<ProcessWebhookResponse> handleSubscriptionUpdated(WebhookEvent event) {
        // Handle subscription updates
        return Result.success(ProcessWebhookResponse.processed(event.id(), event.type()));
    }
    
    private Result<ProcessWebhookResponse> handleSubscriptionDeleted(WebhookEvent event) {
        // Handle subscription deletion
        return Result.success(ProcessWebhookResponse.processed(event.id(), event.type()));
    }
}