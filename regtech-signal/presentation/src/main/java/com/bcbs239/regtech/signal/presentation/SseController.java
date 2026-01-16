package com.bcbs239.regtech.signal.presentation;

import com.bcbs239.regtech.signal.application.SignalService;
import com.bcbs239.regtech.signal.infrastructure.SsePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Presentation layer: SSE endpoint and a small send API for testing.
 */
@RestController
public class SseController {
    private static final Logger log = LoggerFactory.getLogger(SseController.class);
    private final SsePublisher publisher;
    private final SignalService service;

    public SseController(SsePublisher publisher, SignalService service) {
        this.publisher = publisher;
        this.service = service;
    }

    @GetMapping(path = "/api/v1/signal/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return publisher.createEmitter();
    }

    @PostMapping(path = "/api/v1/signal/send")
    public String send(@RequestParam String type, @RequestParam String payload) {
        log.info("Received send request; type={}", type);
        service.publish(type, payload);
        return "ok";
    }
}
