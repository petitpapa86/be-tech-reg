package com.bcbs239.regtech.reportgeneration.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
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
 * - Emits metrics for state transitions and circuit breaker events
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
    
    private final MeterRegistry meterRegistry;
    
    /**
     * Configure circuit breaker registry with event listeners for metrics emission.
     * 
     * This bean registers event consumers that emit metrics when:
     * - Circuit breaker state changes (CLOSED -> OPEN -> HALF_OPEN -> CLOSED)
     * - Calls succeed or fail
     * - Slow calls are detected
     * 
     * Metrics emitted:
     * - report.s3.circuit.breaker.state (gauge): Current state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
     * - report.s3.circuit.breaker.calls (counter): Total calls with tags for result
     * - report.s3.circuit.breaker.transitions (counter): State transitions
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
                
                // Register event listeners for metrics emission
                circuitBreaker.getEventPublisher()
                    .onStateTransition(event -> {
                        log.info("Circuit breaker state transition [name:{},from:{},to:{}]",
                                circuitBreakerName,
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState());
                        
                        // Emit state transition metric
                        meterRegistry.counter("report.s3.circuit.breaker.transitions",
                                List.of(
                                    Tag.of("circuit_breaker", circuitBreakerName),
                                    Tag.of("from_state", event.getStateTransition().getFromState().name()),
                                    Tag.of("to_state", event.getStateTransition().getToState().name())
                                )
                        ).increment();
                        
                        // Emit specific metrics for important transitions
                        switch (event.getStateTransition().getToState()) {
                            case OPEN:
                                log.warn("Circuit breaker OPENED - S3 operations will be blocked [name:{}]", 
                                        circuitBreakerName);
                                meterRegistry.counter("report.s3.circuit.breaker.open",
                                        List.of(Tag.of("circuit_breaker", circuitBreakerName))
                                ).increment();
                                break;
                                
                            case HALF_OPEN:
                                log.info("Circuit breaker HALF_OPEN - testing S3 recovery [name:{}]", 
                                        circuitBreakerName);
                                meterRegistry.counter("report.s3.circuit.breaker.half_open",
                                        List.of(Tag.of("circuit_breaker", circuitBreakerName))
                                ).increment();
                                break;
                                
                            case CLOSED:
                                log.info("Circuit breaker CLOSED - S3 operations resumed [name:{}]", 
                                        circuitBreakerName);
                                meterRegistry.counter("report.s3.circuit.breaker.closed",
                                        List.of(Tag.of("circuit_breaker", circuitBreakerName))
                                ).increment();
                                break;
                                
                            default:
                                // Other states (DISABLED, FORCED_OPEN, METRICS_ONLY)
                                break;
                        }
                        
                        // Update state gauge
                        updateStateGauge(circuitBreakerName, event.getStateTransition().getToState().name());
                    })
                    .onSuccess(event -> {
                        // Emit success metric
                        meterRegistry.counter("report.s3.circuit.breaker.calls",
                                List.of(
                                    Tag.of("circuit_breaker", circuitBreakerName),
                                    Tag.of("result", "success")
                                )
                        ).increment();
                    })
                    .onError(event -> {
                        // Emit error metric
                        meterRegistry.counter("report.s3.circuit.breaker.calls",
                                List.of(
                                    Tag.of("circuit_breaker", circuitBreakerName),
                                    Tag.of("result", "error"),
                                    Tag.of("exception", event.getThrowable().getClass().getSimpleName())
                                )
                        ).increment();
                        
                        log.debug("Circuit breaker recorded error [name:{},exception:{}]",
                                circuitBreakerName,
                                event.getThrowable().getClass().getSimpleName());
                    })
                    .onCallNotPermitted(event -> {
                        // Emit blocked call metric
                        meterRegistry.counter("report.s3.circuit.breaker.calls",
                                List.of(
                                    Tag.of("circuit_breaker", circuitBreakerName),
                                    Tag.of("result", "blocked")
                                )
                        ).increment();
                        
                        log.warn("Circuit breaker blocked call [name:{}]", circuitBreakerName);
                    })
                    .onSlowCallRateExceeded(event -> {
                        // Emit slow call rate exceeded metric
                        meterRegistry.counter("report.s3.circuit.breaker.slow_calls",
                                List.of(Tag.of("circuit_breaker", circuitBreakerName))
                        ).increment();
                        
                        log.warn("Circuit breaker slow call rate exceeded [name:{},rate:{}]",
                                circuitBreakerName,
                                event.getSlowCallRate());
                    })
                    .onFailureRateExceeded(event -> {
                        // Emit failure rate exceeded metric
                        meterRegistry.counter("report.s3.circuit.breaker.failure_rate_exceeded",
                                List.of(Tag.of("circuit_breaker", circuitBreakerName))
                        ).increment();
                        
                        log.warn("Circuit breaker failure rate exceeded [name:{},rate:{}]",
                                circuitBreakerName,
                                event.getFailureRate());
                    });
                
                // Initialize state gauge
                updateStateGauge(circuitBreakerName, circuitBreaker.getState().name());
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
    
    /**
     * Update the circuit breaker state gauge metric.
     * 
     * State values:
     * - CLOSED: 0
     * - OPEN: 1
     * - HALF_OPEN: 2
     * - DISABLED: 3
     * - FORCED_OPEN: 4
     * - METRICS_ONLY: 5
     * 
     * @param circuitBreakerName The name of the circuit breaker
     * @param state The current state
     */
    private void updateStateGauge(String circuitBreakerName, String state) {
        double stateValue = switch (state) {
            case "CLOSED" -> 0.0;
            case "OPEN" -> 1.0;
            case "HALF_OPEN" -> 2.0;
            case "DISABLED" -> 3.0;
            case "FORCED_OPEN" -> 4.0;
            case "METRICS_ONLY" -> 5.0;
            default -> -1.0;
        };
        
        meterRegistry.gauge("report.s3.circuit.breaker.state",
                List.of(
                    Tag.of("circuit_breaker", circuitBreakerName),
                    Tag.of("state", state)
                ),
                stateValue
        );
    }
}
