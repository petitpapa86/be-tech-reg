package com.bcbs239.regtech.metrics.infrastructure.observability;

import com.bcbs239.regtech.metrics.application.signal.ApplicationSignal;
import com.bcbs239.regtech.metrics.application.signal.ApplicationSignalPublisher;
import com.bcbs239.regtech.metrics.application.signal.SignalLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingApplicationSignalPublisher implements ApplicationSignalPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingApplicationSignalPublisher.class);

    @Override
    public void publish(ApplicationSignal signal) {
        if (signal == null) {
            return;
        }

        String type = signal.type();
        SignalLevel level = signal.level();

        if (level == SignalLevel.ERROR) {
            log.error("app-signal type={} payload={}", type, signal);
        } else if (level == SignalLevel.WARN) {
            log.warn("app-signal type={} payload={}", type, signal);
        } else if (level == SignalLevel.DEBUG) {
            log.debug("app-signal type={} payload={}", type, signal);
        } else {
            log.info("app-signal type={} payload={}", type, signal);
        }
    }
}
