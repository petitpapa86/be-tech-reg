package com.bcbs239.regtech.core.application.inbox;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(InboxOptions.class)
public class InboxConfiguration {

    @Bean
    public InboxOptions inboxOptions() {
        return new InboxOptions();
    }
}


