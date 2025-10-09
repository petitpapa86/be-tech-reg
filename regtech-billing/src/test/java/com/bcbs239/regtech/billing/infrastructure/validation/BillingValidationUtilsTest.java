package com.bcbs239.regtech.billing.infrastructure.validation;

import com.bcbs239.regtech.core.shared.Result;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for BillingValidationUtils.
 * Verifies all validation methods work correctly with valid and invalid inputs.
 */
class BillingValidationUtilsTest {

    @Test
    @DisplayName("Should validate payment amounts correctly")
    void testValidatePaymentAmount() {
        // Valid amounts
        assertTrue(BillingValidationUtils.validatePaymentAmount(new BigDecimal("10.00")).isSuccess());
        assertTrue(BillingValidationUtils.validatePaymentAmount(new BigDecimal("500.50")).isSuccess());
        assertTrue(BillingValidationUtils.validatePaymentAmount(new BigDecimal("0.01")).isSuccess());
        
        // Invalid amounts
        assertTrue(BillingValidationUtils.validatePaymentAmount(null).isFailure());
        assertTrue(BillingValidationUtils.validatePaymentAmount(new BigDecimal("0.00")).isFailure());
        assertTrue(BillingValidationUtils.validatePaymentAmount(new BigDecimal("-10.00")).isFailure());
        assertTrue(BillingValidationUtils.validatePaymentAmount(new BigDecimal("1000000.00")).isFailure());
        assertTrue(BillingValidationUtils.validatePaymentAmount(new BigDecimal("10.123456")).isFailure());
    }

    @Test
    @DisplayName("Should validate currency codes correctly")
    void testValidateCurrency() {
        // Valid currencies
        Result<Currency> eurResult = BillingValidationUtils.validateCurrency("EUR");
        assertTrue(eurResult.isSuccess());
        assertEquals("EUR", eurResult.getValue().get().getCurrencyCode());
        
        Result<Currency> usdResult = BillingValidationUtils.validateCurrency("usd");
        assertTrue(usdResult.isSuccess());
        assertEquals("USD", usdResult.getValue().get().getCurrencyCode());
        
        // Invalid currencies
        assertTrue(BillingValidationUtils.validateCurrency(null).isFailure());
        assertTrue(BillingValidationUtils.validateCurrency("").isFailure());
        assertTrue(BillingValidationUtils.validateCurrency("XYZ").isFailure());
        assertTrue(BillingValidationUtils.validateCurrency("JPY").isFailure()); // Not supported
    }

    @Test
    @DisplayName("Should validate Stripe customer IDs correctly")
    void testValidateStripeCustomerId() {
        // Valid customer IDs
        assertTrue(BillingValidationUtils.validateStripeCustomerId("cus_1234567890abcd").isSuccess());
        assertTrue(BillingValidationUtils.validateStripeCustomerId("cus_NffrFeUfNV2Hib").isSuccess());
        
        // Invalid customer IDs
        assertTrue(BillingValidationUtils.validateStripeCustomerId(null).isFailure());
        assertTrue(BillingValidationUtils.validateStripeCustomerId("").isFailure());
        assertTrue(BillingValidationUtils.validateStripeCustomerId("invalid_id").isFailure());
        assertTrue(BillingValidationUtils.validateStripeCustomerId("cus_123").isFailure()); // Too short
    }

    @Test
    @DisplayName("Should validate Stripe payment method IDs correctly")
    void testValidateStripePaymentMethodId() {
        // Valid payment method IDs
        assertTrue(BillingValidationUtils.validateStripePaymentMethodId("pm_1234567890abcd").isSuccess());
        assertTrue(BillingValidationUtils.validateStripePaymentMethodId("pm_NffrFeUfNV2Hib").isSuccess());
        
        // Invalid payment method IDs
        assertTrue(BillingValidationUtils.validateStripePaymentMethodId(null).isFailure());
        assertTrue(BillingValidationUtils.validateStripePaymentMethodId("").isFailure());
        assertTrue(BillingValidationUtils.validateStripePaymentMethodId("invalid_id").isFailure());
        assertTrue(BillingValidationUtils.validateStripePaymentMethodId("pm_123").isFailure()); // Too short
    }

    @Test
    @DisplayName("Should validate Stripe event IDs correctly")
    void testValidateStripeEventId() {
        // Valid event IDs
        assertTrue(BillingValidationUtils.validateStripeEventId("evt_1234567890abcd").isSuccess());
        assertTrue(BillingValidationUtils.validateStripeEventId("evt_NffrFeUfNV2Hib").isSuccess());
        
        // Invalid event IDs
        assertTrue(BillingValidationUtils.validateStripeEventId(null).isFailure());
        assertTrue(BillingValidationUtils.validateStripeEventId("").isFailure());
        assertTrue(BillingValidationUtils.validateStripeEventId("invalid_id").isFailure());
        assertTrue(BillingValidationUtils.validateStripeEventId("evt_123").isFailure()); // Too short
    }

