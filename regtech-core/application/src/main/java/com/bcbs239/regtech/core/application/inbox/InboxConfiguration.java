package com.bcbs239.regtech.core.application.inbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(InboxOptions.class)
public class InboxConfiguration {

    // Explicitly expose a bean named 'inboxOptions' so SpEL references like
    // "#{@inboxOptions.getPollInterval().toMillis()}" resolve reliably.
    @Bean(name = "inboxOptions")
    //@ConfigurationProperties(prefix = "inbox")
    public InboxOptions inboxOptions() {
        return new InboxOptions();
    }

}
