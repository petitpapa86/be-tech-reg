package com.bcbs239.regtech.core.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * Time provider interface for better testability.
 * Allows injecting time sources instead of using static now() methods.
 */
@FunctionalInterface
public interface TimeProvider {

    /**
     * Returns the underlying clock.
     */
    Clock getClock();

    /**
     * Returns the current instant.
     */
    default Instant nowInstant() {
        return Instant.now(getClock());
    }

    /**
     * Returns the current local date time.
     */
    default LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now(getClock());
    }

    /**
     * Returns the current zoned date time.
     */
    default ZonedDateTime nowZonedDateTime() {
        return ZonedDateTime.now(getClock());
    }
}

