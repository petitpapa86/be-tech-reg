package com.bcbs239.regtech.metrics.infrastructure.observability;

import com.bcbs239.regtech.metrics.application.signal.ApplicationSignal;
import com.bcbs239.regtech.metrics.application.signal.ApplicationSignalPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class SpringEventApplicationSignalPublisher implements ApplicationSignalPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public SpringEventApplicationSignalPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publish(ApplicationSignal signal) {
        if (signal == null) {
            return;
        }
        StackTraceElement origin = findOrigin();

        eventPublisher.publishEvent(ApplicationSignalEmittedEvent.now(
                signal,
                origin == null ? null : origin.getClassName(),
                origin == null ? null : origin.getMethodName(),
                origin == null ? null : origin.getFileName(),
                origin == null ? null : origin.getLineNumber()
        ));
    }

    private StackTraceElement findOrigin() {
        StackTraceElement[] frames = Thread.currentThread().getStackTrace();
        if (frames == null) {
            return null;
        }

        for (StackTraceElement frame : frames) {
            if (frame == null) {
                continue;
            }
            String className = frame.getClassName();
            if (className == null) {
                continue;
            }

            // Skip JVM + logging + Spring + our observability plumbing
            if (className.startsWith("java.")) {
                continue;
            }
            if (className.startsWith("sun.")) {
                continue;
            }
            if (className.startsWith("jdk.")) {
                continue;
            }
            if (className.startsWith("org.springframework.")) {
                continue;
            }
            if (className.startsWith("org.slf4j.")) {
                continue;
            }
            if (className.startsWith("ch.qos.logback.")) {
                continue;
            }
            if (className.startsWith("com.bcbs239.regtech.metrics.infrastructure.observability.")) {
                continue;
            }

            // We prefer application-layer callers; this should typically match DashboardUseCase, etc.
            return frame;
        }

        return null;
    }
}
