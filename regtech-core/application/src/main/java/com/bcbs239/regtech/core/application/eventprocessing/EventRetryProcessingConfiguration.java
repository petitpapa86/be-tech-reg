package com.bcbs239.regtech.core.application.eventprocessing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for event retry processing beans.
 */
@Configuration
public class EventRetryProcessingConfiguration {

    @Bean
    public EventRetryOptions eventRetryOptions() {
        return new EventRetryOptions();
    }
}