package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.application.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Component
public class DefaultMessageProcessor implements MessageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageProcessor.class);

    private final IntegrationEventDeserializer deserializer;
    private final EventDispatcher dispatcher;
    private final Consumer<String> markAsProcessedFn;
    private final BiConsumer<String, String> markAsPermanentlyFailedFn;

    public DefaultMessageProcessor(IntegrationEventDeserializer deserializer,
                                   EventDispatcher dispatcher,
                                   Consumer<String> markAsProcessedFn,
                                   BiConsumer<String, String> markAsPermanentlyFailedFn) {
        this.deserializer = deserializer;
        this.dispatcher = dispatcher;
        this.markAsProcessedFn = markAsProcessedFn;
        this.markAsPermanentlyFailedFn = markAsPermanentlyFailedFn;
    }

    @Override
    @Transactional
    public void process(InboxMessageEntity message) {
        try {
            IntegrationEvent event = deserializer.deserialize(message.getEventType(), message.getEventData());

            boolean success = dispatcher.dispatch(event, message.getId());

            if (success) {
                markAsProcessedFn.accept(message.getId());
                logger.debug("Processed inbox message {} successfully", message.getId());
            } else {
                markAsPermanentlyFailedFn.accept(message.getId(), "One or more handlers failed");
                logger.warn("One or more handlers failed for message {}", message.getId());
            }
        } catch (ClassNotFoundException e) {
            logger.error("Integration event class not found for inbox message {}: {}", message.getId(), e.getMessage());
            markAsPermanentlyFailedFn.accept(message.getId(), e.getMessage());
            throw new RuntimeException("Integration event class not found", e);
        } catch (Exception e) {
            logger.error("Failed to process message {}: {}", message.getId(), e.getMessage(), e);
            markAsPermanentlyFailedFn.accept(message.getId(), e.getMessage());
            throw new RuntimeException("Failed to process message", e);
        }
    }
}
