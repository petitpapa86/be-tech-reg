package com.bcbs239.regtech.signal.presentation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Web configuration for Signal module SSE endpoints.
 * 
 * NOTE: SSE endpoints are now handled by SseRestController using @RestController
 * instead of functional routing. This is because Spring's functional routing
 * (RouterFunction) doesn't properly handle SseEmitter return types, causing
 * HttpMediaTypeNotAcceptableException errors.
 * 
 * The @RestController approach with @GetMapping properly handles:
 * - Content-Type: text/event-stream negotiation
 * - SseEmitter lifecycle management
 * - Automatic response serialization
 */
@Configuration
public class SignalWebConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SignalWebConfiguration.class);

    // No RouterFunction bean needed - using @RestController instead
}
