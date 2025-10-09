package com.bcbs239.regtech.billing.infrastructure.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that required environment variables and configuration are present
 * for the billing module, especially in production environments.
 */
@Component
@Profile("prod")
public class BillingEnvironmentValidator {

    private static final Logger logger = LoggerFactory.getLogger(BillingEnvironmentValidator.class);

    private final Environment environment;
    private final BillingConfigurationProperties properties;

    public BillingEnvironmentValidator(Environment environment, BillingConfigurationProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateEnvironment() {
        logger.info("Validating billing module environment configuration...");
        
        List<String> errors = new ArrayList<>();
        
        // Validate required environment variables
        validateRequiredEnvironmentVariables(errors);
        
        // Validate configuration properties
        validateConfigurationProperties(errors);
        
        // Validate Stripe configuration
        validateStripeConfiguration(errors);
        
        if (!errors.isEmpty()) {
            logger.error("Billing module environment validation failed:");
            errors.forEach(error -> logger.error("  - {}", error));
            throw new IllegalStateException("Billing module environment validation failed. See logs for details.");
        }
        
        logger.info("Billing module environment validation completed successfully");
    }

    private void validateRequiredEnvironmentVariables(List<String> errors) {
        String[] requiredVars = {
            "STRIPE_LIVE_API_KEY",
            "STRIPE_LIVE_WEBHOOK_SECRET",
            "DB_PASSWORD",
            "DB_HOST",
            "DB_NAME",
            "DB_USERNAME"
        };
        
        for (String var : requiredVars) {
            String value = environment.getProperty(var);
            if (value == null || value.trim().isEmpty()) {
                errors.add("Required environment variable not set: " + var);
            }
        }
    }

    private void validateConfigurationProperties(List<String> errors) {
        try {
            // Validate each configuration section
            if (properties.tiers() != null) {
                validateTierConfiguration(errors);
            }
            
            if (properties.dunning() != null) {
                validateDunningConfiguration(errors);
            }
            
            if (properties.invoices() != null) {
                validateInvoiceConfiguration(errors);
            }
            
            if (properties.billingCycle() != null) {
                validateBillingCycleConfiguration(errors);
            }
            
            if (properties.scheduling() != null) {
                validateSchedulingConfiguration(errors);
            }
            
        } catch (Exception e) {
            errors.add("Configuration validation error: " + e.getMessage());
        }
    }

    private void validateStripeConfiguration(List<String> errors) {
        try {
            String apiKey = properties.stripe().apiKey();
            String webhookSecret = properties.stripe().webhookSecret();
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                errors.add("Stripe API key is not configured");
            } else if (apiKey.contains("placeholder") || apiKey.contains("test")) {
                errors.add("Stripe API key appears to be a placeholder or test key in production");
            } else if (!apiKey.startsWith("sk_live_")) {
                errors.add("Stripe API key should start with 'sk_live_' in production environment");
            }
            
            if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
                errors.add("Stripe webhook secret is not configured");
            } else if (webhookSecret.contains("placeholder") || webhookSecret.contains("test")) {
                errors.add("Stripe webhook secret appears to be a placeholder or test value in production");
            } else if (!webhookSecret.startsWith("whsec_")) {
                errors.add("Stripe webhook secret should start with 'whsec_' in production environment");
            }
            
        } catch (Exception e) {
            errors.add("Stripe configuration validation error: " + e.getMessage());
        }
    }

    private void validateTierConfiguration(List<String> errors) {
        try {
            var starter = properties.tiers().starter();
            if (starter.monthlyPrice() == null || starter.monthlyPrice().doubleValue() <= 0) {
                errors.add("STARTER tier monthly price must be positive");
            }
            if (starter.exposureLimit() <= 0) {
                errors.add("STARTER tier exposure limit must be positive");
            }
        } catch (Exception e) {
            errors.add("Tier configuration validation error: " + e.getMessage());
        }
    }

    private void validateDunningConfiguration(List<String> errors) {
        try {
            var intervals = properties.dunning().reminderIntervals();
            if (intervals.step1() <= 0 || intervals.step2() <= 0 || intervals.step3() <= 0) {
                errors.add("All dunning reminder intervals must be positive");
            }
            if (properties.dunning().finalActionDelay() <= 0) {
                errors.add("Dunning final action delay must be positive");
            }
        } catch (Exception e) {
            errors.add("Dunning configuration validation error: " + e.getMessage());
        }
    }

    private void validateInvoiceConfiguration(List<String> errors) {
        try {
            if (properties.invoices().dueDays() <= 0) {
                errors.add("Invoice due days must be positive");
            }
            if (properties.invoices().currency() == null || properties.invoices().currency().trim().isEmpty()) {
                errors.add("Invoice currency must be specified");
            }
        } catch (Exception e) {
            errors.add("Invoice configuration validation error: " + e.getMessage());
        }
    }

    private void validateBillingCycleConfiguration(List<String> errors) {
        try {
            if (properties.billingCycle().timezone() == null) {
                errors.add("Billing cycle timezone must be specified");
            }
            int billingDay = properties.billingCycle().billingDay();
            if (billingDay < 1 || billingDay > 28) {
                errors.add("Billing day must be between 1 and 28");
            }
        } catch (Exception e) {
            errors.add("Billing cycle configuration validation error: " + e.getMessage());
        }
    }

    private void validateSchedulingConfiguration(List<String> errors) {
        try {
            var monthlyBilling = properties.scheduling().monthlyBilling();
            if (monthlyBilling.enabled()) {
                if (monthlyBilling.cron() == null || monthlyBilling.cron().trim().isEmpty()) {
                    errors.add("Monthly billing cron expression is required when enabled");
                }
            }
            
            var dunningProcess = properties.scheduling().dunningProcess();
            if (dunningProcess.enabled()) {
                if (dunningProcess.cron() == null || dunningProcess.cron().trim().isEmpty()) {
                    errors.add("Dunning process cron expression is required when enabled");
                }
                if (dunningProcess.threadPoolSize() <= 0) {
                    errors.add("Dunning process thread pool size must be positive");
                }
            }
        } catch (Exception e) {
            errors.add("Scheduling configuration validation error: " + e.getMessage());
        }
    }
}
