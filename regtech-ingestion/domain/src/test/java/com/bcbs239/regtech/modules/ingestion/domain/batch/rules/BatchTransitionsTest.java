package com.bcbs239.regtech.modules.ingestion.domain.batch.rules;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.modules.ingestion.domain.batch.BatchId;
import com.bcbs239.regtech.modules.ingestion.domain.batch.BatchStatus;
import com.bcbs239.regtech.modules.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.modules.ingestion.domain.batch.IngestionBatch;
import com.bcbs239.regtech.modules.ingestion.domain.bankinfo.BankId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BatchTransitions unit tests")
class BatchTransitionsTest {

    private FileMetadata sampleMetadata() {
        return new FileMetadata("file.json", "application/json", 1000, "md5", "sha256");
    }

    @Test
    void validateTransition_should_allow_uploaded_to_parsing() {
        IngestionBatch batch = new IngestionBatch(BatchId.generate(), BankId.of("B1"), sampleMetadata());
        Result<Void> r = com.bcbs239.regtech.modules.ingestion.domain.batch.BatchTransitions.validateTransition(batch, BatchStatus.PARSING);
        assertThat(r.isSuccess()).isTrue();
    }

    @Test
    void validateTransition_should_reject_invalid_transitions() {
        IngestionBatch batch = new IngestionBatch(BatchId.generate(), BankId.of("B1"), sampleMetadata());
        // cannot go directly to COMPLETED from UPLOADED
        Result<Void> r = com.bcbs239.regtech.modules.ingestion.domain.batch.BatchTransitions.validateTransition(batch, BatchStatus.COMPLETED);
        assertThat(r.isFailure()).isTrue();
    }

    @Test
    void applyTransition_should_update_status() {
        IngestionBatch batch = new IngestionBatch(BatchId.generate(), BankId.of("B1"), sampleMetadata());
        com.bcbs239.regtech.modules.ingestion.domain.batch.BatchTransitions.applyTransition(batch, BatchStatus.PARSING);
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.PARSING);
    }
}
