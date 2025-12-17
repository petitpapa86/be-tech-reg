package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Coordinator for parallel exposure validation.
 * Handles the orchestration of validation across multiple exposures.
 */
@Component
public class ParallelExposureValidationCoordinator {

    private final ExecutorService executor;

    public ParallelExposureValidationCoordinator(
        @Value("${spring.datasource.hikari.maximum-pool-size:20}") int hikariMaxPoolSize,
        @Value("${dataquality.validation.max-in-flight:0}") int configuredMaxInFlight
    ) {
        int safeHikariMaxPoolSize = Math.max(1, hikariMaxPoolSize);
        int safeConfiguredMaxInFlight = Math.max(0, configuredMaxInFlight);

        // IMPORTANT: Virtual threads are cheap, but the DB connection pool is not.
        // Keep a single bounded executor for the whole app so concurrent validation requests
        // don't each spin up their own pool and overwhelm Hikari.
        // Default to half the pool size because rule execution performs best-effort audit writes
        // in REQUIRES_NEW transactions, which can briefly require a second connection per worker.
        int defaultMaxInFlight = Math.max(1, safeHikariMaxPoolSize / 2);
        int maxInFlight = safeConfiguredMaxInFlight > 0
            ? safeConfiguredMaxInFlight
            : defaultMaxInFlight;
        maxInFlight = Math.max(1, maxInFlight);

        this.executor = Executors.newFixedThreadPool(
            maxInFlight,
            Thread.ofVirtual().name("dq-validate-", 0).factory()
        );
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Validates all exposures, using parallel processing for large batches.
     *
     * @param exposures List of exposures to validate
     * @param validator The validator to use
     * @return List of validation results
     */
    public List<ValidationResults> validateAll(
        List<ExposureRecord> exposures,
        ExposureRuleValidator validator
    ) {
        if (exposures.size() < 1_000) {
            // Sequential for small batches
            return exposures.stream()
                .map(validator::validateNoPersist)
                .toList();
        }

        // Parallel for large batches, using a single bounded executor.
        try {
            return executor.invokeAll(
                    exposures.stream()
                        .map(e -> (Callable<ValidationResults>)
                            () -> validator.validateNoPersist(e))
                        .toList()
                ).stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}