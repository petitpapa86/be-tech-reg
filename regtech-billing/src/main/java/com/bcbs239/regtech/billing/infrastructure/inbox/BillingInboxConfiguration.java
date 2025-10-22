package com.bcbs239.regtech.billing.infrastructure.inbox;

import com.bcbs239.regtech.core.inbox.InboxMessageOperations;
import com.bcbs239.regtech.core.inbox.InboxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for inbox processing in the billing bounded context.
 * The core module handles inbox processing centrally, so this configuration
 * only provides billing-specific options.
 */
@Configuration
public class BillingInboxConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(BillingInboxConfiguration.class);

    /**
     * Configure inbox processing options for billing context.
     */
    @Bean
    public InboxOptions billingInboxOptions() {
        return new InboxOptions(50, Duration.ofSeconds(30), "billing", true);
    }
}