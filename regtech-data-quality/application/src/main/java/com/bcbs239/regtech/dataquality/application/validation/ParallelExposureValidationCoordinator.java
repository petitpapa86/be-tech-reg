package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Coordinator for parallel exposure validation.
 * Handles the orchestration of validation across multiple exposures.
 */
@Component
public class ParallelExposureValidationCoordinator {

    private final ExecutorService executor;
    private final int maxInFlight;
    private final int parallelThreshold;
    private final int configuredChunkSize;

    public ParallelExposureValidationCoordinator(
        @Value("${spring.datasource.hikari.maximum-pool-size:20}") int hikariMaxPoolSize,
        @Value("${dataquality.validation.max-in-flight:0}") int configuredMaxInFlight,
        @Value("${dataquality.validation.parallel-threshold:1000}") int configuredParallelThreshold,
        @Value("${dataquality.validation.chunk-size:0}") int configuredChunkSize
    ) {
        int safeHikariMaxPoolSize = Math.max(1, hikariMaxPoolSize);
        int safeConfiguredMaxInFlight = Math.max(0, configuredMaxInFlight);

        // IMPORTANT:
        // - The ThreadFactory below creates VIRTUAL threads.
        // - We still BOUND in-flight work because the DB connection pool is the real limiter.
        // Keep a single bounded executor for the whole app so concurrent validation requests
        // don't each spin up their own pool and overwhelm Hikari.
        // Default to (poolSize - 2) to leave some headroom for non-validation work.
        int defaultMaxInFlight = Math.max(1, safeHikariMaxPoolSize - 2);
        int computedMaxInFlight = safeConfiguredMaxInFlight > 0
            ? safeConfiguredMaxInFlight
            : defaultMaxInFlight;

        // Cap to avoid runaway concurrency when pool size is misconfigured.
        // Users can still explicitly override via dataquality.validation.max-in-flight.
        this.maxInFlight = Math.min(computedMaxInFlight, 64);

        this.executor = Executors.newFixedThreadPool(
            this.maxInFlight,
            Thread.ofVirtual().name("dq-validate-", 0).factory()
        );

        this.parallelThreshold = Math.max(1, configuredParallelThreshold);
        // chunk-size=0 => auto-tune chunk size based on batch size.
        this.configuredChunkSize = Math.max(0, configuredChunkSize);
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
     * @return ValidationBatchResult containing the results and exposure results map
     */
    public ValidationBatchResult validateAll(
        List<ExposureRecord> exposures,
        ExposureRuleValidator validator
    ) {
        if (validator == null) {
            throw new IllegalArgumentException("validator must not be null");
        }

        validator.prefetchForBatch(exposures);
        try {
            ConcurrentHashMap<String, ExposureValidationResult> exposureResults = new ConcurrentHashMap<>(
                Math.max(16, (int) (exposures.size() / 0.75f) + 1)
            );
            List<ValidationResults> results = validateAllWith(exposures, validator.prepareForBatch(), exposureResults);
            return new ValidationBatchResult(results, exposureResults);
        } finally {
            validator.onBatchComplete();
        }
    }

    private List<ValidationResults> validateAllWith(
        List<ExposureRecord> exposures,
        Function<ExposureRecord, ValidationResults> batchValidator,
        ConcurrentHashMap<String, ExposureValidationResult> exposureResults
    ) {
        if (exposures == null || exposures.isEmpty()) {
            return List.of();
        }
        if (batchValidator == null) {
            throw new IllegalArgumentException("batchValidator must not be null");
        }

        if (exposures.size() < parallelThreshold) {
            // Sequential for small batches
            List<ValidationResults> results = exposures.stream()
                .map(batchValidator)
                .toList();
            for (ValidationResults result : results) {
                if (result != null) {
                    exposureResults.put(result.exposureId(), createExposureValidationResult(result));
                }
            }
            return results;
        }

        // Parallel for large batches, using a single bounded executor.
        // Validate in chunks to keep task counts and per-task overhead reasonable.
        int total = exposures.size();
        // If chunk size isn't configured, target roughly one chunk per worker.
        int effectiveChunkSize = configuredChunkSize > 0
            ? configuredChunkSize
            : Math.max(1, (total + maxInFlight - 1) / maxInFlight);

        int taskCount = (total + effectiveChunkSize - 1) / effectiveChunkSize;

        try {
            List<Callable<List<ValidationResults>>> tasks = java.util.stream.IntStream.range(0, taskCount)
                .mapToObj(taskIndex -> (Callable<List<ValidationResults>>) () -> {
                    int from = taskIndex * effectiveChunkSize;
                    int to = Math.min(total, from + effectiveChunkSize);
                    List<ValidationResults> results = new java.util.ArrayList<>(to - from);
                    for (int i = from; i < to; i++) {
                        ValidationResults result = batchValidator.apply(exposures.get(i));
                        results.add(result);
                        if (result != null) {
                            exposureResults.put(result.exposureId(), createExposureValidationResult(result));
                        }
                    }
                    return results;
                })
                .toList();

            return executor.invokeAll(tasks).stream()
                .flatMap(f -> {
                    try {
                        return f.get().stream();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof RuntimeException runtimeException) {
                            throw runtimeException;
                        }
                        if (cause instanceof Error error) {
                            throw error;
                        }
                        throw new RuntimeException(cause == null ? e : cause);
                    }
                })
                .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private ExposureValidationResult createExposureValidationResult(ValidationResults validationResults) {
        List<ValidationError> errors = validationResults.validationErrors();

        // Group errors by dimension
        Map<QualityDimension, List<ValidationError>> dimensionErrors = new HashMap<>();
        for (QualityDimension dimension : QualityDimension.values()) {
            dimensionErrors.put(dimension, new ArrayList<>());
        }
        for (ValidationError error : errors) {
            dimensionErrors.get(error.dimension()).add(error);
        }

        return ExposureValidationResult.builder()
                .exposureId(validationResults.exposureId())
                .errors(errors)
                .dimensionErrors(dimensionErrors)
                .isValid(errors.isEmpty())
                .build();
    }
}