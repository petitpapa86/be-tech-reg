package com.bcbs239.regtech.ingestion.infrastructure.observability;

import com.bcbs239.regtech.ingestion.domain.batch.BatchProcessingCompletedEvent;
import com.bcbs239.regtech.ingestion.domain.batch.BatchProcessingStartedEvent;
import com.bcbs239.regtech.ingestion.domain.batch.BatchStoredEvent;
import com.bcbs239.regtech.ingestion.domain.batch.BatchUploadedEvent;
import com.bcbs239.regtech.ingestion.domain.batch.BatchValidatedEvent;
import com.bcbs239.regtech.ingestion.domain.batch.events.BatchProcessingFailedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IngestionMetricsListener {

    private final MeterRegistry meterRegistry;

    @EventListener
    public void onBatchUploaded(BatchUploadedEvent event) {
        log.debug("Recording metrics for BatchUploadedEvent: {}", event.batchId().value());
        meterRegistry.counter("ingestion.batch.uploaded",
                List.of(Tag.of("bank_id", event.bankId().value()))
        ).increment();
    }

    @EventListener
    public void onBatchProcessingStarted(BatchProcessingStartedEvent event) {
        log.debug("Recording metrics for BatchProcessingStartedEvent: {}", event.batchId().value());
        meterRegistry.counter("ingestion.batch.started",
                List.of(Tag.of("bank_id", event.bankId().value()))
        ).increment();
    }

    @EventListener
    public void onBatchValidated(BatchValidatedEvent event) {
        log.debug("Recording metrics for BatchValidatedEvent: {}", event.batchId().value());
        meterRegistry.counter("ingestion.batch.validated",
                List.of(Tag.of("bank_id", event.bankId().value()))
        ).increment();
        
        meterRegistry.summary("ingestion.batch.exposures")
                .record(event.exposureCount());
    }

    @EventListener
    public void onBatchStored(BatchStoredEvent event) {
        log.debug("Recording metrics for BatchStoredEvent: {}", event.batchId().value());
        meterRegistry.counter("ingestion.batch.stored",
                List.of(Tag.of("bank_id", event.bankId().value()))
        ).increment();
    }

    @EventListener
    public void onBatchProcessingCompleted(BatchProcessingCompletedEvent event) {
        log.debug("Recording metrics for BatchProcessingCompletedEvent: {}", event.batchId().value());
        meterRegistry.counter("ingestion.batch.completed",
                List.of(Tag.of("bank_id", event.bankId().value()))
        ).increment();
        
        meterRegistry.summary("ingestion.file.size")
            .record(event.fileSizeBytes());
    }

    @EventListener
    public void onBatchProcessingFailed(BatchProcessingFailedEvent event) {
        log.debug("Recording metrics for BatchProcessingFailedEvent: {}", event.getBatchId());
        meterRegistry.counter("ingestion.batch.failed",
                List.of(
                        Tag.of("bank_id", event.getBankId()),
                        Tag.of("error_type", event.getErrorType())
                )
        ).increment();
    }
}
