package com.bcbs239.regtech.signal.presentation;

import com.bcbs239.regtech.signal.application.SignalService;
import com.bcbs239.regtech.signal.infrastructure.SsePublisher;
import com.bcbs239.regtech.core.presentation.controllers.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Presentation layer: SSE endpoint and a small send API for testing.
 *
 * This component uses functional routing and extends `BaseController` to
 * reuse consistent ServerResponse handling used across modules.
 */
@Component
public class SseController extends BaseController implements com.bcbs239.regtech.signal.presentation.common.IEndpoint {
    private static final Logger log = LoggerFactory.getLogger(SseController.class);
    private final SsePublisher publisher;
    private final SignalService service;

    public SseController(SsePublisher publisher, SignalService service) {
        this.publisher = publisher;
        this.service = service;
    }

    @Override
    public RouterFunction<ServerResponse> mapEndpoints() {
        return RouterFunctions.route()
                .GET("/api/v1/signal/stream", RequestPredicates.accept(MediaType.ALL), this::stream)
                .POST("/api/v1/signal/send", this::send)
                .build();
    }

    public ServerResponse stream(ServerRequest request) {
        try {
            log.info("Frontend connecting to SSE stream from: {}", request.remoteAddress());
            SseEmitter emitter = publisher.createEmitter();
            log.info("SSE emitter created and returned to frontend");
            return ServerResponse.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(emitter);
        } catch (Exception e) {
            log.error("Failed to create SSE emitter", e);
            return handleSystemErrorResponse(e);
        }
    }

    public ServerResponse send(ServerRequest request) {
        try {
            String type = request.param("type").orElse("default");
            String payload = request.param("payload").orElse("");

            log.info("Received send request; type={}", type);
            service.publish(type, payload);

            return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SuccessResponse<>("ok", "Message published", "signal.sent"));
        } catch (Exception e) {
            log.error("Failed to publish signal", e);
            return handleSystemErrorResponse(e);
        }
    }
}
