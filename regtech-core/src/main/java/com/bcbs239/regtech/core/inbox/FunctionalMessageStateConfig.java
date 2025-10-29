package com.bcbs239.regtech.core.inbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Configuration
public class FunctionalMessageStateConfig {

    @Bean
    public Consumer<String> markAsProcessingFn(Function<InboxFunctions.MarkAsProcessingRequest, Integer> markAsProcessingCoreFn) {
        return id -> markAsProcessingCoreFn.apply(new InboxFunctions.MarkAsProcessingRequest(id));
    }

    @Bean
    public Consumer<String> markAsProcessedFn(Function<InboxFunctions.MarkAsProcessedRequest, Integer> markAsProcessedCoreFn) {
        return id -> markAsProcessedCoreFn.apply(new InboxFunctions.MarkAsProcessedRequest(id, Instant.now()));
    }

    @Bean
    public BiConsumer<String, String> markAsPermanentlyFailedFn(Function<InboxFunctions.MarkAsPermanentlyFailedRequest, Integer> markAsPermanentlyFailedCoreFn) {
        return (id, reason) -> markAsPermanentlyFailedCoreFn.apply(new InboxFunctions.MarkAsPermanentlyFailedRequest(id, reason));
    }
}

