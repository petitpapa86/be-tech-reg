package com.bcbs239.regtech.billing.infrastructure.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for InputSanitizer functionality.
 */
class InputSanitizerTest {

    private InputSanitizer inputSanitizer;

    @BeforeEach
    void setUp() {
        inputSanitizer = new InputSanitizer();
    }

    @Test
    void sanitizeText_shouldRemoveHtmlTags() {
        String input = "Hello <script>alert('xss')</script> World";
        String result = inputSanitizer.sanitizeText(input);
        
        assertThat(result).isEqualTo("Hello  World");
    }

    @Test
    void sanitizeText_shouldRemoveXssPatterns() {
        String input = "Hello javascript:alert('xss') World";
        String result = inputSanitizer.sanitizeText(input);
        
        assertThat(result).isEqualTo("Hello  World");
    }

    @Test
    void sanitizeText_shouldRemoveNullBytes() {
        String input = "Hello\0World";
        String result = inputSanitizer.sanitizeText(input);
        
        assertThat(result).isEqualTo("HelloWorld");
    }

    @Test
    void sanitizeText_shouldTruncateLongInput() {
        String input = "a".repeat(1500);
        String result = inputSanitizer.sanitizeText(input);
        
        assertThat(result).hasSize(1000);
    }

    @Test
    void sanitizeEmail_shouldValidateCorrectEmail() {
        String email = "test@example.com";
        String result = inputSanitizer.sanitizeEmail(email);
        
        assertThat(result).isEqualTo("test@example.com");
    }

    @Test
    void sanitizeEmail_shouldRejectInvalidEmail() {
        String email = "invalid-email";
        
        assertThatThrownBy(() -> inputSanitizer.sanitizeEmail(email))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email format");
    }

    @Test
    void sanitizeEmail_shouldRejectTooLongEmail() {
        String email = "a".repeat(250) + "@example.com";
        
        assertThatThrownBy(() -> inputSanitizer.sanitizeEmail(email))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("too long");
    }

    @Test
    void sanitizePhoneNumber_shouldValidateCorrectPhone() {
        String phone = "+1-555-123-4567";
        String result = inputSanitizer.sanitizePhoneNumber(phone);
        
        assertThat(result).isEqualTo("+1-555-123-4567");
    }

    @Test
    void sanitizePhoneNumber_shouldRejectInvalidPhone() {
        String phone = "invalid-phone<script>";
        
        assertThatThrownBy(() -> inputSanitizer.sanitizePhoneNumber(phone))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid phone number format");
    }

    @Test
    void sanitizeCurrencyCode_shouldValidateCorrectCode() {
        String currency = "usd";
        String result = inputSanitizer.sanitizeCurrencyCode(currency);
        
        assertThat(result).isEqualTo("USD");
    }

    @Test
    void sanitizeCurrencyCode_shouldRejectInvalidCode() {
        String currency = "invalid";
        
        assertThatThrownBy(() -> inputSanitizer.sanitizeCurrencyCode(currency))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid currency code format");
    }

    @Test
    void sanitizeStripeId_shouldValidateCorrectId() {
        String stripeId = "cus_1234567890abcdef";
        String result = inputSanitizer.sanitizeStripeId(stripeId);
        
        assertThat(result).isEqualTo("cus_1234567890abcdef");
    }

    @Test
    void sanitizeStripeId_shouldRejectInvalidId() {
        String stripeId = "invalid<script>";
        
        assertThatThrownBy(() -> inputSanitizer.sanitizeStripeId(stripeId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid Stripe ID format");
    }

    @Test
    void sanitizeStripeId_shouldRejectTooShortId() {
        String stripeId = "short";
        
        assertThatThrownBy(() -> inputSanitizer.sanitizeStripeId(stripeId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid Stripe ID length");
    }

    @Test
    void sanitizeAlphanumeric_shouldValidateCorrectInput() {
        String input = "Hello World 123";
        String result = inputSanitizer.sanitizeAlphanumeric(input);
        
        assertThat(result).isEqualTo("Hello World 123");
    }

    @Test
    void sanitizeAlphanumeric_shouldRejectInvalidCharacters() {
        String input = "Hello@World!";
        
        assertThatThrownBy(() -> inputSanitizer.sanitizeAlphanumeric(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalid characters");
    }

    @Test
    void validateNoSqlInjection_shouldDetectSqlInjection() {
        String input = "'; DROP TABLE users; --";
        
        assertThatThrownBy(() -> inputSanitizer.validateNoSqlInjection(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dangerous SQL patterns");
    }

    @Test
    void validateMaxLength_shouldEnforceMaxLength() {
        String input = "toolong";
        
        assertThatThrownBy(() -> inputSanitizer.validateMaxLength(input, 5, "test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exceeds maximum length");
    }

    @Test
    void validateMinLength_shouldEnforceMinLength() {
        String input = "short";
        
        assertThatThrownBy(() -> inputSanitizer.validateMinLength(input, 10, "test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be at least");
    }
}
