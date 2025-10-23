package com.bcbs239.regtech.core.shared;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * Time provider interface for better testability.
 * Allows injecting time sources instead of using static now() methods.
 */
public interface TimeProvider {

    /**
     * Returns the current instant.
     */
    Instant nowInstant();

    /**
     * Returns the current local date time.
     */
    LocalDateTime nowLocalDateTime();

    /**
     * Returns the current zoned date time.
     */
    ZonedDateTime nowZonedDateTime();

    /**
     * Returns the underlying clock.
     */
    Clock getClock();
}