    @Test
    @DisplayName("Should validate webhook payloads correctly")
    void testValidateWebhookPayload() {
        // Valid webhook payload
        String validPayload = """
            {
                "id": "evt_1234567890abcd",
                "type": "invoice.payment_succeeded",
                "data": {
                    "object": {
                        "id": "in_1234567890abcd"
                    }
                },
                "created": 1234567890
            }
            """;
        
        Result<JsonNode> validResult = BillingValidationUtils.validateWebhookPayload(validPayload);
        assertTrue(validResult.isSuccess());
        assertEquals("evt_1234567890abcd", validResult.getValue().get().get("id").asText());
        
        // Invalid webhook payloads
        assertTrue(BillingValidationUtils.validateWebhookPayload(null).isFailure());
        assertTrue(BillingValidationUtils.validateWebhookPayload("").isFailure());
        assertTrue(BillingValidationUtils.validateWebhookPayload("invalid json").isFailure());
        assertTrue(BillingValidationUtils.validateWebhookPayload("{}").isFailure()); // Missing required fields
        
        String missingIdPayload = """
            {
                "type": "invoice.payment_succeeded",
                "data": {},
                "created": 1234567890
            }
            """;
        assertTrue(BillingValidationUtils.validateWebhookPayload(missingIdPayload).isFailure());
    }

    @Test
    @DisplayName("Should validate webhook signatures correctly")
    void testValidateWebhookSignature() {
        // Valid webhook signatures
        assertTrue(BillingValidationUtils.validateWebhookSignature("t=1234567890,v1=signature").isSuccess());
        assertTrue(BillingValidationUtils.validateWebhookSignature("t=1234567890,v1=abc123,v0=def456").isSuccess());
        
        // Invalid webhook signatures
        assertTrue(BillingValidationUtils.validateWebhookSignature(null).isFailure());
        assertTrue(BillingValidationUtils.validateWebhookSignature("").isFailure());
        assertTrue(BillingValidationUtils.validateWebhookSignature("invalid_signature").isFailure());
        assertTrue(BillingValidationUtils.validateWebhookSignature("t=1234567890").isFailure()); // Missing v1
        assertTrue(BillingValidationUtils.validateWebhookSignature("v1=signature").isFailure()); // Missing t
    }

    @Test
    @DisplayName("Should check supported webhook events correctly")
    void testIsSupportedWebhookEvent() {
        // Supported events
        assertTrue(BillingValidationUtils.isSupportedWebhookEvent("invoice.payment_succeeded"));
        assertTrue(BillingValidationUtils.isSupportedWebhookEvent("customer.subscription.created"));
        assertTrue(BillingValidationUtils.isSupportedWebhookEvent("payment_intent.succeeded"));
        
        // Unsupported events
        assertFalse(BillingValidationUtils.isSupportedWebhookEvent("unsupported.event"));
        assertFalse(BillingValidationUtils.isSupportedWebhookEvent(null));
        assertFalse(BillingValidationUtils.isSupportedWebhookEvent(""));
    }

    @Test
    @DisplayName("Should sanitize string inputs correctly")
    void testSanitizeStringInput() {
        // Normal strings
        assertEquals("test", BillingValidationUtils.sanitizeStringInput("test"));
        assertEquals("test", BillingValidationUtils.sanitizeStringInput("  test  "));
        
        // Strings with harmful characters
        assertEquals("test", BillingValidationUtils.sanitizeStringInput("test<script>"));
        assertEquals("test", BillingValidationUtils.sanitizeStringInput("test\"'&"));
        assertEquals("test", BillingValidationUtils.sanitizeStringInput("test\r\n\t"));
        
        // Null input
        assertNull(BillingValidationUtils.sanitizeStringInput(null));
    }

    @Test
    @DisplayName("Should validate correlation IDs correctly")
    void testValidateCorrelationId() {
        // Valid correlation IDs
        assertTrue(BillingValidationUtils.validateCorrelationId("user-123-saga").isSuccess());
        assertTrue(BillingValidationUtils.validateCorrelationId("abc").isSuccess()); // Minimum length
        
        // Invalid correlation IDs
        assertTrue(BillingValidationUtils.validateCorrelationId(null).isFailure());
        assertTrue(BillingValidationUtils.validateCorrelationId("").isFailure());
        assertTrue(BillingValidationUtils.validateCorrelationId("ab").isFailure()); // Too short
        assertTrue(BillingValidationUtils.validateCorrelationId("a".repeat(101)).isFailure()); // Too long
    }

    @Test
    @DisplayName("Should validate billing account IDs correctly")
    void testValidateBillingAccountId() {
        // Valid billing account IDs
        assertTrue(BillingValidationUtils.validateBillingAccountId("billing-123").isSuccess());
        assertTrue(BillingValidationUtils.validateBillingAccountId("abc").isSuccess()); // Minimum length
        
        // Invalid billing account IDs
        assertTrue(BillingValidationUtils.validateBillingAccountId(null).isFailure());
        assertTrue(BillingValidationUtils.validateBillingAccountId("").isFailure());
        assertTrue(BillingValidationUtils.validateBillingAccountId("ab").isFailure()); // Too short
        assertTrue(BillingValidationUtils.validateBillingAccountId("a".repeat(51)).isFailure()); // Too long
    }
}
