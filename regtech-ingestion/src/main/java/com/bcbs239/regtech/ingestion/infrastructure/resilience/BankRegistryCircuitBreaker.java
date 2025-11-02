package com.bcbs239.regtech.ingestion.infrastructure.resilience;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.model.BankId;
import com.bcbs239.regtech.ingestion.domain.model.BankInfo;
import com.bcbs239.regtech.ingestion.domain.repository.BankInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit breaker implementation for Bank Registry service calls.
 * Provides fallback to cached bank information when the external service is unavailable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankRegistryCircuitBreaker {
    
    private final BankInfoRepository bankInfoRepository;
    
    @Value("${regtech.bank-registry.circuit-breaker.failure-threshold:5}")
    private int failureThreshold;
    
    @Value("${regtech.bank-registry.circuit-breaker.timeout-duration-seconds:60}")
    private long timeoutDurationSeconds;
    
    @Value("${regtech.bank-registry.circuit-breaker.half-open-max-calls:3}")
    private int halfOpenMaxCalls;
    
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenCallCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
    
    public enum CircuitState {
        CLOSED,    // Normal operation
        OPEN,      // Circuit is open, calls are failing fast
        HALF_OPEN  // Testing if service has recovered
    }
    
    /**
     * Executes a Bank Registry call with circuit breaker protection.
     * Falls back to cached bank information when the circuit is open.
     */
    public Result<BankInfo> callBankRegistryWithFallback(BankId bankId, BankRegistryCall registryCall) {
        log.debug("Calling Bank Registry for bank {} with circuit breaker protection", bankId.value());
        
        CircuitState currentState = getCurrentState();
        
        switch (currentState) {
            case CLOSED -> {
                return executeCall(bankId, registryCall);
            }
            case OPEN -> {
                log.warn("Circuit breaker is OPEN for Bank Registry, using cached fallback for bank {}", bankId.value());
                return useCachedFallback(bankId);
            }
            case HALF_OPEN -> {
                if (halfOpenCallCount.get() < halfOpenMaxCalls) {
                    halfOpenCallCount.incrementAndGet();
                    log.info("Circuit breaker is HALF_OPEN, attempting call {} of {} for bank {}", 
                            halfOpenCallCount.get(), halfOpenMaxCalls, bankId.value());
                    return executeCall(bankId, registryCall);
                } else {
                    log.warn("Circuit breaker HALF_OPEN call limit reached, using cached fallback for bank {}", bankId.value());
                    return useCachedFallback(bankId);
                }
            }
            default -> {
                log.error("Unknown circuit breaker state: {}", currentState);
                return useCachedFallback(bankId);
            }
        }
    }
    
    /**
     * Executes the actual Bank Registry call and handles success/failure.
     */
    private Result<BankInfo> executeCall(BankId bankId, BankRegistryCall registryCall) {
        try {
            Result<BankInfo> result = registryCall.call();
            
            if (result.isSuccess()) {
                onCallSuccess();
                return result;
            } else {
                onCallFailure();
                log.warn("Bank Registry call failed for bank {}: {}", 
                        bankId.value(), result.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                return useCachedFallback(bankId);
            }
            
        } catch (Exception e) {
            onCallFailure();
            log.error("Bank Registry call threw exception for bank {}: {}", bankId.value(), e.getMessage());
            return useCachedFallback(bankId);
        }
    }
    
    /**
     * Uses cached bank information as fallback when Bank Registry is unavailable.
     */
    private Result<BankInfo> useCachedFallback(BankId bankId) {
        log.info("Using cached bank information fallback for bank {}", bankId.value());
        
        Optional<BankInfo> cachedBankInfo = bankInfoRepository.findByBankId(bankId);
        
        if (cachedBankInfo.isPresent()) {
            BankInfo bankInfo = cachedBankInfo.get();
            log.info("Found cached bank information for bank {} (last updated: {})", 
                    bankId.value(), bankInfo.lastUpdated());
            
            // Add warning about using cached data
            return Result.success(bankInfo);
            
        } else {
            log.error("No cached bank information available for bank {}", bankId.value());
            return Result.failure(ErrorDetail.of("BANK_REGISTRY_UNAVAILABLE_NO_CACHE", 
                String.format("Bank Registry service is unavailable and no cached information exists for bank %s. " +
                             "Please ensure the bank is registered and try again later.", bankId.value())));
        }
    }
    
    /**
     * Handles successful call - resets failure count and closes circuit if needed.
     */
    private void onCallSuccess() {
        failureCount.set(0);
        halfOpenCallCount.set(0);
        
        CircuitState currentState = state.get();
        if (currentState == CircuitState.HALF_OPEN) {
            state.set(CircuitState.CLOSED);
            log.info("Circuit breaker transitioned from HALF_OPEN to CLOSED after successful call");
        }
    }
    
    /**
     * Handles failed call - increments failure count and opens circuit if threshold reached.
     */
    private void onCallFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(Instant.now().toEpochMilli());
        
        CircuitState currentState = state.get();
        
        if (currentState == CircuitState.CLOSED && failures >= failureThreshold) {
            state.set(CircuitState.OPEN);
            log.warn("Circuit breaker transitioned from CLOSED to OPEN after {} failures", failures);
        } else if (currentState == CircuitState.HALF_OPEN) {
            state.set(CircuitState.OPEN);
            halfOpenCallCount.set(0);
            log.warn("Circuit breaker transitioned from HALF_OPEN to OPEN after failure");
        }
    }
    
    /**
     * Determines current circuit state based on time and failure count.
     */
    private CircuitState getCurrentState() {
        CircuitState currentState = state.get();
        
        if (currentState == CircuitState.OPEN) {
            long timeSinceLastFailure = Instant.now().toEpochMilli() - lastFailureTime.get();
            long timeoutMs = timeoutDurationSeconds * 1000;
            
            if (timeSinceLastFailure >= timeoutMs) {
                // Transition to HALF_OPEN to test if service has recovered
                if (state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                    halfOpenCallCount.set(0);
                    log.info("Circuit breaker transitioned from OPEN to HALF_OPEN after timeout period");
                    return CircuitState.HALF_OPEN;
                }
            }
        }
        
        return state.get();
    }
    
    /**
     * Gets current circuit breaker metrics for monitoring.
     */
    public CircuitBreakerMetrics getMetrics() {
        return new CircuitBreakerMetrics(
            state.get(),
            failureCount.get(),
            halfOpenCallCount.get(),
            lastFailureTime.get(),
            failureThreshold,
            timeoutDurationSeconds
        );
    }
    
    /**
     * Resets the circuit breaker to CLOSED state (for testing or manual intervention).
     */
    public void reset() {
        state.set(CircuitState.CLOSED);
        failureCount.set(0);
        halfOpenCallCount.set(0);
        lastFailureTime.set(0);
        log.info("Circuit breaker manually reset to CLOSED state");
    }
    
    /**
     * Forces the circuit breaker to OPEN state (for testing or manual intervention).
     */
    public void forceOpen() {
        state.set(CircuitState.OPEN);
        lastFailureTime.set(Instant.now().toEpochMilli());
        log.warn("Circuit breaker manually forced to OPEN state");
    }
    
    /**
     * Functional interface for Bank Registry calls.
     */
    @FunctionalInterface
    public interface BankRegistryCall {
        Result<BankInfo> call() throws Exception;
    }
    
    /**
     * Circuit breaker metrics for monitoring and observability.
     */
    public record CircuitBreakerMetrics(
        CircuitState state,
        int failureCount,
        int halfOpenCallCount,
        long lastFailureTimeMs,
        int failureThreshold,
        long timeoutDurationSeconds
    ) {
        
        public boolean isHealthy() {
            return state == CircuitState.CLOSED && failureCount < failureThreshold;
        }
        
        public String getHealthStatus() {
            return switch (state) {
                case CLOSED -> failureCount == 0 ? "HEALTHY" : "DEGRADED";
                case HALF_OPEN -> "RECOVERING";
                case OPEN -> "UNHEALTHY";
            };
        }
        
        public long getTimeSinceLastFailureSeconds() {
            if (lastFailureTimeMs == 0) {
                return -1;
            }
            return ChronoUnit.SECONDS.between(
                Instant.ofEpochMilli(lastFailureTimeMs),
                Instant.now()
            );
        }
    }
}