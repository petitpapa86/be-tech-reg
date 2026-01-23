package com.bcbs239.regtech.reportgeneration.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Resilience4j configuration for circuit breaker pattern.
 * 
 * Configures circuit breaker for S3 operations with the following behavior:
 * - Failure threshold: 10 consecutive failures OR 50% failure rate over 5-minute window
 * - Wait duration: 5 minutes in OPEN state before transitioning to HALF_OPEN
 * - Permitted calls in HALF_OPEN: 1 test call
 * 
 * Circuit breaker states:
 * - CLOSED: Normal operation, all calls allowed
 * - OPEN: Circuit breaker triggered, all calls blocked immediately
 * - HALF_OPEN: Testing if service recovered, limited calls allowed
 * 
 * Requirements: 22.1, 22.2, 22.3, 22.4, 22.5
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class Resilience4jConfiguration {
    
    /**
     * Configure circuit breaker registry with event listeners for logging.
     * 
     * This bean registers event consumers that log when:
     * - Circuit breaker state changes (CLOSED -> OPEN -> HALF_OPEN -> CLOSED)
     * - Slow calls are detected
     * 
     * @param circuitBreakerRegistry The Resilience4j circuit breaker registry
     * @return Configured registry event consumer
     */
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerRegistryEventConsumer(
            CircuitBreakerRegistry circuitBreakerRegistry) {
        
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                String circuitBreakerName = circuitBreaker.getName();
                
                log.info("Circuit breaker registered [name:{}]", circuitBreakerName);
                
                // Register event listeners for logging
                circuitBreaker.getEventPublisher()
                    .onStateTransition(event -> {
                        log.info("Circuit breaker state transition [name:{},from:{},to:{}]",
                                circuitBreakerName,
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState());
                        
                        switch (event.getStateTransition().getToState()) {
                            case OPEN:
                                log.warn("Circuit breaker OPENED - S3 operations will be blocked [name:{}]", 
                                        circuitBreakerName);
                                break;
                                
                            case HALF_OPEN:
                                log.info("Circuit breaker HALF_OPEN - testing S3 recovery [name:{}]", 
                                        circuitBreakerName);
                                break;
                                
                            case CLOSED:
                                log.info("Circuit breaker CLOSED - S3 operations resumed [name:{}]", 
                                        circuitBreakerName);
                                break;
                                
                            default:
                                break;
                        }
                    })
                    .onError(event -> {
                        log.debug("Circuit breaker recorded error [name:{},exception:{}]",
                                circuitBreakerName,
                                event.getThrowable().getClass().getSimpleName());
                    })
                    .onCallNotPermitted(event -> {
                        log.warn("Circuit breaker blocked call [name:{}]", circuitBreakerName);
                    })
                    .onSlowCallRateExceeded(event -> {
                        log.warn("Circuit breaker slow call rate exceeded [name:{},rate:{}]",
                                circuitBreakerName,
                                event.getSlowCallRate());
                    })
                    .onFailureRateExceeded(event -> {
                        log.warn("Circuit breaker failure rate exceeded [name:{},rate:{}]",
                                circuitBreakerName,
                                event.getFailureRate());
                    });
            }
            
            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
                log.info("Circuit breaker removed [name:{}]", entryRemoveEvent.getRemovedEntry().getName());
            }
            
            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                log.info("Circuit breaker replaced [name:{}]", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }
}
