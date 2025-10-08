package com.bcbs239.regtech.billing.infrastructure.configuration;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for billing configuration validation without requiring Spring context.
 */
class ConfigurationValidationTest {

    @Test
    void stripeConfiguration_shouldValidateCorrectly() {
        // Valid configuration
        StripeConfiguration validConfig = new StripeConfiguration(
            "sk_live_1234567890abcdef",
            "whsec_1234567890abcdef"
        );
        
        assertThat(validConfig.isLiveMode()).isTrue();
        assertThat(validConfig.isTestMode()).isFalse();
        
        // Should not throw exception
        validConfig.validate();
    }

    @Test
    void stripeConfiguration_shouldDetectTestMode() {
        StripeConfiguration testConfig = new StripeConfiguration(
            "sk_test_1234567890abcdef",
            "whsec_test_1234567890abcdef"
        );
        
        assertThat(testConfig.isTestMode()).isTrue();
        assertThat(testConfig.isLiveMode()).isFalse();
    }

    @Test
    void stripeConfiguration_shouldFailValidationForPlaceholders() {
        StripeConfiguration invalidConfig = new StripeConfiguration(
            "sk_test_placeholder",
            "whsec_placeholder"
        );
        
        assertThatThrownBy(invalidConfig::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("placeholder");
    }

    @Test
    void tierConfiguration_shouldValidateCorrectly() {
        TierConfiguration validConfig = new TierConfiguration(
            new BigDecimal("500.00"),
            "EUR",
            10000
        );
        
        assertThat(validConfig.getStarterMonthlyPrice()).isEqualTo(new BigDecimal("500.00"));
        assertThat(validConfig.getStarterCurrency().getCurrencyCode()).isEqualTo("EUR");
        assertThat(validConfig.getStarterExposureLimit()).isEqualTo(10000);
        
        // Should not throw exception
        validConfig.validate();
    }

    @Test
    void tierConfiguration_shouldFailValidationForInvalidValues() {
        TierConfiguration invalidConfig = new TierConfiguration(
            new BigDecimal("-100.00"),
            "INVALID",
            -1
        );
        
        assertThatThrownBy(invalidConfig::validate)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dunningConfiguration_shouldValidateCorrectly() {
        DunningConfiguration validConfig = new DunningConfiguration(
            7, 14, 21, 30
        );
        
        assertThat(validConfig.getStep1IntervalDays()).isEqualTo(7);
        assertThat(validConfig.getStep2IntervalDays()).isEqualTo(14);
        assertThat(validConfig.getStep3IntervalDays()).isEqualTo(21);
        assertThat(validConfig.getFinalActionDelayDays()).isEqualTo(30);
        assertThat(validConfig.getTotalDunningPeriodDays()).isEqualTo(72);
        
        // Should not throw exception
        validConfig.validate();
    }

    @Test
    void dunningConfiguration_shouldFailValidationForInvalidIntervals() {
        DunningConfiguration invalidConfig = new DunningConfiguration(
            14, 7, 21, 30  // step2 < step1
        );
        
        assertThatThrownBy(invalidConfig::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ascending order");
    }

    @Test
    void invoiceConfiguration_shouldValidateCorrectly() {
        InvoiceConfiguration validConfig = new InvoiceConfiguration(
            14, "EUR"
        );
        
        assertThat(validConfig.getDueDays()).isEqualTo(14);
        assertThat(validConfig.getCurrency().getCurrencyCode()).isEqualTo("EUR");
        assertThat(validConfig.getCurrencyCode()).isEqualTo("EUR");
        
        // Should not throw exception
        validConfig.validate();
    }

    @Test
    void invoiceConfiguration_shouldFailValidationForInvalidValues() {
        InvoiceConfiguration invalidConfig = new InvoiceConfiguration(
            100, "INVALID"  // > 90 days and invalid currency
        );
        
        assertThatThrownBy(invalidConfig::validate)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void billingCycleConfiguration_shouldValidateCorrectly() {
        BillingCycleConfiguration validConfig = new BillingCycleConfiguration(
            ZoneId.of("Europe/Amsterdam"), 1
        );
        
        assertThat(validConfig.getTimezone()).isEqualTo(ZoneId.of("Europe/Amsterdam"));
        assertThat(validConfig.getBillingDay()).isEqualTo(1);
        
        // Should not throw exception
        validConfig.validate();
    }

    @Test
    void billingCycleConfiguration_shouldFailValidationForInvalidDay() {
        BillingCycleConfiguration invalidConfig = new BillingCycleConfiguration(
            ZoneId.of("UTC"), 32  // Invalid day
        );
        
        assertThatThrownBy(invalidConfig::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("between 1 and 28");
    }

    @Test
    void outboxConfiguration_shouldValidateCorrectly() {
        OutboxConfiguration validConfig = new OutboxConfiguration(
            true, 30000L, 120000L, 3, 86400000L, 7
        );
        
        assertThat(validConfig.isEnabled()).isTrue();
        assertThat(validConfig.getProcessingIntervalMs()).isEqualTo(30000L);
        assertThat(validConfig.getRetryIntervalMs()).isEqualTo(120000L);
        assertThat(validConfig.getMaxRetries()).isEqualTo(3);
        assertThat(validConfig.getCleanupIntervalMs()).isEqualTo(86400000L);
        assertThat(validConfig.getCleanupRetentionDays()).isEqualTo(7);
        
        // Should not throw exception
        validConfig.validate();
    }

    @Test
    void outboxConfiguration_shouldFailValidationForInvalidValues() {
        OutboxConfiguration invalidConfig = new OutboxConfiguration(
            true, 500L, 100L, 15, -1L, 0  // Various invalid values
        );
        
        assertThatThrownBy(invalidConfig::validate)
            .isInstanceOf(IllegalStateException.class);
    }
}