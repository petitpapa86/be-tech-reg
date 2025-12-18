package com.bcbs239.regtech.dataquality.infrastructure.observability;

import com.bcbs239.regtech.dataquality.domain.report.events.QualityScoresCalculatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EventLogger {
    private static final Logger logger = LoggerFactory.getLogger(EventLogger.class);

    @EventListener
    public void onQualityScoresCalculatedEvent(QualityScoresCalculatedEvent event){
        logger.info("Quality validation completed: {}", event.getQualityScores());
    }
}
