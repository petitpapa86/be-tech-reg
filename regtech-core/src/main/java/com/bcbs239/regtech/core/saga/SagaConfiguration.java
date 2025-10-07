package com.bcbs239.regtech.core.saga;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Configuration class for saga infrastructure components.
 * Wires together the saga orchestrator and supporting services.
 */
@Configuration
public class SagaConfiguration {

    private final MessageBus messageBus;
    private final InMemoryBusinessTimeoutService timeoutService;

    public SagaConfiguration(MessageBus messageBus, InMemoryBusinessTimeoutService timeoutService) {
        this.messageBus = messageBus;
        this.timeoutService = timeoutService;
    }

    /**
     * Creates the main saga orchestrator bean
     */
    @Bean
    public SagaOrchestrator sagaOrchestrator(SagaRepository sagaRepository,
                                           MessageBus messageBus,
                                           MonitoringService monitoringService,
                                           BusinessTimeoutService timeoutService) {
        return new SagaOrchestrator(sagaRepository, messageBus, monitoringService, timeoutService);
    }

    /**
     * Starts the message bus when the application starts
     */
    @PostConstruct
    public void startMessageBus() {
        messageBus.start();
    }

    /**
     * Stops services when the application shuts down
     */
    @PreDestroy
    public void stopServices() {
        messageBus.stop();
        timeoutService.shutdown();
    }
}