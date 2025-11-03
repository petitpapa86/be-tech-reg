package com.bcbs239.regtech.billing.infrastructure.services;

import com.bcbs239.regtech.billing.domain.services.PaymentService;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.valueobjects.PaymentMethodId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeCustomer;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeSubscription;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import org.springframework.stereotype.Service;

/**
 * Infrastructure implementation of PaymentService using Stripe.
 * Bridges the domain interface with the Stripe infrastructure service.
 */
@Service
public class StripePaymentService implements PaymentService {
    
    private final StripeService stripeService;
    
    public StripePaymentService(StripeService stripeService) {
        this.stripeService = stripeService;
    }
    
    @Override
    public Result<CustomerCreationResult> createCustomer(CustomerCreationRequest request) {
        try {
            Result<StripeCustomer> result = stripeService.createCustomer(
                request.email(),
                request.name(),
                request.paymentMethodId()
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
            Result<StripeSubscription> result = stripeService.createSubscription(
                request.customerId(),
                request.tier(),
                request.paymentMethodId()
            );
            
            if (result.isFailure()) {
                return Result.failure(result.getError().get());
            }
            
            StripeSubscription subscription = result.getValue().get();
            SubscriptionCreationResult domainResult = new SubscriptionCreationResult(
                subscription.subscriptionId(),
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
            return stripeService.cancelSubscription(subscriptionId);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_CANCELLATION_FAILED",
                "Failed to cancel subscription: " + e.getMessage(),
                "error.payment.subscriptionCancellationFailed"));
        }
    }
    
    @Override
    public Result<InvoiceCreationResult> createInvoice(InvoiceCreationRequest request) {
        try {
            // Assuming StripeService has a createInvoice method
            Result<com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeInvoice> result = 
                stripeService.createInvoice(request.customerId(), request.amount(), request.description());
            
            if (result.isFailure()) {
                return Result.failure(result.getError().get());
            }
            
            com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeInvoice invoice = result.getValue().get();
            InvoiceCreationResult domainResult = new InvoiceCreationResult(
                invoice.invoiceId(),
                invoice.status(),
                invoice.amount()
            );
            
            return Result.success(domainResult);
            
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("INVOICE_CREATION_FAILED",
                "Failed to create invoice: " + e.getMessage(),
                "error.payment.invoiceCreationFailed"));
        }
    }
    
    @Override
    public Result<Boolean> verifyWebhookSignature(String payload, String signatureHeader) {
        try {
            return stripeService.verifyWebhookSignature(payload, signatureHeader);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("WEBHOOK_VERIFICATION_FAILED",
                "Failed to verify webhook signature: " + e.getMessage(),
                "error.payment.webhookVerificationFailed"));
        }
    }
}