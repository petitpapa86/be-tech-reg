package com.bcbs239.regtech.reportgeneration.application.integration;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.integration.BatchQualityCompletedIntegrationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BatchQualityCompletedIntegrationEventAdapater {

//    @EventListener
//    public void onBatchCompletedIntegrationEvent(BatchQualityCompletedIntegrationEvent integrationEvent) {
//        if (CorrelationContext.isInboxReplay()) {
//            return;
//        }
//    }
}
