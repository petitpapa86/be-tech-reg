package com.bcbs239.regtech.metrics.infrastructure.observability;

import com.bcbs239.regtech.metrics.application.signal.ApplicationSignal;
import com.bcbs239.regtech.metrics.application.signal.ComplianceReportUpsertedSignal;
import com.bcbs239.regtech.metrics.application.signal.DashboardMetricsUpdateIgnoredSignal;
import com.bcbs239.regtech.metrics.application.signal.DashboardMetricsUpdatedSignal;
import com.bcbs239.regtech.metrics.application.signal.DashboardQueriedSignal;
import com.bcbs239.regtech.metrics.application.signal.SignalLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Infrastructure component that listens to ApplicationSignal events and logs them.
 * 
 * <p>This removes the need for direct logging in the application layer.
 * The application layer publishes semantic signals/events, and this infrastructure
 * component converts them to structured logs with appropriate context (MDC).</p>
 * 
 * <p>Design principle: Application layer is unaware of logging infrastructure.
 * It only publishes domain-meaningful signals via ApplicationSignalPublisher.</p>
 */
@Component
public class ApplicationSignalLoggingListener {

    private static final Logger log = LoggerFactory.getLogger(ApplicationSignalLoggingListener.class);

    @EventListener
    public void on(ApplicationSignalEmittedEvent event) {
        if (event == null || event.signal() == null) {
            return;
        }

        ApplicationSignal signal = event.signal();

        switch (signal) {
            case DashboardQueriedSignal(String id, String startDate, String endDate) -> {
                withContext(id, null, event, () ->
                        info(signal, "Dashboard requested bankId={} range={}..{}", id, startDate, endDate)
                );
                return;
            }
            case DashboardMetricsUpdatedSignal(
                    String bankId, String batchId, java.time.LocalDate periodStart, java.time.LocalDate completedDate,
                    Double overallScore, Double completenessScore, Integer totalErrors
            ) -> {
                withContext(bankId, batchId, event, () ->
                        info(signal,
                                "Dashboard metrics updated bankId={} batchId={} periodStart={} completedDate={} overallScore={} completenessScore={} totalErrors={}",
                                bankId, batchId, periodStart, completedDate, overallScore, completenessScore, totalErrors)
                );
                return;
            }
            case DashboardMetricsUpdateIgnoredSignal(String reason, String bankId, String batchId) -> {
                withContext(bankId, batchId, event, () ->
                        warn(signal, "Dashboard metrics update ignored reason={} bankId={} batchId={}", reason, bankId, batchId)
                );
                return;
            }
            case ComplianceReportUpsertedSignal s -> {
                withContext(s.bankId(), null, event, () ->
                        info(signal,
                                "Compliance report upserted reportId={} bankId={} status={} reportType={} reportingDate={}",
                                s.reportId(), s.bankId(), s.status(), s.reportType(), s.reportingDate())
                );
                return;
            }
            default -> {
            }
        }

        // Fallback: log type + payload for unknown signals
        info(signal, "Application signal emitted type={} payload={}", signal.type(), signal);
    }

    private void withContext(String bankId, String batchId, ApplicationSignalEmittedEvent event, Runnable action) {
        try (
                MDC.MDCCloseable ignoredBankId = bankId == null ? null : MDC.putCloseable("bank-id", bankId);
                MDC.MDCCloseable ignoredBatchId = batchId == null ? null : MDC.putCloseable("batch-id", batchId);
                MDC.MDCCloseable ignoredSourceClass = event == null || event.sourceClassName() == null ? null : MDC.putCloseable("source-class", event.sourceClassName());
                MDC.MDCCloseable ignoredSourceMethod = event == null || event.sourceMethodName() == null ? null : MDC.putCloseable("source-method", event.sourceMethodName());
                MDC.MDCCloseable ignoredSourceFile = event == null || event.sourceFileName() == null ? null : MDC.putCloseable("source-file", event.sourceFileName());
                MDC.MDCCloseable ignoredSourceLine = event == null || event.sourceLineNumber() == null ? null : MDC.putCloseable("source-line", String.valueOf(event.sourceLineNumber()))
        ) {
            action.run();
        }
    }

    private void info(ApplicationSignal signal, String message, Object... args) {
        if (signal.level() == SignalLevel.DEBUG) {
            log.debug(message, args);
        } else {
            log.info(message, args);
        }
    }

    private void warn(ApplicationSignal signal, String message, Object... args) {
        if (signal.level() == SignalLevel.ERROR) {
            log.error(message, args);
        } else {
            log.warn(message, args);
        }
    }
}
