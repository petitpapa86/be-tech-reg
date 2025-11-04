package com.bcbs239.regtech.billing.infrastructure.services;


import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.billing.domain.shared.events.WebhookEvent;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeCustomer;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeSubscription;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.stripe.model.Event;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Infrastructure implementation of PaymentService using Stripe.
 * Bridges the domain interface with the Stripe infrastructure service.
 */
@Service
public class StripePaymentService implements PaymentService {
    
    private final StripeService stripeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StripePaymentService(StripeService stripeService) {
        this.stripeService = stripeService;
    }
    
    @Override
    public Result<CustomerCreationResult> createCustomer(CustomerCreationRequest request) {
        try {
            // StripeService supports createCustomer(email, name). PaymentMethodId is not required here.
            Result<StripeCustomer> result = stripeService.createCustomer(
                request.email(),
                request.name()
            );
            
            if (result.isFailure()) {
                return Result.failure(result.getError().get());
            }
            
            StripeCustomer customer = result.getValue().get();
            CustomerCreationResult domainResult = new CustomerCreationResult(
                customer.customerId(),
                customer.email(),
                customer.name()
            );
            
            return Result.success(domainResult);
            
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("CUSTOMER_CREATION_FAILED",
                "Failed to create customer: " + e.getMessage(),
                "error.payment.customerCreationFailed"));
        }
    }
    
    @Override
    public Result<SubscriptionCreationResult> createSubscription(SubscriptionCreationRequest request) {
        try {
            // StripeService.createSubscription expects (StripeCustomerId, SubscriptionTier)
            Result<StripeSubscription> result = stripeService.createSubscription(
                request.customerId(),
                request.tier()
            );
            
            if (result.isFailure()) {
                return Result.failure(result.getError().get());
            }
            
            StripeSubscription subscription = result.getValue().get();
            // Map domain result: subscriptionId is a String in the PaymentService contract
            String subId = subscription.subscriptionId() != null ? subscription.subscriptionId().value() : null;
            SubscriptionCreationResult domainResult = new SubscriptionCreationResult(
                subId,
                subscription.status(),
                subscription.customerId()
            );
            
            return Result.success(domainResult);
            
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_CREATION_FAILED",
                "Failed to create subscription: " + e.getMessage(),
                "error.payment.subscriptionCreationFailed"));
        }
    }
    
    @Override
    public Result<Void> cancelSubscription(String subscriptionId) {
        try {
            // Convert string id to domain StripeSubscriptionId
            var parsed = com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId.fromString(subscriptionId);
            if (parsed.isFailure()) {
                return Result.failure(parsed.getError().get());
            }
            return stripeService.cancelSubscription(parsed.getValue().get());
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_CANCELLATION_FAILED",
                "Failed to cancel subscription: " + e.getMessage(),
                "error.payment.subscriptionCancellationFailed"));
        }
    }
    
    @Override
    public Result<InvoiceCreationResult> createInvoice(InvoiceCreationRequest request) {
        try {
            // PaymentService passes amount as String - convert to Money using EUR default
            java.math.BigDecimal amountBd = new BigDecimal(request.amount());
            com.bcbs239.regtech.billing.domain.valueobjects.Money money = com.bcbs239.regtech.billing.domain.valueobjects.Money.of(amountBd, Currency.getInstance("EUR"));

            Result<com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeInvoice> result =
                stripeService.createInvoice(request.customerId(), money, request.description());

            if (result.isFailure()) {
                return Result.failure(result.getError().get());
            }
            
            com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeInvoice invoice = result.getValue().get();
            // Map invoice fields to strings expected by PaymentService contract
            String invoiceId = invoice.invoiceId() != null ? invoice.invoiceId().value() : null;
            String amountStr = invoice.amount() != null ? invoice.amount().amount().toPlainString() : null;
            InvoiceCreationResult domainResult = new InvoiceCreationResult(
                invoiceId,
                invoice.status(),
                amountStr
            );
            
            return Result.success(domainResult);
            
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("INVOICE_CREATION_FAILED",
                "Failed to create invoice: " + e.getMessage(),
                "error.payment.invoiceCreationFailed"));
        }
    }
    
    @Override
    public Result<com.bcbs239.regtech.billing.domain.shared.events.WebhookEvent> verifyAndParseWebhook(String payload, String signatureHeader) {
        try {
            Result<Event> eventResult = stripeService.verifyWebhookSignature(payload, signatureHeader);
            if (eventResult.isFailure()) {
                return Result.failure(eventResult.getError().get());
            }
            Event event = eventResult.getValue().get();

            // Convert event JSON to JsonNode
            JsonNode root = objectMapper.readTree(event.toJson());
            JsonNode dataNode = root.has("data") ? root.get("data") : root;
            long created = event.getCreated() != null ? event.getCreated().longValue() : java.time.Instant.now().getEpochSecond();

            WebhookEvent webhookEvent = new WebhookEvent(event.getId(), event.getType(), dataNode, created);
            return Result.success(webhookEvent);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("WEBHOOK_VERIFICATION_FAILED",
                "Failed to verify and parse webhook: " + e.getMessage(),
                "error.payment.webhookVerificationFailed"));
        }
    }
}