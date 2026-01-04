package com.bcbs239.regtech.metrics.infrastructure.observability;

import com.bcbs239.regtech.metrics.application.signal.ApplicationSignal;

import java.time.Instant;

public record ApplicationSignalEmittedEvent(
        ApplicationSignal signal,
        Instant emittedAt,
        String sourceClassName,
        String sourceMethodName,
        String sourceFileName,
        Integer sourceLineNumber
) {
    public static ApplicationSignalEmittedEvent now(ApplicationSignal signal) {
        return new ApplicationSignalEmittedEvent(signal, Instant.now(), null, null, null, null);
    }

    public static ApplicationSignalEmittedEvent now(
            ApplicationSignal signal,
            String sourceClassName,
            String sourceMethodName,
            String sourceFileName,
            Integer sourceLineNumber
    ) {
        return new ApplicationSignalEmittedEvent(
                signal,
                Instant.now(),
                sourceClassName,
                sourceMethodName,
                sourceFileName,
                sourceLineNumber
        );
    }
}
