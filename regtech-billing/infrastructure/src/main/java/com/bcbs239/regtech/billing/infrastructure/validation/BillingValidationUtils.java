package com.bcbs239.regtech.billing.infrastructure.validation;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for billing-specific validation operations.
 * Provides validation methods for payment amounts, currencies, Stripe data, and webhook payloads.
 */
@Component
public class BillingValidationUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Supported currencies for billing operations
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("EUR", "USD", "GBP");

    // Stripe ID patterns for validation
    private static final Pattern STRIPE_CUSTOMER_ID_PATTERN = Pattern.compile("^cus_[a-zA-Z0-9]{14,}$");
    private static final Pattern STRIPE_SUBSCRIPTION_ID_PATTERN = Pattern.compile("^sub_[a-zA-Z0-9]{14,}$");
    private static final Pattern STRIPE_INVOICE_ID_PATTERN = Pattern.compile("^in_[a-zA-Z0-9]{14,}$");
    private static final Pattern STRIPE_PAYMENT_METHOD_ID_PATTERN = Pattern.compile("^pm_[a-zA-Z0-9]{14,}$");
    private static final Pattern STRIPE_EVENT_ID_PATTERN = Pattern.compile("^evt_[a-zA-Z0-9]{14,}$");

    // Payment amount constraints
    private static final BigDecimal MIN_PAYMENT_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_PAYMENT_AMOUNT = new BigDecimal("999999.99");

    // Webhook event types that we process
    private static final Set<String> SUPPORTED_WEBHOOK_EVENTS = Set.of(
            "invoice.payment_succeeded",
            "invoice.payment_failed",
            "invoice.created",
            "invoice.finalized",
            "customer.subscription.created",
            "customer.subscription.updated",
            "customer.subscription.deleted",
            "payment_intent.succeeded",
            "payment_intent.payment_failed",
            "cstomer.updated",
            "customer.deleted",
            "charge.refunded",
            "customer.created"
    );

    /**
     * Validate payment amount for billing operations
     */
    public static Result<Void> validatePaymentAmount(BigDecimal amount) {
        if (amount == null) {
            return Result.failure(ErrorDetail.of("PAYMENT_AMOUNT_NULL",
                    "Payment amount cannot be null", "validation.payment.amount.null"));
        }

        if (amount.compareTo(MIN_PAYMENT_AMOUNT) < 0) {
            return Result.failure(ErrorDetail.of("PAYMENT_AMOUNT_TOO_SMALL",
                    String.format("Payment amount must be at least %s", MIN_PAYMENT_AMOUNT),
                    "validation.payment.amount.too.small"));
        }

        if (amount.compareTo(MAX_PAYMENT_AMOUNT) > 0) {
            return Result.failure(ErrorDetail.of("PAYMENT_AMOUNT_TOO_LARGE",
                    String.format("Payment amount cannot exceed %s", MAX_PAYMENT_AMOUNT),
                    "validation.payment.amount.too.large"));
        }

        // Check for reasonable decimal places (max 4 for internal calculations, 2 for display)
        if (amount.scale() > 4) {
            return Result.failure(ErrorDetail.of("PAYMENT_AMOUNT_TOO_PRECISE",
                    "Payment amount cannot have more than 4 decimal places",
                    "validation.payment.amount.too.precise"));
        }

        return Result.success(null);
    }

    /**
     * Validate currency code for billing operations
     */
    public static Result<Currency> validateCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("CURRENCY_CODE_REQUIRED",
                    "Currency code is required", "validation.currency.code.required"));
        }

        String normalizedCode = currencyCode.trim().toUpperCase();

        if (!SUPPORTED_CURRENCIES.contains(normalizedCode)) {
            return Result.failure(ErrorDetail.of("CURRENCY_NOT_SUPPORTED",
                    String.format("Currency %s is not supported. Supported currencies: %s",
                            normalizedCode, SUPPORTED_CURRENCIES),
                    "validation.currency.not.supported"));
        }

        try {
            Currency currency = Currency.getInstance(normalizedCode);
            return Result.success(currency);
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of("INVALID_CURRENCY_CODE",
                    String.format("Invalid currency code: %s", normalizedCode),
                    "validation.currency.invalid"));
        }
    }

    /**
     * Validate Stripe customer ID format
     */
    public static Result<Void> validateStripeCustomerId(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("STRIPE_CUSTOMER_ID_REQUIRED",
                    "Stripe customer ID is required", "validation.stripe.customer.id.required"));
        }

        if (!STRIPE_CUSTOMER_ID_PATTERN.matcher(customerId.trim()).matches()) {
            return Result.failure(ErrorDetail.of("INVALID_STRIPE_CUSTOMER_ID",
                    "Invalid Stripe customer ID format", "validation.stripe.customer.id.invalid"));
        }

        return Result.success(null);
    }

    /**
     * Validate Stripe subscription ID format
     */
    public static Result<Void> validateStripeSubscriptionId(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("STRIPE_SUBSCRIPTION_ID_REQUIRED",
                    "Stripe subscription ID is required", "validation.stripe.subscription.id.required"));
        }

        if (!STRIPE_SUBSCRIPTION_ID_PATTERN.matcher(subscriptionId.trim()).matches()) {
            return Result.failure(ErrorDetail.of("INVALID_STRIPE_SUBSCRIPTION_ID",
                    "Invalid Stripe subscription ID format", "validation.stripe.subscription.id.invalid"));
        }

        return Result.success(null);
    }

    /**
     * Validate Stripe invoice ID format
     */
    public static Result<Void> validateStripeInvoiceId(String invoiceId) {
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("STRIPE_INVOICE_ID_REQUIRED",
                    "Stripe invoice ID is required", "validation.stripe.invoice.id.required"));
        }

        if (!STRIPE_INVOICE_ID_PATTERN.matcher(invoiceId.trim()).matches()) {
            return Result.failure(ErrorDetail.of("INVALID_STRIPE_INVOICE_ID",
                    "Invalid Stripe invoice ID format", "validation.stripe.invoice.id.invalid"));
        }

        return Result.success(null);
    }

    /**
     * Validate Stripe payment method ID format
     */
    public static Result<Void> validateStripePaymentMethodId(String paymentMethodId) {
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("STRIPE_PAYMENT_METHOD_ID_REQUIRED",
                    "Stripe payment method ID is required", "validation.stripe.payment.method.id.required"));
        }

        if (!STRIPE_PAYMENT_METHOD_ID_PATTERN.matcher(paymentMethodId.trim()).matches()) {
            return Result.failure(ErrorDetail.of("INVALID_STRIPE_PAYMENT_METHOD_ID",
                    "Invalid Stripe payment method ID format", "validation.stripe.payment.method.id.invalid"));
        }

        return Result.success(null);
    }

    /**
     * Validate Stripe event ID format
     */
    public static Result<Void> validateStripeEventId(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("STRIPE_EVENT_ID_REQUIRED",
                    "Stripe event ID is required", "validation.stripe.event.id.required"));
        }

        if (!STRIPE_EVENT_ID_PATTERN.matcher(eventId.trim()).matches()) {
            return Result.failure(ErrorDetail.of("INVALID_STRIPE_EVENT_ID",
                    "Invalid Stripe event ID format", "validation.stripe.event.id.invalid"));
        }

        return Result.success(null);
    }

    /**
     * Validate Stripe webhook payload structure and content
     */
    public static Result<JsonNode> validateWebhookPayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("WEBHOOK_PAYLOAD_REQUIRED",
                    "Webhook payload is required", "validation.webhook.payload.required"));
        }

        try {
            JsonNode eventJson = objectMapper.readTree(payload);

            // Validate required fields
            if (!eventJson.has("id")) {
                return Result.failure(ErrorDetail.of("WEBHOOK_MISSING_ID",
                        "Webhook payload missing required 'id' field", "validation.webhook.missing.id"));
            }

            if (!eventJson.has("type")) {
                return Result.failure(ErrorDetail.of("WEBHOOK_MISSING_TYPE",
                        "Webhook payload missing required 'type' field", "validation.webhook.missing.type"));
            }

            if (!eventJson.has("data")) {
                return Result.failure(ErrorDetail.of("WEBHOOK_MISSING_DATA",
                        "Webhook payload missing required 'data' field", "validation.webhook.missing.data"));
            }

            if (!eventJson.has("created")) {
                return Result.failure(ErrorDetail.of("WEBHOOK_MISSING_CREATED",
                        "Webhook payload missing required 'created' field", "validation.webhook.missing.created"));
            }

            // Validate event ID format
            String eventId = eventJson.get("id").asText();
            Result<Void> eventIdValidation = validateStripeEventId(eventId);
            if (eventIdValidation.isFailure()) {
                return Result.failure(eventIdValidation.getError().get());
            }

            // Validate event type
            String eventType = eventJson.get("type").asText();
            if (eventType == null || eventType.trim().isEmpty()) {
                return Result.failure(ErrorDetail.of("WEBHOOK_INVALID_TYPE",
                        "Webhook event type cannot be empty", "validation.webhook.invalid.type"));
            }

            return Result.success(eventJson);

        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("WEBHOOK_PAYLOAD_INVALID_JSON",
                    "Webhook payload is not valid JSON: " + e.getMessage(),
                    "validation.webhook.payload.invalid.json"));
        }
    }

    /**
     * Validate webhook signature header format
     */
    public static Result<Void> validateWebhookSignature(String signatureHeader) {
        if (signatureHeader == null || signatureHeader.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("WEBHOOK_SIGNATURE_REQUIRED",
                    "Webhook signature header is required", "validation.webhook.signature.required"));
        }

        // Stripe signature format: t=timestamp,v1=signature
        if (!signatureHeader.contains("t=") || !signatureHeader.contains("v1=")) {
            return Result.failure(ErrorDetail.of("WEBHOOK_SIGNATURE_INVALID_FORMAT",
                    "Webhook signature header has invalid format", "validation.webhook.signature.invalid.format"));
        }

        return Result.success(null);
    }

    /**
     * Check if webhook event type is supported
     */
    public static boolean isSupportedWebhookEvent(String eventType) {
        return eventType != null && SUPPORTED_WEBHOOK_EVENTS.contains(eventType.trim());
    }

    /**
     * Sanitize string input by trimming and removing potentially harmful characters
     */
    public static String sanitizeStringInput(String input) {
        if (input == null) {
            return null;
        }

        return input.trim()
                .replaceAll("[\\r\\n\\t]", "") // Remove line breaks and tabs
                .replaceAll("[<>\"'&]", ""); // Remove potentially harmful HTML/XML characters
    }

    /**
     * Validate correlation ID format (used in sagas)
     */
    public static Result<Void> validateCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("CORRELATION_ID_REQUIRED",
                    "Correlation ID is required", "validation.correlation.id.required"));
        }

        String sanitized = sanitizeStringInput(correlationId);
        if (sanitized.length() < 3 || sanitized.length() > 100) {
            return Result.failure(ErrorDetail.of("CORRELATION_ID_INVALID_LENGTH",
                    "Correlation ID must be between 3 and 100 characters",
                    "validation.correlation.id.invalid.length"));
        }

        return Result.success(null);
    }

    /**
     * Validate billing account ID format
     */
    public static Result<Void> validateBillingAccountId(String billingAccountId) {
        if (billingAccountId == null || billingAccountId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_ID_REQUIRED",
                    "Billing account ID is required", "validation.billing.account.id.required"));
        }

        String sanitized = sanitizeStringInput(billingAccountId);
        if (sanitized.length() < 3 || sanitized.length() > 50) {
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_ID_INVALID_LENGTH",
                    "Billing account ID must be between 3 and 50 characters",
                    "validation.billing.account.id.invalid.length"));
        }

        return Result.success(null);
    }
}
