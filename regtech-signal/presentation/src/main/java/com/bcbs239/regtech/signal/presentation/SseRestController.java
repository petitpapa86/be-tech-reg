package com.bcbs239.regtech.signal.presentation;

import com.bcbs239.regtech.signal.application.SignalService;
import com.bcbs239.regtech.signal.infrastructure.SsePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller for SSE (Server-Sent Events) streaming.
 * 
 * SSE endpoints MUST use @RestController with @GetMapping because Spring's functional
 * routing (RouterFunction) doesn't properly handle SseEmitter return types.
 * 
 * This controller provides:
 * - GET /api/v1/signal/stream - SSE stream endpoint (returns SseEmitter)
 * - POST /api/v1/signal/send - Test endpoint to publish events
 */
@RestController
@RequestMapping("/api/v1/signal")
public class SseRestController {
    private static final Logger log = LoggerFactory.getLogger(SseRestController.class);
    
    private final SsePublisher publisher;
    private final SignalService service;

    public SseRestController(SsePublisher publisher, SignalService service) {
        this.publisher = publisher;
        this.service = service;
    }

    /**
     * SSE stream endpoint.
     * 
     * Returns an SseEmitter that will stream events to the client.
     * Content-Type is automatically set to text/event-stream by Spring.
     * 
     * This method does NOT throw exceptions to GlobalExceptionHandler because
     * SSE uses streaming protocol and cannot return JSON ApiResponse objects.
     * All errors are logged and handled internally.
     * 
     * @return SseEmitter for streaming events
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        try {
            log.info("Frontend connecting to SSE stream");
            SseEmitter emitter = publisher.createEmitter();
            log.info("SSE emitter created and returned to frontend");
            return emitter;
        } catch (Exception e) {
            log.error("Failed to create SSE emitter", e);
            // Return a new emitter that immediately completes with error
            SseEmitter errorEmitter = new SseEmitter();
            errorEmitter.completeWithError(e);
            return errorEmitter;
        }
    }

    /**
     * Test endpoint to manually publish events.
     * 
     * Usage: POST /api/v1/signal/send?type=test&payload=hello
     * 
     * @param type Event type (default: "default")
     * @param payload Event payload (default: "")
     * @return Success response
     */
    @PostMapping("/send")
    public SuccessResponse send(
            @RequestParam(defaultValue = "default") String type,
            @RequestParam(defaultValue = "") String payload
    ) {
        log.info("Received send request; type={}", type);
        service.publish(type, payload);
        return new SuccessResponse("ok", "Message published", "signal.sent");
    }

    /**
     * Simple response record for send endpoint.
     */
    public record SuccessResponse(String status, String message, String messageKey) {}
}
