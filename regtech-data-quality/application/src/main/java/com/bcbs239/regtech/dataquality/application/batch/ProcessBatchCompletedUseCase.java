package com.bcbs239.regtech.dataquality.application.batch;

import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedInboundEvent;
import com.bcbs239.regtech.dataquality.application.validation.ValidateBatchQualityCommand;
import com.bcbs239.regtech.dataquality.application.validation.ValidateBatchQualityCommandHandler;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component("dataQualityProcessBatchCompletedUseCase")
@RequiredArgsConstructor
public class ProcessBatchCompletedUseCase {
    private final ValidateBatchQualityCommandHandler handler;
    private final IQualityReportRepository repository;


    public void process(BatchCompletedInboundEvent event) {
        if (!event.isValid()) return;
        if (repository.existsByBatchId(new BatchId(event.getBatchId()))) return;
        handle(event);
    }

    @Async("qualityEventExecutor")
    private void handle(BatchCompletedInboundEvent event) {
        handler.handle(createValidationCommand(event));
    }

    private ValidateBatchQualityCommand createValidationCommand(BatchCompletedInboundEvent event) {
        String correlationId = event.getCorrelationId();
        String fileFormat = extractFileExtension(event.getFilename());

        if (correlationId != null && !correlationId.isEmpty()) {
            return ValidateBatchQualityCommand.withCorrelation(
                    new BatchId(event.getBatchId()),
                    new BankId(event.getBankId()),
                    event.getS3Uri(),
                    event.getTotalExposures(),
                    correlationId,
                    event.getFilename(),
                    event.getFileSizeBytes(),
                    fileFormat
            );
        }

        return ValidateBatchQualityCommand.of(
                new BatchId(event.getBatchId()),
                new BankId(event.getBankId()),
                event.getS3Uri(),
                event.getTotalExposures(),
                event.getFilename(),
                event.getFileSizeBytes(),
                fileFormat
        );
    }

    private String extractFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
