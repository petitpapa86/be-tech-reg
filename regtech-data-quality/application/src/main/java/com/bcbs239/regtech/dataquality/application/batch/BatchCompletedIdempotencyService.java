package com.bcbs239.regtech.dataquality.application.batch;

import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedInboundEvent;
import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedIntegrationEvent;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class BatchCompletedIdempotencyService {
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final IQualityReportRepository repository;

    public boolean tryMark(BatchCompletedInboundEvent event) {
        String key = key(event);
     //   if (inFlight.contains(key)) return false;
        if (repository.existsByBatchId(new BatchId(event.getBatchId()))) return false;
        inFlight.add(key);
        return true;
    }

    public void unmark(BatchCompletedInboundEvent event) {
        inFlight.remove(key(event));
    }

    private String key(BatchCompletedInboundEvent e) {
        return e.getBatchId() + ":" + e.getBankId();
    }

    public void complete(BatchCompletedInboundEvent event) {

    }
}
