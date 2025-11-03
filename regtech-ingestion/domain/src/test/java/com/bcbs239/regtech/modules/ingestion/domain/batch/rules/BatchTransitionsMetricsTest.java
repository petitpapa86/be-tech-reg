package com.bcbs239.regtech.modules.ingestion.domain.batch.rules;

import com.bcbs239.regtech.core.shared.InMemoryTransitionMetrics;
import com.bcbs239.regtech.core.shared.TransitionMetricsHolder;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.batch.BatchId;
import com.bcbs239.regtech.ingestion.domain.batch.BatchStatus;
import com.bcbs239.regtech.ingestion.domain.batch.BatchTransitions;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.batch.IngestionBatch;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

