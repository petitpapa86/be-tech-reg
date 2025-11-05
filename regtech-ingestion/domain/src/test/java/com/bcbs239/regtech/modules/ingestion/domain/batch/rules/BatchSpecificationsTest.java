package com.bcbs239.regtech.modules.ingestion.domain.batch.rules;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.batch.*;
import com.bcbs239.regtech.ingestion.domain.batch.rules.BatchSpecifications;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

@DisplayName("BatchSpecifications unit tests")
class BatchSpecificationsTest {

    private FileMetadata sampleMetadata() {
        return new FileMetadata("file.json", "application/json", 1000, "md5", "sha256");
    }

    @Test
    void mustNotBeTerminal_should_succeed_for_uploaded() {
        IngestionBatch batch = new IngestionBatch(BatchId.generate(), BankId.of("B1"), sampleMetadata());
        Result<Void> r = BatchSpecifications.mustNotBeTerminal().isSatisfiedBy(batch);
        assertThat(r.isSuccess()).isTrue();
    }

    @Test
    void mustNotBeTerminal_should_fail_for_completed() {
        IngestionBatch batch = new IngestionBatch(BatchId.generate(), BankId.of("B1"), BatchStatus.COMPLETED,
            sampleMetadata(), null, null, 5, Instant.now(), Instant.now(), null, 0L, 0, null, null, Instant.now());
        Result<Void> r = BatchSpecifications.mustNotBeTerminal().isSatisfiedBy(batch);
        assertThat(r.isFailure()).isTrue();
        assertThat(r.getError().get().getMessage()).contains("terminal");
    }

    @Test
    void mustHaveS3Reference_should_fail_when_missing_and_succeed_when_present() {
        IngestionBatch batch = new IngestionBatch(BatchId.generate(), BankId.of("B1"), sampleMetadata());
        Result<Void> r1 = BatchSpecifications.mustHaveS3Reference().isSatisfiedBy(batch);
        assertThat(r1.isFailure()).isTrue();

        IngestionBatch batchWithS3 = new IngestionBatch(BatchId.generate(), BankId.of("B1"), BatchStatus.UPLOADED,
            sampleMetadata(), S3Reference.of("bucket","key","v1"), null, 1, Instant.now(), null, null, null, 0, null, null, Instant.now());
        Result<Void> r2 = BatchSpecifications.mustHaveS3Reference().isSatisfiedBy(batchWithS3);
        assertThat(r2.isSuccess()).isTrue();
    }

    @Test
    void mustHaveExposureCount_should_fail_for_invalid_and_succeed_for_valid() {
        IngestionBatch batch = new IngestionBatch(BatchId.generate(), BankId.of("B1"), BatchStatus.UPLOADED,
            sampleMetadata(), null, null, null, Instant.now(), null, null, null, 0, null, null, Instant.now());
        Result<Void> r1 = BatchSpecifications.mustHaveExposureCount().isSatisfiedBy(batch);
        assertThat(r1.isFailure()).isTrue();

        IngestionBatch batch2 = new IngestionBatch(BatchId.generate(), BankId.of("B1"), BatchStatus.UPLOADED,
            sampleMetadata(), null, null, 3, Instant.now(), null, null, null, 0, null, null, Instant.now());
        Result<Void> r2 = BatchSpecifications.mustHaveExposureCount().isSatisfiedBy(batch2);
        assertThat(r2.isSuccess()).isTrue();
    }

    @Test
    void mustBeUploaded_should_enforce_uploaded_state() {
        IngestionBatch batch = new IngestionBatch(BatchId.generate(), BankId.of("B1"), sampleMetadata());
        Result<Void> r = BatchSpecifications.mustBeUploaded().isSatisfiedBy(batch);
        assertThat(r.isSuccess()).isTrue();

        IngestionBatch b2 = new IngestionBatch(BatchId.generate(), BankId.of("B1"), BatchStatus.VALIDATED,
            sampleMetadata(), null, null, 1, Instant.now(), null, null, null, 0, null, null, Instant.now());
        Result<Void> r2 = BatchSpecifications.mustBeUploaded().isSatisfiedBy(b2);
        assertThat(r2.isFailure()).isTrue();
    }
}

