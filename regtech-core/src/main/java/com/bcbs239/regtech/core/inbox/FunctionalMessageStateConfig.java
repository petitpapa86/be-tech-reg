package com.bcbs239.regtech.core.inbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Configuration
public class FunctionalMessageStateConfig {

    @Bean
    public Consumer<String> markAsProcessedFn(InboxMessageJpaRepository repository) {
        return id -> repository.markAsProcessed(id, Instant.now());
    }

    @Bean
    public BiConsumer<String, String> markAsPermanentlyFailedFn(InboxMessageJpaRepository repository) {
        return (id, reason) -> repository.markAsPermanentlyFailed(id, reason);
    }
}

