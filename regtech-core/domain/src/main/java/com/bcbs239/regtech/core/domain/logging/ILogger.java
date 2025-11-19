package com.bcbs239.regtech.core.domain.logging;

/**
 * Lightweight compatibility interface for legacy ILogger usages.
 * New code should prefer org.slf4j.Logger directly, but many
 * existing components still inject ILogger — provide a minimal
 * adapter surface here so we can satisfy Spring DI and delegate
 * to SLF4J in infrastructure.
 */
public interface ILogger {
    void trace(String msg);
    void trace(String format, Object... args);

    void debug(String msg);
    void debug(String format, Object... args);

    void info(String msg);
    void info(String format, Object... args);

    void warn(String msg);
    void warn(String format, Object... args);

    void error(String msg);
    void error(String format, Object... args);
    void error(String msg, Throwable t);
}
