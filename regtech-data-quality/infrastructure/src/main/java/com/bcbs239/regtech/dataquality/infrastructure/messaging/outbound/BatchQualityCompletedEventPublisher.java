package com.bcbs239.regtech.dataquality.infrastructure.messaging.outbound;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.events.integration.DataQualityCompletedIntegrationEvent;
import com.bcbs239.regtech.dataquality.domain.report.events.QualityValidationCompletedEvent;
import com.bcbs239.regtech.dataquality.infrastructure.reporting.QualityReportEntity;
import com.bcbs239.regtech.dataquality.infrastructure.reporting.QualityReportJpaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("dataQualityBatchQualityCompletedEventPublisher")
@RequiredArgsConstructor
public class BatchQualityCompletedEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(BatchQualityCompletedEventPublisher.class);

    private final IIntegrationEventBus eventBus;
    private final QualityReportJpaRepository qualityReportJpaRepository;

    @EventListener
    public void handle(QualityValidationCompletedEvent event) {
        if (CorrelationContext.isOutboxReplay()) {
            logger.debug("Skipping integration publish for QualityValidationCompletedEvent {} because this is an outbox replay", event.getEventId());
            return;
        }
        try {
            logger.info("Converting and publishing BatchQualityCompletedIntegrationEvent for batch {}", event.getBatchId().value());

            QualityReportEntity reportEntity = qualityReportJpaRepository.findByBatchId(event.getBatchId().value())
                    .orElse(null);

            Integer totalExposures = reportEntity != null ? reportEntity.getTotalExposures() : null;
            Integer validExposures = reportEntity != null ? reportEntity.getValidExposures() : null;
            Integer totalErrors = reportEntity != null ? reportEntity.getTotalErrors() : null;
            Boolean complianceStatus = reportEntity != null ? reportEntity.getComplianceStatus() : null;

            Double completenessScore = event.getQualityScores() != null ? event.getQualityScores().completenessScore() : null;
            Double accuracyScore = event.getQualityScores() != null ? event.getQualityScores().accuracyScore() : null;
            Double consistencyScore = event.getQualityScores() != null ? event.getQualityScores().consistencyScore() : null;
            Double timelinessScore = event.getQualityScores() != null ? event.getQualityScores().timelinessScore() : null;
            Double uniquenessScore = event.getQualityScores() != null ? event.getQualityScores().uniquenessScore() : null;
            Double validityScore = event.getQualityScores() != null ? event.getQualityScores().validityScore() : null;

            DataQualityCompletedIntegrationEvent integrationEvent = new DataQualityCompletedIntegrationEvent(
                    event.getBatchId().value(),
                    event.getBankId().value(),
                    event.getDetailsReference().uri(),
                    event.getQualityScores().overallScore(),
                    event.getQualityGrade().getLetterGrade(),
                    Instant.now(),
                    totalExposures,
                    validExposures,
                    totalErrors,
                    complianceStatus,
                    completenessScore,
                    accuracyScore,
                    consistencyScore,
                    timelinessScore,
                    uniquenessScore,
                    validityScore,
                    event.getCorrelationId(),
                    event.getFilename(),
                    event.getReportId().value()
            );

            ScopedValue.where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
                    .where(CorrelationContext.OUTBOX_REPLAY, true)
                    .run(() -> eventBus.publish(integrationEvent));

            logger.info("Published BatchQualityCompletedIntegrationEvent for batch {}", event.getBatchId().value());

        } catch (Exception ex) {
            logger.error("Failed to publish BatchQualityCompletedIntegrationEvent for batch {}", event.getBatchId().value(), ex);
            throw ex;
        }
    }
}