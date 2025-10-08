package com.bcbs239.regtech.billing.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Map;

/**
 * Main configuration properties for the billing module.
 * Maps to the 'billing' section in application-billing.yml
 */
@ConfigurationProperties(prefix = "billing")
public record BillingConfigurationProperties(
    StripeProperties stripe,
    TiersProperties tiers,
    DunningProperties dunning,
    InvoicesProperties invoices,
    BillingCycleProperties billingCycle,
    OutboxProperties outbox,
    SchedulingProperties scheduling,
    NotificationsProperties notifications
) {

    /**
     * Stripe API configuration properties
     */
    public record StripeProperties(
        String apiKey,
        String webhookSecret
    ) {}

    /**
     * Subscription tier configuration properties
     */
    public record TiersProperties(
        StarterTierProperties starter
    ) {}

    /**
     * Starter tier specific properties
     */
    public record StarterTierProperties(
        BigDecimal monthlyPrice,
        String currency,
        int exposureLimit
    ) {}

    /**
     * Dunning process configuration properties
     */
    public record DunningProperties(
        ReminderIntervalsProperties reminderIntervals,
        int finalActionDelay
    ) {}

    /**
     * Dunning reminder intervals configuration
     */
    public record ReminderIntervalsProperties(
        int step1,
        int step2,
        int step3
    ) {}

    /**
     * Invoice configuration properties
     */
    public record InvoicesProperties(
        int dueDays,
        String currency
    ) {}

    /**
     * Billing cycle configuration properties
     */
    public record BillingCycleProperties(
        ZoneId timezone,
        int billingDay
    ) {}

    /**
     * Outbox pattern configuration properties
     */
    public record OutboxProperties(
        boolean enabled,
        long processingInterval,
        long retryInterval,
        int maxRetries,
        long cleanupInterval,
        int cleanupRetentionDays
    ) {}

    /**
     * Scheduling configuration properties
     */
    public record SchedulingProperties(
        MonthlyBillingScheduleProperties monthlyBilling,
        DunningProcessScheduleProperties dunningProcess
    ) {}

    /**
     * Monthly billing schedule configuration
     */
    public record MonthlyBillingScheduleProperties(
        boolean enabled,
        String cron,
        String timezone
    ) {}

    /**
     * Dunning process schedule configuration
     */
    public record DunningProcessScheduleProperties(
        boolean enabled,
        String cron,
        String timezone,
        int threadPoolSize
    ) {}

    /**
     * Notifications configuration properties
     */
    public record NotificationsProperties(
        EmailNotificationProperties email,
        SmsNotificationProperties sms,
        PushNotificationProperties push
    ) {}

    /**
     * Email notification configuration
     */
    public record EmailNotificationProperties(
        boolean enabled,
        Map<String, String> templates
    ) {}

    /**
     * SMS notification configuration
     */
    public record SmsNotificationProperties(
        boolean enabled
    ) {}

    /**
     * Push notification configuration
     */
    public record PushNotificationProperties(
        boolean enabled
    ) {}
}