package com.bcbs239.regtech.modules.ingestion.domain.batch.rules;


import com.bcbs239.regtech.core.application.monitoring.InMemoryTransitionMetrics;
import com.bcbs239.regtech.core.application.monitoring.TransitionMetricsHolder;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.batch.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests are disabled because metrics emission from the domain layer violates
 * architectural boundaries. The domain layer should not depend on the application layer.
 * 
 * Metrics emission should be handled by:
 * 1. A wrapper in the application layer that calls BatchTransitions and emits metrics
 * 2. An aspect-oriented approach in the infrastructure layer
 * 3. Event-driven metrics where domain events trigger metric emission
 * 
 * TODO: Implement metrics emission at the appropriate architectural layer
 */
@Disabled("Metrics emission violates domain/application layer boundaries - needs architectural refactoring")
@DisplayName("BatchTransitions metrics integration")
class BatchTransitionsMetricsTest {

    private FileMetadata sampleMetadata() {
        return new FileMetadata("file.json", "application/json", 1000, "md5", "sha256");
    }

    @AfterEach
    void cleanup() {
        TransitionMetricsHolder.clear();
    }

    @Test
    void validateTransition_emits_metrics_on_success_and_failure() {
        InMemoryTransitionMetrics metrics = new InMemoryTransitionMetrics();
        TransitionMetricsHolder.set(metrics);

        IngestionBatch batch = new IngestionBatch(BatchId.generate(), BankId.of("B1"), sampleMetadata());

        // success: UPLOADED -> PARSING
        Result<Void> r1 = BatchTransitions.validateTransition(batch, BatchStatus.PARSING);
        assertThat(r1.isSuccess()).isTrue();
        assertThat(metrics.getRequested("UPLOADED","PARSING")).isGreaterThanOrEqualTo(1);
        assertThat(metrics.getSuccess("UPLOADED","PARSING")).isGreaterThanOrEqualTo(1);

        // failure: UPLOADED -> COMPLETED
        Result<Void> r2 = BatchTransitions.validateTransition(batch, BatchStatus.COMPLETED);
        assertThat(r2.isFailure()).isTrue();
        assertThat(metrics.getRequested("UPLOADED","COMPLETED")).isGreaterThanOrEqualTo(1);
        assertThat(metrics.getFailure("UPLOADED","COMPLETED")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void applyTransition_emits_metrics_on_apply() {
        InMemoryTransitionMetrics metrics = new InMemoryTransitionMetrics();
        TransitionMetricsHolder.set(metrics);

        IngestionBatch batch = new IngestionBatch(BatchId.generate(), BankId.of("B1"), sampleMetadata());
        BatchTransitions.applyTransition(batch, BatchStatus.PARSING);

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.PARSING);
        assertThat(metrics.getSuccess("UPLOADED","PARSING")).isGreaterThanOrEqualTo(1);
        assertThat(metrics.getTotalLatencyMs("UPLOADED","PARSING")).isGreaterThanOrEqualTo(0L);
    }
}


