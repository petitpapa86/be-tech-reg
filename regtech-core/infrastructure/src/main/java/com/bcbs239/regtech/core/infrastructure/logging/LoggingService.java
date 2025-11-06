package com.bcbs239.regtech.core.infrastructure.logging;

import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.infrastructure.persistence.LoggingConfiguration;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Infrastructure implementation of the ILogger domain interface.
 * Delegates to LoggingConfiguration for actual logging functionality.
 */
@Service
public class LoggingService implements ILogger {

    @Override
    public void asyncStructuredLog(String message, Map<String, Object> details) {
        LoggingConfiguration.logStructured(message, details);
    }

    @Override
    public void asyncStructuredErrorLog(String message, Throwable throwable, Map<String, Object> details) {
        LoggingConfiguration.logStructured(message, details, throwable);

    }
}

