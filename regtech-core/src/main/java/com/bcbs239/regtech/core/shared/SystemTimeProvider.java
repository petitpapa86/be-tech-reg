package com.bcbs239.regtech.core.shared;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

/**
 * Time provider using functional closures (suppliers) for better testability.
 */
public class SystemTimeProvider {
    private Supplier<Instant> nowInstant;
    private Supplier<LocalDateTime> nowLocalDateTime;
    private Supplier<ZonedDateTime> nowZonedDateTime;

    // Default constructor uses system clock
    public SystemTimeProvider() {
        Clock clock = Clock.systemDefaultZone();
        this.nowInstant = () -> Instant.now(clock);
        this.nowLocalDateTime = () -> LocalDateTime.now(clock);
        this.nowZonedDateTime = () -> ZonedDateTime.now(clock);
    }

    // Constructor for injecting custom time suppliers (e.g., for testing)
    public SystemTimeProvider(Supplier<Instant> instantSupplier,
                              Supplier<LocalDateTime> localDateTimeSupplier,
                              Supplier<ZonedDateTime> zonedDateTimeSupplier) {
        this.nowInstant = instantSupplier;
        this.nowLocalDateTime = localDateTimeSupplier;
        this.nowZonedDateTime = zonedDateTimeSupplier;
    }

    public Instant nowInstant() {
        return nowInstant.get();
    }

    public LocalDateTime nowLocalDateTime() {
        return nowLocalDateTime.get();
    }

    public ZonedDateTime nowZonedDateTime() {
        return nowZonedDateTime.get();
    }
}