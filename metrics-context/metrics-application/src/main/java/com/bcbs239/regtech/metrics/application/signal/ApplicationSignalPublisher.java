package com.bcbs239.regtech.metrics.application.signal;

public interface ApplicationSignalPublisher {
    void publish(ApplicationSignal signal);
}
