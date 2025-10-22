package com.bcbs239.regtech.core.inbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Configuration
public class FunctionalMessageStateConfig {

    @Bean
    public Consumer<String> markAsProcessedFn(InboxMessageOperations repository) {
        return id -> repository.markAsProcessedFn().apply(new InboxMessageOperations.MarkAsProcessedRequest(id, Instant.now()));
    }

    @Bean
    public BiConsumer<String, String> markAsPermanentlyFailedFn(InboxMessageOperations repository) {
        return (id, reason) -> repository.markAsPermanentlyFailedFn().apply(new InboxMessageOperations.MarkAsPermanentlyFailedRequest(id, reason));
    }
}

