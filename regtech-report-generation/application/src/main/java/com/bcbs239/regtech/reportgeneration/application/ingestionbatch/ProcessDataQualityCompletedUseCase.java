package com.bcbs239.regtech.reportgeneration.application.ingestionbatch;

import com.bcbs239.regtech.core.domain.eventprocessing.EventProcessingFailure;
import com.bcbs239.regtech.core.domain.eventprocessing.IEventProcessingFailureRepository;
import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedInboundEvent;

import com.bcbs239.regtech.core.domain.events.integration.DataQualityCompletedInboundEvent;
import com.bcbs239.regtech.reportgeneration.application.coordination.QualityEventData;
import com.bcbs239.regtech.reportgeneration.application.coordination.ReportCoordinator;
import com.bcbs239.regtech.reportgeneration.domain.generation.IGeneratedReportRepository;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;


import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.reportgeneration.domain.generation.GeneratedReport;
import java.util.concurrent.CompletableFuture;

@Component("ProcessDataQualityCompletedUseCase")
@RequiredArgsConstructor
public class ProcessDataQualityCompletedUseCase {
    private final ReportCoordinator reportCoordinator;
    private final ObjectMapper objectMapper;
    private final IEventProcessingFailureRepository failureRepository;

    public void process(DataQualityCompletedInboundEvent event)  {
        try {
            if (!event.isValid()) {
                return;
            }

            handle(event).thenAccept(result -> {
                if (result != null && result.isFailure()) {
                    handleEventProcessingError(event, new RuntimeException(result.getError().get().getMessage()));
                }
            }).exceptionally(e -> {
                handleEventProcessingError(event, (Exception) e);
                return null;
            });

        } catch (Exception e) {

            handleEventProcessingError(event, e);
        }
    }

    private CompletableFuture<Result<GeneratedReport>> handle(DataQualityCompletedInboundEvent event) {
        QualityEventData qualityEventData = new QualityEventData(
                event.getBatchId(),
                event.getBankId(),
                event.getS3ReferenceUri(),
                BigDecimal.valueOf(event.getOverallScore()),
                event.getQualityGrade(),
                event.getCompletedAt()
        );
        return reportCoordinator.handleQualityCompleted(qualityEventData);

    }

    private void handleEventProcessingError(DataQualityCompletedInboundEvent event, Exception error)  {

        String eventPayload ;
        try {
            eventPayload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Map<String, String> metadata = Map.of(
                "batchId", event.getBatchId(),
                "eventType", event.getClass().getName(),
                "errorType", error.getClass().getSimpleName()
        );

        EventProcessingFailure failure = EventProcessingFailure.create(
                event.getClass().getName(),
                eventPayload,
                error.getMessage() != null ? error.getMessage() : error.getClass().getName(),
                getStackTraceAsString(error),
                metadata,
                3
        );

        failureRepository.save(failure);
    }

    private String getStackTraceAsString(Throwable e) {
        if (e == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");

        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }

        return sb.toString();
    }
}
