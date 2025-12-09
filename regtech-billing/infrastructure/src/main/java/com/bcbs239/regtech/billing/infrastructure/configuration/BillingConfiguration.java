package com.bcbs239.regtech.billing.infrastructure.configuration;

import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * Main configuration class for the billing module.
 * Enables configuration properties and provides configuration beans.
 */
@Configuration
@EnableConfigurationProperties(BillingConfigurationProperties.class)
@SuppressWarnings("unused")
public class BillingConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(BillingConfiguration.class);

    private final BillingConfigurationProperties properties;
    private final Environment environment;
    private final JpaBillingAccountRepository billingAccountRepository;
    private final JpaSubscriptionRepository subscriptionRepository;

    public BillingConfiguration(
            BillingConfigurationProperties properties,
            Environment environment,
            JpaBillingAccountRepository billingAccountRepository,
            JpaSubscriptionRepository subscriptionRepository) {
        this.properties = properties;
        this.environment = environment;
        this.billingAccountRepository = billingAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @SuppressWarnings("unused")
    public void logConfigurationOnStartup() {
        String[] activeProfiles = environment.getActiveProfiles();
        String profileInfo = activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "default";
        
        logger.info("Billing module initialized with profiles: [{}]", profileInfo);
        logger.info("Stripe mode: {}", properties.stripe().apiKey().startsWith("sk_live_") ? "LIVE" : "TEST");
        logger.info("Outbox processing: {}", properties.outbox().enabled() ? "ENABLED" : "DISABLED");
        logger.info("Monthly billing schedule: {}", properties.scheduling().monthlyBilling().enabled() ? "ENABLED" : "DISABLED");
        logger.info("Dunning process schedule: {}", properties.scheduling().dunningProcess().enabled() ? "ENABLED" : "DISABLED");
    }

     /**
      * Provides access to Stripe configuration
      */
     @SuppressWarnings("unused")
     @Bean
     public StripeConfiguration stripeConfiguration() {
         return new StripeConfiguration(
             properties.stripe().apiKey(),
             properties.stripe().webhookSecret()
         );
     }

     /**
      * Provides access to subscription tier configuration
      */
     @SuppressWarnings("unused")
     @Bean
     public TierConfiguration tierConfiguration() {
        var starter = properties.tiers().starter();
        return new TierConfiguration(
            starter.monthlyPrice(),
            starter.currency(),
            starter.exposureLimit()
        );
    }

    /**
     * Provides access to dunning process configuration
     */
     @SuppressWarnings("unused")
     @Bean
     public DunningConfiguration dunningConfiguration() {
        var intervals = properties.dunning().reminderIntervals();
        return new DunningConfiguration(
            intervals.step1(),
            intervals.step2(),
            intervals.step3(),
            properties.dunning().finalActionDelay()
        );
    }

    /**
     * Provides access to invoice configuration
     */
     @SuppressWarnings("unused")
     @Bean
     public InvoiceConfiguration invoiceConfiguration() {
        return new InvoiceConfiguration(
            properties.invoices().dueDays(),
            properties.invoices().currency()
        );
    }

    /**
     * Provides access to billing cycle configuration
     */
     @SuppressWarnings("unused")
     @Bean
     public BillingCycleConfiguration billingCycleConfiguration() {
        return new BillingCycleConfiguration(
            properties.billingCycle().timezone(),
            properties.billingCycle().billingDay()
        );
    }



    /**
     * Provides access to scheduling configuration
     */
     @SuppressWarnings("unused")
     @Bean
     public SchedulingConfiguration schedulingConfiguration() {
        var monthlyBilling = properties.scheduling().monthlyBilling();
        var dunningProcess = properties.scheduling().dunningProcess();
        
        return new SchedulingConfiguration(
            new SchedulingConfiguration.MonthlyBillingSchedule(
                monthlyBilling.enabled(),
                monthlyBilling.cron(),
                monthlyBilling.timezone()
            ),
            new SchedulingConfiguration.DunningProcessSchedule(
                dunningProcess.enabled(),
                dunningProcess.cron(),
                dunningProcess.timezone(),
                dunningProcess.threadPoolSize()
            )
        );
    }

    /**
     * Provides access to notifications configuration
     */
     @SuppressWarnings("unused")
     @Bean
     public NotificationsConfiguration notificationsConfiguration() {
        var email = properties.notifications().email();
        var sms = properties.notifications().sms();
        var push = properties.notifications().push();
        
        return new NotificationsConfiguration(
            new NotificationsConfiguration.EmailNotification(
                email.enabled(),
                email.templates()
            ),
            new NotificationsConfiguration.SmsNotification(sms.enabled()),
            new NotificationsConfiguration.PushNotification(push.enabled())
        );
    }
}

