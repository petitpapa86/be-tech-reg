package com.bcbs239.regtech.billing.infrastructure.stripe;

import com.bcbs239.regtech.billing.domain.valueobjects.*;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Subscription;
import com.stripe.model.Invoice;
import com.stripe.model.Event;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.InvoiceCreateParams;
import com.stripe.param.InvoiceRetrieveParams;
import com.stripe.param.InvoiceFinalizeInvoiceParams;
import com.stripe.net.Webhook;
import com.stripe.exception.SignatureVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for integrating with Stripe API
 * Handles customer management, subscriptions, and invoice operations
 */
@Service
public class StripeService {

    private final String apiKey;
    private final String webhookSecret;

    public StripeService(@Value("${stripe.api.key}") String apiKey,
                        @Value("${stripe.webhook.secret}") String webhookSecret) {
        this.apiKey = apiKey;
        this.webhookSecret = webhookSecret;
        Stripe.apiKey = apiKey;
    }

    /**
     * Create a new Stripe customer
     */
    public Result<StripeCustomer> createCustomer(String email, String name) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .build();

            Customer customer = Customer.create(params);
            
            return StripeCustomerId.fromString(customer.getId())
                .map(customerId -> new StripeCustomer(customerId, email, name));
                
        } catch (StripeException e) {
            return Result.failure(new ErrorDetail(
                "STRIPE_CUSTOMER_CREATION_FAILED",
                "Failed to create Stripe customer: " + e.getMessage(),
                "stripe.customer.creation.failed"
            ));
        }
    }

    /**
     * Create a new Stripe customer with additional information
     */
    public Result<StripeCustomer> createCustomer(String email, String name, String phone, StripeAddress address) {
        try {
            CustomerCreateParams.Builder paramsBuilder = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name);

            if (phone != null && !phone.trim().isEmpty()) {
                paramsBuilder.setPhone(phone);
            }

            if (address != null) {
                paramsBuilder.setAddress(CustomerCreateParams.Address.builder()
                    .setLine1(address.line1())
                    .setLine2(address.line2())
                    .setCity(address.city())
                    .setState(address.state())
                    .setPostalCode(address.postalCode())
                    .setCountry(address.country())
                    .build());
            }

            Customer customer = Customer.create(paramsBuilder.build());
            
            return StripeCustomerId.fromString(customer.getId())
                .map(customerId -> new StripeCustomer(customerId, email, name));
                
        } catch (StripeException e) {
            return Result.failure(new ErrorDetail(
                "STRIPE_CUSTOMER_CREATION_FAILED",
                "Failed to create Stripe customer: " + e.getMessage(),
                "stripe.customer.creation.failed"
            ));
        }
    }

    /**
     * Address information for Stripe customer creation
     */
    public record StripeAddress(
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country
    ) {}

    /**
     * Attach a payment method to a customer
     */
    public Result<Void> attachPaymentMethod(StripeCustomerId customerId, PaymentMethodId paymentMethodId) {
        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId.value());
            
            PaymentMethodAttachParams params = PaymentMethodAttachParams.builder()
                .setCustomer(customerId.value())
                .build();
                
            paymentMethod.attach(params);
            
            return Result.success(null);
            
        } catch (StripeException e) {
            return Result.failure(new ErrorDetail(
                "STRIPE_PAYMENT_METHOD_ATTACH_FAILED",
                "Failed to attach payment method: " + e.getMessage(),
                "stripe.payment.method.attach.failed"
            ));
        }
    }

    /**
     * Set the default payment method for a customer
     */
    public Result<Void> setDefaultPaymentMethod(StripeCustomerId customerId, PaymentMethodId paymentMethodId) {
        try {
            Customer customer = Customer.retrieve(customerId.value());
            
            CustomerUpdateParams params = CustomerUpdateParams.builder()
                .setInvoiceSettings(
                    CustomerUpdateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(paymentMethodId.value())
                        .build()
                )
                .build();
                
            customer.update(params);
            
            return Result.success(null);
            
        } catch (StripeException e) {
            return Result.failure(new ErrorDetail(
                "STRIPE_DEFAULT_PAYMENT_METHOD_FAILED",
                "Failed to set default payment method: " + e.getMessage(),
                "stripe.default.payment.method.failed"
            ));
        }
    }

    /**
     * Create a subscription with billing anchor to next month start
     */
    public Result<StripeSubscription> createSubscription(StripeCustomerId customerId, SubscriptionTier tier) {
        try {
            // Calculate billing anchor to first day of next month
            LocalDate nextMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1);
            long billingCycleAnchor = nextMonth.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            
            // Create price for the subscription tier
            String priceId = createOrGetPriceId(tier);
            
            SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                .setCustomer(customerId.value())
                .addItem(
                    SubscriptionCreateParams.Item.builder()
                        .setPrice(priceId)
                        .build()
                )
                .setBillingCycleAnchor(billingCycleAnchor)
                .setProrationBehavior(SubscriptionCreateParams.ProrationBehavior.CREATE_PRORATIONS)
                .setCollectionMethod(SubscriptionCreateParams.CollectionMethod.CHARGE_AUTOMATICALLY)
                .build();

            Subscription subscription = Subscription.create(params);
            
            return StripeSubscriptionId.fromString(subscription.getId())
                .flatMap(subscriptionId -> 
                    StripeInvoiceId.fromString(subscription.getLatestInvoice())
                        .map(invoiceId -> new StripeSubscription(
                            subscriptionId,
                            customerId,
                            invoiceId,
                            subscription.getStatus()
                        ))
                );
                
        } catch (StripeException e) {
            return Result.failure(new ErrorDetail(
                "STRIPE_SUBSCRIPTION_CREATION_FAILED",
                "Failed to create Stripe subscription: " + e.getMessage(),
                "stripe.subscription.creation.failed"
            ));
        }
    }

    /**
     * Cancel a subscription
     */
    public Result<Void> cancelSubscription(StripeSubscriptionId subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId.value());
            subscription.cancel();
            
            return Result.success(null);
            
        } catch (StripeException e) {
            return Result.failure(new ErrorDetail(
                "STRIPE_SUBSCRIPTION_CANCELLATION_FAILED",
                "Failed to cancel Stripe subscription: " + e.getMessage(),
                "stripe.subscription.cancellation.failed"
            ));
        }
    }

    /**
     * Cancel a subscription at period end
     */
    public Result<Void> cancelSubscriptionAtPeriodEnd(StripeSubscriptionId subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId.value());
            
            Map<String, Object> params = new HashMap<>();
            params.put("cancel_at_period_end", true);
            
            subscription.update(params);
            
            return Result.success(null);
            
        } catch (StripeException e) {
            return Result.failure(new ErrorDetail(
                "STRIPE_SUBSCRIPTION_SCHEDULE_CANCELLATION_FAILED",
                "Failed to schedule Stripe subscription cancellation: " + e.getMessage(),
                "stripe.subscription.schedule.cancellation.failed"
            ));
        }
    }

    /**
     * Create an invoice for a customer
     */
    public Result<StripeInvoice> createInvoice(StripeCustomerId customerId, Money amount, String description) {
        try {
            InvoiceCreateParams params = InvoiceCreateParams.builder()
                .setCustomer(customerId.value())
                .setCurrency(amount.currency().getCurrencyCode().toLowerCase())
                .setDescription(description)
                .setAutoAdvance(false) // Don't auto-finalize, we'll do it manually
                .build();

            Invoice invoice = Invoice.create(params);
            
            return StripeInvoiceId.fromString(invoice.getId())
                .map(invoiceId -> new StripeInvoice(
                    invoiceId,
                    customerId,
                    amount,
                    invoice.getStatus(),
                    invoice.getHostedInvoiceUrl()
                ));
                
        } catch (StripeException e) {
            return Result.failure(new ErrorDetail(
                "STRIPE_INVOICE_CREATION_FAILED",
                "Failed to create Stripe invoice: " + e.getMessage(),
                "stripe.invoice.creation.failed"
            ));
        }
    }

    /**
     * Retrieve an invoice by ID
     */
    public Result<StripeInvoice> retrieveInvoice(StripeInvoiceId invoiceId) {
        try {
            Invoice invoice = Invoice.retrieve(invoiceId.value());
            
            return StripeCustomerId.fromString(invoice.getCustomer())
                .flatMap(customerId -> {
                    Money amount = Money.of(
                        BigDecimal.valueOf(invoice.getAmountDue()).divide(BigDecimal.valueOf(100)),
                        Currency.getInstance(invoice.getCurrency().toUpperCase())
                    );
                    
                    return Result.success(new StripeInvoice(
                        invoiceId,
                        customerId,
                        amount,
                        invoice.getStatus(),
                        invoice.getHostedInvoiceUrl()
                    ));
                });
                
        } catch (StripeException e) {
            return Result.failure(new ErrorDetail(
                "STRIPE_INVOICE_RETRIEVAL_FAILED",
                "Failed to retrieve Stripe invoice: " + e.getMessage(),
                "stripe.invoice.retrieval.failed"
            ));
        }
    }

    /**
     * Finalize an invoice to make it payable
     */
    public Result<StripeInvoice> finalizeInvoice(StripeInvoiceId invoiceId) {
        try {
            Invoice invoice = Invoice.retrieve(invoiceId.value());
            
            InvoiceFinalizeInvoiceParams params = InvoiceFinalizeInvoiceParams.builder()
                .setAutoAdvance(true)
                .build();
                
            invoice = invoice.finalizeInvoice(params);
            
            return StripeCustomerId.fromString(invoice.getCustomer())
                .flatMap(customerId -> {
                    Money amount = Money.of(
                        BigDecimal.valueOf(invoice.getAmountDue()).divide(BigDecimal.valueOf(100)),
                        Currency.getInstance(invoice.getCurrency().toUpperCase())
                    );
                    
                    return Result.success(new StripeInvoice(
                        invoiceId,
                        customerId,
                        amount,
                        invoice.getStatus(),
                        invoice.getHostedInvoiceUrl()
                    ));
                });
                
        } catch (StripeException e) {
            return Result.failure(new ErrorDetail(
                "STRIPE_INVOICE_FINALIZATION_FAILED",
                "Failed to finalize Stripe invoice: " + e.getMessage(),
                "stripe.invoice.finalization.failed"
            ));
        }
    }

    /**
     * Verify webhook signature and construct event
     */
    public Result<Event> verifyWebhookSignature(String payload, String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            return Result.success(event);
            
        } catch (SignatureVerificationException e) {
            return Result.failure(new ErrorDetail(
                "WEBHOOK_SIGNATURE_VERIFICATION_FAILED",
                "Failed to verify webhook signature: " + e.getMessage(),
                "webhook.signature.verification.failed"
            ));
        } catch (Exception e) {
            return Result.failure(new ErrorDetail(
                "WEBHOOK_EVENT_CONSTRUCTION_FAILED",
                "Failed to construct webhook event: " + e.getMessage(),
                "webhook.event.construction.failed"
            ));
        }
    }

    /**
     * Synchronize invoice status from Stripe event
     */
    public Result<InvoiceStatusUpdate> synchronizeInvoiceStatus(Event event) {
        try {
            if (!isInvoiceEvent(event.getType())) {
                return Result.failure(new ErrorDetail(
                    "INVALID_INVOICE_EVENT",
                    "Event type is not an invoice event: " + event.getType(),
                    "webhook.invalid.invoice.event"
                ));
            }

            Invoice stripeInvoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
            if (stripeInvoice == null) {
                return Result.failure(new ErrorDetail(
                    "INVOICE_DATA_MISSING",
                    "Invoice data is missing from webhook event",
                    "webhook.invoice.data.missing"
                ));
            }

            return StripeInvoiceId.fromString(stripeInvoice.getId())
                .map(invoiceId -> new InvoiceStatusUpdate(
                    invoiceId,
                    stripeInvoice.getStatus(),
                    event.getType(),
                    stripeInvoice.getPaid(),
                    stripeInvoice.getAmountPaid() != null ? 
                        Money.of(
                            BigDecimal.valueOf(stripeInvoice.getAmountPaid()).divide(BigDecimal.valueOf(100)),
                            Currency.getInstance(stripeInvoice.getCurrency().toUpperCase())
                        ) : null
                ));
                
        } catch (Exception e) {
            return Result.failure(new ErrorDetail(
                "INVOICE_STATUS_SYNC_FAILED",
                "Failed to synchronize invoice status: " + e.getMessage(),
                "webhook.invoice.status.sync.failed"
            ));
        }
    }

    /**
     * Check if event type is related to invoices
     */
    private boolean isInvoiceEvent(String eventType) {
        return eventType.startsWith("invoice.") || 
               eventType.equals("payment_intent.succeeded") ||
               eventType.equals("payment_intent.payment_failed");
    }

    /**
     * Create or get existing price ID for subscription tier
     * In a real implementation, this would create Stripe Price objects
     * For now, we'll use hardcoded price IDs that should be created in Stripe dashboard
     */
    private String createOrGetPriceId(SubscriptionTier tier) {
        return switch (tier) {
            case STARTER -> "price_starter_500_eur_monthly"; // This should be created in Stripe dashboard
        };
    }

    /**
     * DTO for invoice status updates from webhooks
     */
    public record InvoiceStatusUpdate(
        StripeInvoiceId invoiceId,
        String status,
        String eventType,
        Boolean paid,
        Money amountPaid
    ) {}
}