package com.bcbs239.regtech.core.application.inbox;

import org.springframework.context.annotation.Configuration;

@Configuration
public class InboxConfiguration {

    // Explicitly expose a bean named 'inboxOptions' and bind it to properties with prefix 'inbox'.
    // This guarantees a bean named 'inboxOptions' is available for SpEL references like
    // "#{@inboxOptions.getPollInterval().toMillis()}" and for constructor injection by name.
//    @Bean(name = "inboxOptions")
//    @ConfigurationProperties(prefix = "inbox")
//    public InboxOptions inboxOptions() {
//        return new InboxOptions();
//    }

}
