package com.bcbs239.regtech.signal.infrastructure;

import com.bcbs239.regtech.signal.domain.SignalEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Simple in-memory SseEmitter publisher with event buffering.
 * Buffers recent events (last 30 seconds) to send to newly connecting clients.
 */
@Component
public class SsePublisher {
    private static final Logger log = LoggerFactory.getLogger(SsePublisher.class);
    private static final Duration EVENT_BUFFER_DURATION = Duration.ofSeconds(30);
    private static final int MAX_BUFFER_SIZE = 50;
    
    private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();
    private final LinkedList<BufferedEvent> eventBuffer = new LinkedList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static class BufferedEvent {
        final SignalEvent event;
        final Instant timestamp;
        
        BufferedEvent(SignalEvent event) {
            this.event = event;
            this.timestamp = Instant.now();
        }
        
        boolean isExpired() {
            return Duration.between(timestamp, Instant.now()).compareTo(EVENT_BUFFER_DURATION) > 0;
        }
    }

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        log.info("SSE emitter created (total={})", emitters.size());
        
        // Send buffered events to new client
        sendBufferedEventsToNewClient(emitter);
        
        return emitter;
    }
    
    private void sendBufferedEventsToNewClient(SseEmitter emitter) {
        synchronized (eventBuffer) {
            if (!eventBuffer.isEmpty()) {
                log.info("Sending {} buffered events to new client", eventBuffer.size());
                for (BufferedEvent bufferedEvent : eventBuffer) {
                    executor.execute(() -> {
                        try {
                            sendEvent(emitter, bufferedEvent.event);
                        } catch (Exception e) {
                            log.warn("Failed to send buffered event to new client", e);
                        }
                    });
                }
            }
        }
    }

    public void publish(SignalEvent event) {
        log.info("Publishing SSE event: type={}, id={}, activeEmitters={}", event.getType(), event.getId(), emitters.size());
        
        // Add to buffer
        addToBuffer(event);
        
        if (emitters.isEmpty()) {
            log.warn("No active SSE emitters to publish event to! Event type={} (buffered for {} seconds)", 
                event.getType(), EVENT_BUFFER_DURATION.getSeconds());
        } else {
            publishToAll(emitter -> sendEvent(emitter, event));
        }
    }
    
    private void addToBuffer(SignalEvent event) {
        synchronized (eventBuffer) {
            // Remove expired events
            eventBuffer.removeIf(BufferedEvent::isExpired);
            
            // Add new event
            eventBuffer.add(new BufferedEvent(event));
            
            // Keep buffer size limited
            while (eventBuffer.size() > MAX_BUFFER_SIZE) {
                eventBuffer.removeFirst();
            }
            
            log.debug("Event added to buffer. Buffer size: {}", eventBuffer.size());
        }
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
            log.debug("Successfully sent SSE event: type={}, id={}", event.getType(), event.getId());
        } catch (IOException e) {
            log.error("Failed to send SSE event: type={}, id={}", event.getType(), event.getId(), e);
            throw new RuntimeException(e);
        }
    }
}
