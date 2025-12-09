package com.bcbs239.regtech.billing.infrastructure.services;


import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.billing.domain.shared.events.WebhookEvent;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeCustomer;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeSubscription;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Event;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Infrastructure implementation of PaymentService using Stripe.
 * Bridges the domain interface with the Stripe infrastructure service.
 */
@Service
public class StripePaymentService implements PaymentService {
    
    private final StripeService stripeService;
    private final ObjectMapper objectMapper;

    public StripePaymentService(StripeService stripeService, ObjectMapper objectMapper) {
        this.stripeService = stripeService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<CustomerCreationResult> createCustomer(CustomerCreationRequest request) {
        try {
            // Create customer
            Result<StripeCustomer> result = stripeService.createCustomer(
                request.email(),
                request.name()
            );
            
            if (result.isFailure()) {
                return Result.failure(result.getError().get());
            }
            
            StripeCustomer customer = result.getValue().get();
            
            // If payment method provided, attach it and set as default
            if (request.paymentMethodId() != null) {
                Result<Void> attachResult = stripeService.attachPaymentMethod(
                    customer.customerId(),
                    request.paymentMethodId()
                );
                
                if (attachResult.isFailure()) {
                    return Result.failure(attachResult.getError().get());
                }
                
                Result<Void> defaultResult = stripeService.setDefaultPaymentMethod(
                    customer.customerId(),
                    request.paymentMethodId()
                );
                
                if (defaultResult.isFailure()) {
                    return Result.failure(defaultResult.getError().get());
                }
            }
            
            CustomerCreationResult domainResult = new CustomerCreationResult(
                customer.customerId(),
                customer.email(),
                customer.name()
            );
            
            return Result.success(domainResult);
            
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("CUSTOMER_CREATION_FAILED", ErrorType.BUSINESS_RULE_ERROR,
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
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_CREATION_FAILED", ErrorType.BUSINESS_RULE_ERROR,
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
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_CANCELLATION_FAILED", ErrorType.BUSINESS_RULE_ERROR,
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
            return Result.failure(ErrorDetail.of("INVOICE_CREATION_FAILED", ErrorType.BUSINESS_RULE_ERROR,
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
            return Result.failure(ErrorDetail.of("WEBHOOK_VERIFICATION_FAILED", ErrorType.BUSINESS_RULE_ERROR,
                "Failed to verify and parse webhook: " + e.getMessage(),
                "error.payment.webhookVerificationFailed"));
        }
    }
    
    @Override
    public Result<RefundResult> refundPayment(String paymentIntentId, String reason) {
        try {
            Result<StripeService.StripeRefund> refundResult = 
                stripeService.refundPayment(paymentIntentId, reason);
            
            if (refundResult.isFailure()) {
                return Result.failure(refundResult.getError().get());
            }
            
            StripeService.StripeRefund refund = refundResult.getValue().get();
            
            RefundResult domainResult = new RefundResult(
                refund.refundId(),
                refund.status(),
                refund.amount(),
                refund.paymentIntentId()
            );
            
            return Result.success(domainResult);
            
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("REFUND_FAILED", ErrorType.BUSINESS_RULE_ERROR,
                "Failed to refund payment: " + e.getMessage(),
                "error.payment.refundFailed"));
        }
    }
    
    @Override
    public Result<Void> voidInvoice(String invoiceId) {
        try {
            Result<Void> voidResult = stripeService.voidInvoice(invoiceId);
            
            if (voidResult.isFailure()) {
                return Result.failure(voidResult.getError().get());
            }
            
            return Result.success(null);
            
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("VOID_INVOICE_FAILED", ErrorType.BUSINESS_RULE_ERROR,
                "Failed to void invoice: " + e.getMessage(),
                "error.payment.voidInvoiceFailed"));
        }
    }
}

