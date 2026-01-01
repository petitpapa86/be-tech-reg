package com.bcbs239.regtech.core.infrastructure.systemservices;

import java.time.Clock;
import java.util.Objects;

import com.bcbs239.regtech.core.application.TimeProvider;

/**
 * Time provider backed by a {@link Clock} for better testability.
 */
public class SystemTimeProvider implements TimeProvider {
    private final Clock clock;

    // Default constructor uses system clock
    public SystemTimeProvider() {
        this(Clock.systemDefaultZone());
    }

    // Constructor for injecting a custom clock (e.g., Clock.fixed(...) in tests)
    public SystemTimeProvider(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Clock getClock() {
        return clock;
    }
}

