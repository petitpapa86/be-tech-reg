package com.bcbs239.regtech.metrics.application.signal;

/**
 * A semantic, application-layer signal describing a business-relevant action or decision.
 *
 * Infrastructure can observe and log/metric/trace these signals.
 */
public interface ApplicationSignal {
    String type();

    default SignalLevel level() {
        return SignalLevel.INFO;
    }
}
