package com.bcbs239.regtech.core.infrastructure.logging;

import com.bcbs239.regtech.core.domain.logging.ILogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Spring component that provides an ILogger implementation delegating to SLF4J.
 * It is intentionally lightweight and stateless; callers should typically
 * request a typed SLF4J Logger, but this adapter satisfies legacy DI.
 */
@Component
public class Slf4jILoggerAdapter implements ILogger {

    private final Logger logger = LoggerFactory.getLogger("com.bcbs239.regtech.ILogger");

    @Override
    public void trace(String msg) { logger.trace(msg); }

    @Override
    public void trace(String format, Object... args) { logger.trace(format, args); }

    @Override
    public void debug(String msg) { logger.debug(msg); }

    @Override
    public void debug(String format, Object... args) { logger.debug(format, args); }

    @Override
    public void info(String msg) { logger.info(msg); }

    @Override
    public void info(String format, Object... args) { logger.info(format, args); }

    @Override
    public void warn(String msg) { logger.warn(msg); }

    @Override
    public void warn(String format, Object... args) { logger.warn(format, args); }

    @Override
    public void error(String msg) { logger.error(msg); }

    @Override
    public void error(String format, Object... args) { logger.error(format, args); }

    @Override
    public void error(String msg, Throwable t) { logger.error(msg, t); }
}

