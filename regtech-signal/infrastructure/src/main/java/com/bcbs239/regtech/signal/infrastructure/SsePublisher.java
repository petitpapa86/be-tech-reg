package com.bcbs239.regtech.signal.infrastructure;

import com.bcbs239.regtech.signal.domain.SignalEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Simple in-memory SseEmitter publisher.
 * Not for production use — use a distributed solution for multiple instances.
 */
@Component
public class SsePublisher {
    private static final Logger log = LoggerFactory.getLogger(SsePublisher.class);
    private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        log.info("SSE emitter created (total={})", emitters.size());
        return emitter;
    }

    public void publish(SignalEvent event) {
        publishToAll(emitter -> sendEvent(emitter, event));
    }

    private void publishToAll(Consumer<SseEmitter> action) {
        for (SseEmitter emitter : emitters) {
            executor.execute(() -> {
                try {
                    action.accept(emitter);
                } catch (Exception e) {
                    try { emitter.completeWithError(e); } catch (Exception ex) { /* ignore */ }
                    emitters.remove(emitter);
                }
            });
        }
    }

    private void sendEvent(SseEmitter emitter, SignalEvent event) {
        try {
            SseEmitter.SseEventBuilder builder = SseEmitter.event()
                    .id(event.getId())
                    .name(event.getType())
                    .data(event.getPayload())
                    .reconnectTime(3000);
            emitter.send(builder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
