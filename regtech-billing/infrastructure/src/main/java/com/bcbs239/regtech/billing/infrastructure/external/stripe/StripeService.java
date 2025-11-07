package com.bcbs239.regtech.billing.infrastructure.external.stripe;

import com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId;
import com.bcbs239.regtech.billing.domain.payments.PaymentMethodId;
import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.billing.infrastructure.configuration.BillingConfiguration;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private final  BillingConfiguration billingConfiguration;

    public StripeService(BillingConfiguration billingConfiguration) {
        this.billingConfiguration = billingConfiguration;
        // Initialize Stripe SDK with API key from billing configuration (development/test key provided in application.yml)
        try {
            // Prefer explicit configuration from billingConfiguration; fall back to environment variable
            String apiKey = billingConfiguration.stripeConfiguration().apiKey();
            if (apiKey == null || apiKey.isBlank()) {
                apiKey = System.getenv("STRIPE_API_KEY");
            }
            if (apiKey != null && !apiKey.isBlank()) {
                Stripe.apiKey = apiKey;
            }
        } catch (Exception e) {
            // Best effort: if stripe SDK initialization fails, log and continue; createCustomer will fail with a meaningful error
            // Use System.err to avoid dependency on logging configuration during early startup
            System.err.println("Failed to initialize Stripe SDK with configured API key: " + e.getMessage());
        }
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
            return Result.failure(ErrorDetail.of(
                "STRIPE_CUSTOMER_CREATION_FAILED",
                ErrorType.BUSINESS_RULE_ERROR,
                "Failed to create Stripe customer: " + e.getMessage(),
                "stripe.customer.creation.failed"
            ));
        }
    }

    /**
     * Create a new Stripe customer with additional information
     */
    @SuppressWarnings("unused")
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
            return Result.failure(ErrorDetail.of(
                "STRIPE_CUSTOMER_CREATION_FAILED",
                ErrorType.BUSINESS_RULE_ERROR,
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
            return Result.failure(ErrorDetail.of(
                "STRIPE_PAYMENT_METHOD_ATTACH_FAILED",
                ErrorType.BUSINESS_RULE_ERROR,
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
            return Result.failure(ErrorDetail.of(
                "STRIPE_DEFAULT_PAYMENT_METHOD_FAILED",
                ErrorType.BUSINESS_RULE_ERROR,
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
            LocalDate nextMonth = LocalDate.now().plusMonths(1);
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
            return Result.failure(ErrorDetail.of(
                "STRIPE_SUBSCRIPTION_CREATION_FAILED",
                ErrorType.BUSINESS_RULE_ERROR,
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
            return Result.failure(ErrorDetail.of(
                "STRIPE_SUBSCRIPTION_CANCELLATION_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to cancel Stripe subscription: " + e.getMessage(),
                "stripe.subscription.cancellation.failed"
            ));
        }
    }

    /**
     * Cancel a subscription at period end
     */
    @SuppressWarnings("unused")
    public Result<Void> cancelSubscriptionAtPeriodEnd(StripeSubscriptionId subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId.value());
            
            Map<String, Object> params = new HashMap<>();
            params.put("cancel_at_period_end", true);
            
            subscription.update(params);
            
            return Result.success(null);
            
        } catch (StripeException e) {
            return Result.failure(ErrorDetail.of(
                "STRIPE_SUBSCRIPTION_SCHEDULE_CANCELLATION_FAILED",
                ErrorType.SYSTEM_ERROR,
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
            return Result.failure(ErrorDetail.of(
                "STRIPE_INVOICE_CREATION_FAILED",
                ErrorType.SYSTEM_ERROR,
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
                        BigDecimal.valueOf(invoice.getAmountDue()).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP),
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
            return Result.failure(ErrorDetail.of(
                "STRIPE_INVOICE_RETRIEVAL_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to retrieve Stripe invoice: " + e.getMessage(),
                "stripe.invoice.retrieval.failed"
            ));
        }
    }

    @SuppressWarnings("unused")
    public Result<StripeInvoice> finalizeInvoice(StripeInvoiceId invoiceId) {
         // This method may be invoked by external code or webhook processing. Suppress unused-warning if static
         try {
             final Invoice invoice = Invoice.retrieve(invoiceId.value());

             InvoiceFinalizeInvoiceParams params = InvoiceFinalizeInvoiceParams.builder()
                 .setAutoAdvance(true)
                 .build();

             final Invoice finalizedInvoice = invoice.finalizeInvoice(params);

             return StripeCustomerId.fromString(finalizedInvoice.getCustomer())
                 .flatMap(customerId -> {
                     Money amount = Money.of(
                         BigDecimal.valueOf(finalizedInvoice.getAmountDue()).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP),
                         Currency.getInstance(finalizedInvoice.getCurrency().toUpperCase())
                     );

                     return Result.success(new StripeInvoice(
                         invoiceId,
                         customerId,
                         amount,
                         finalizedInvoice.getStatus(),
                         finalizedInvoice.getHostedInvoiceUrl()
                     ));
                 });

         } catch (StripeException e) {
             return Result.failure(ErrorDetail.of(
                 "STRIPE_INVOICE_FINALIZATION_FAILED",
                 ErrorType.SYSTEM_ERROR,
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
            Event event = Webhook.constructEvent(payload, sigHeader, billingConfiguration.stripeConfiguration().webhookSecret());
            return Result.success(event);
            
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of(
                "WEBHOOK_SIGNATURE_VERIFICATION_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to verify webhook signature: " + e.getMessage(),
                "webhook.signature.verification.failed"
            ));
        }
    }

    /**
     * Synchronize invoice status from Stripe event
     */
    public Result<InvoiceStatusUpdate> synchronizeInvoiceStatus(Event event) {
        try {
            if (!isInvoiceEvent(event.getType())) {
                return Result.failure(ErrorDetail.of(
                    "INVALID_INVOICE_EVENT",
                    ErrorType.SYSTEM_ERROR,
                    "Event type is not an invoice event: " + event.getType(),
                    "webhook.invalid.invoice.event"
                ));
            }

            final Invoice stripeInvoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
            if (stripeInvoice == null) {
                return Result.failure(ErrorDetail.of(
                    "INVOICE_DATA_MISSING",
                    ErrorType.SYSTEM_ERROR,
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
                            BigDecimal.valueOf(stripeInvoice.getAmountPaid()).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP),
                            Currency.getInstance(stripeInvoice.getCurrency().toUpperCase())
                        ) : null
                ));
                
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of(
                "INVOICE_STATUS_SYNC_FAILED",
                ErrorType.SYSTEM_ERROR,
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

