package com.bcbs239.regtech.billing.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Configuration properties for Billing module
 * Bound from application-billing.yml
 * Requirements: 6.2, 6.5, 10.3
 */
@Data
@Validated
@ConfigurationProperties(prefix = "billing")
public class BillingProperties {

    @NotNull(message = "Billing enabled flag must be specified")
    private Boolean enabled = true;

    @NotNull(message = "Stripe configuration must be specified")
    private StripeProperties stripe = new StripeProperties();

    @NotNull(message = "Tiers configuration must be specified")
    private TiersProperties tiers = new TiersProperties();

    @NotNull(message = "Dunning configuration must be specified")
    private DunningProperties dunning = new DunningProperties();

    @NotNull(message = "Invoices configuration must be specified")
    private InvoicesProperties invoices = new InvoicesProperties();

    @NotNull(message = "Billing cycle configuration must be specified")
    private BillingCycleProperties billingCycle = new BillingCycleProperties();

    @NotNull(message = "Outbox configuration must be specified")
    private OutboxProperties outbox = new OutboxProperties();

    @NotNull(message = "Scheduling configuration must be specified")
    private SchedulingProperties scheduling = new SchedulingProperties();

    @NotNull(message = "Notifications configuration must be specified")
    private NotificationsProperties notifications = new NotificationsProperties();

    /**
     * Stripe configuration
     */
    @Data
    public static class StripeProperties {
        private String apiKey;
        private String webhookSecret;
    }

    /**
     * Subscription tier configuration
     */
    @Data
    public static class TiersProperties {
        @NotNull(message = "Starter tier configuration must be specified")
        private StarterTierProperties starter = new StarterTierProperties();

        @Data
        public static class StarterTierProperties {
            @NotNull(message = "Monthly price must be specified")
            private BigDecimal monthlyPrice = new BigDecimal("299.99");

            @NotBlank(message = "Currency must be specified")
            private String currency = "USD";

            @Min(value = 1, message = "Exposure limit must be at least 1")
            private int exposureLimit = 10000;
        }
    }

    /**
     * Dunning process configuration
     */
    @Data
    public static class DunningProperties {
        @NotNull(message = "Reminder intervals must be specified")
        private ReminderIntervalsProperties reminderIntervals = new ReminderIntervalsProperties();

        @Min(value = 1, message = "Final action delay must be at least 1 day")
        private int finalActionDelay = 30;

        @Data
        public static class ReminderIntervalsProperties {
            @Min(value = 1, message = "Step 1 interval must be at least 1 day")
            private int step1 = 7;

            @Min(value = 1, message = "Step 2 interval must be at least 1 day")
            private int step2 = 14;

            @Min(value = 1, message = "Step 3 interval must be at least 1 day")
            private int step3 = 21;
        }
    }

    /**
     * Invoice configuration
     */
    @Data
    public static class InvoicesProperties {
        @Min(value = 1, message = "Due days must be at least 1")
        private int dueDays = 30;

        @NotBlank(message = "Currency must be specified")
        private String currency = "USD";
    }

    /**
     * Billing cycle configuration
     */
    @Data
    public static class BillingCycleProperties {
        @NotBlank(message = "Timezone must be specified")
        private String timezone = "America/New_York";

        @Min(value = 1, message = "Billing day must be at least 1")
        private int billingDay = 1;
    }

    /**
     * Outbox pattern configuration
     */
    @Data
    public static class OutboxProperties {
        private boolean enabled = true;

        @Min(value = 1000, message = "Processing interval must be at least 1000 ms")
        private int processingInterval = 30000;

        @Min(value = 1000, message = "Retry interval must be at least 1000 ms")
        private int retryInterval = 60000;

        @Min(value = 0, message = "Max retries must be non-negative")
        private int maxRetries = 3;

        @Min(value = 1000, message = "Cleanup interval must be at least 1000 ms")
        private int cleanupInterval = 86400000;

        @Min(value = 1, message = "Cleanup retention days must be at least 1")
        private int cleanupRetentionDays = 30;
    }

    /**
     * Scheduled job configuration
     */
    @Data
    public static class SchedulingProperties {
        @NotNull(message = "Monthly billing configuration must be specified")
        private MonthlyBillingProperties monthlyBilling = new MonthlyBillingProperties();

        @NotNull(message = "Dunning process configuration must be specified")
        private DunningProcessProperties dunningProcess = new DunningProcessProperties();

        @Data
        public static class MonthlyBillingProperties {
            private boolean enabled = true;

            @NotBlank(message = "Cron expression must be specified")
            private String cron = "0 0 1 * *";

            @NotBlank(message = "Timezone must be specified")
            private String timezone = "America/New_York";
        }

        @Data
        public static class DunningProcessProperties {
            private boolean enabled = true;

            @NotBlank(message = "Cron expression must be specified")
            private String cron = "0 0 2 * *";

            @NotBlank(message = "Timezone must be specified")
            private String timezone = "America/New_York";

            @Min(value = 1, message = "Thread pool size must be at least 1")
            private int threadPoolSize = 5;
        }
    }

    /**
     * Notification configuration
     */
    @Data
    public static class NotificationsProperties {
        @NotNull(message = "Email configuration must be specified")
        private EmailProperties email = new EmailProperties();

        @NotNull(message = "SMS configuration must be specified")
        private SmsProperties sms = new SmsProperties();

        @NotNull(message = "Push configuration must be specified")
        private PushProperties push = new PushProperties();

        @Data
        public static class EmailProperties {
            private boolean enabled = true;

            @NotNull(message = "Email templates must be specified")
            private TemplatesProperties templates = new TemplatesProperties();

            @Data
            public static class TemplatesProperties {
                @NotBlank(message = "Payment reminder template must be specified")
                private String paymentReminder = "payment-reminder.html";

                @NotBlank(message = "Payment failed template must be specified")
                private String paymentFailed = "payment-failed.html";

                @NotBlank(message = "Subscription cancelled template must be specified")
                private String subscriptionCancelled = "subscription-cancelled.html";
            }
        }

        @Data
        public static class SmsProperties {
            private boolean enabled = false;
        }

        @Data
        public static class PushProperties {
            private boolean enabled = false;
        }
    }
}
