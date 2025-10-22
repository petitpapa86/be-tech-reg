package com.bcbs239.regtech.core.infrastructure.outbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Configuration
public class FunctionalOutboxConfig {

    @Bean
    public Consumer<String> markAsProcessedFn(Function<OutboxFunctions.MarkAsProcessedRequest, Integer> markAsProcessedFn) {
        return id -> markAsProcessedFn.apply(new OutboxFunctions.MarkAsProcessedRequest(id, Instant.now()));
    }

    @Bean
    public BiConsumer<String, String> markAsFailedFn(Function<OutboxFunctions.MarkAsFailedRequest, Integer> markAsFailedFn) {
        return (id, reason) -> markAsFailedFn.apply(new OutboxFunctions.MarkAsFailedRequest(id, reason));
    }
}