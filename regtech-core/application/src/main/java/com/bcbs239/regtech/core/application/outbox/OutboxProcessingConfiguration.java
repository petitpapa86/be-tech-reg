package com.bcbs239.regtech.core.application.outbox;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for outbox processing beans.
 */
@Configuration
@EnableConfigurationProperties({OutboxOptions.class})
public class OutboxProcessingConfiguration {

}

