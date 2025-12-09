package com.bcbs239.regtech.reportgeneration.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for Resilience4j circuit breaker configuration.
 * 
 * Verifies that:
 * - Circuit breaker is properly configured with correct thresholds
 * - State transitions work correctly
 * - Circuit breaker blocks calls when open
 */
class Resilience4jConfigurationTest {
    
    @Test
    void shouldConfigureCircuitBreakerRegistry() {
        // Given: Circuit breaker registry with default configuration
        // When: We create a registry
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        
        // Then: Registry should be available
        assertThat(registry).isNotNull();
    }
    
    @Test
    void shouldConfigureS3UploadCircuitBreakerWithCorrectSettings() {
        // Given: A circuit breaker configuration for S3 upload
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(5))
                .permittedNumberOfCallsInHalfOpenState(1)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        
        // When: We create a circuit breaker with this config
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker circuitBreaker = registry.circuitBreaker("s3-upload");
        
        // Then: Circuit breaker should have correct configuration
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getName()).isEqualTo("s3-upload");
        assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(10);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState().apply(1))
                .isEqualTo(Duration.ofMinutes(5).toMillis());
        assertThat(circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState())
                .isEqualTo(1);
        assertThat(circuitBreaker.getCircuitBreakerConfig().isAutomaticTransitionFromOpenToHalfOpenEnabled())
                .isTrue();
    }
    
    @Test
    void shouldEmitMetricsOnStateTransition() {
        // Given: A circuit breaker with metrics
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(100))
                .build();
        
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker circuitBreaker = registry.circuitBreaker("test-cb");
        
        // Register event listener to track state transitions
        final boolean[] stateTransitionOccurred = {false};
        circuitBreaker.getEventPublisher().onStateTransition(event -> {
            stateTransitionOccurred[0] = true;
        });
        
        // When: We trigger failures to open the circuit
        try {
            circuitBreaker.executeSupplier(() -> {
                throw new RuntimeException("Test failure 1");
            });
        } catch (Exception ignored) {}
        
        try {
            circuitBreaker.executeSupplier(() -> {
                throw new RuntimeException("Test failure 2");
            });
        } catch (Exception ignored) {}
        
        // Then: State transition should have occurred
        assertThat(stateTransitionOccurred[0]).isTrue();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
    
    @Test
    void shouldBlockCallsWhenCircuitBreakerIsOpen() {
        // Given: A circuit breaker that is open
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build();
        
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker circuitBreaker = registry.circuitBreaker("test-cb-blocking");
        
        // Trigger failures to open the circuit
        try {
            circuitBreaker.executeSupplier(() -> {
                throw new RuntimeException("Test failure 1");
            });
        } catch (Exception ignored) {}
        
        try {
            circuitBreaker.executeSupplier(() -> {
                throw new RuntimeException("Test failure 2");
            });
        } catch (Exception ignored) {}
        
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        
        // When: We try to execute a call while circuit is open
        boolean callBlocked = false;
        try {
            circuitBreaker.executeSupplier(() -> "This should be blocked");
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
            callBlocked = true;
        }
        
        // Then: Call should be blocked
        assertThat(callBlocked).isTrue();
    }
    
    @Test
    void shouldTransitionToHalfOpenAfterWaitDuration() throws InterruptedException {
        // Given: A circuit breaker with short wait duration
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(200))
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker circuitBreaker = registry.circuitBreaker("test-cb-half-open");
        
        // Trigger failures to open the circuit
        try {
            circuitBreaker.executeSupplier(() -> {
                throw new RuntimeException("Test failure 1");
            });
        } catch (Exception ignored) {}
        
        try {
            circuitBreaker.executeSupplier(() -> {
                throw new RuntimeException("Test failure 2");
            });
        } catch (Exception ignored) {}
        
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        
        // When: We wait for the wait duration to pass
        Thread.sleep(300);
        
        // Then: Circuit breaker should transition to HALF_OPEN
        // Note: Transition happens on next call attempt
        try {
            circuitBreaker.executeSupplier(() -> "Test call");
        } catch (Exception ignored) {}
        
        assertThat(circuitBreaker.getState()).isIn(
                CircuitBreaker.State.HALF_OPEN, 
                CircuitBreaker.State.CLOSED
        );
    }
}
