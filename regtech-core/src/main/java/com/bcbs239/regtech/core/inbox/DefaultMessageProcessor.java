package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.application.IntegrationEvent;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
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
    public Result<Void> process(InboxMessageEntity message) throws Exception {
        IntegrationEvent event = deserializer.deserialize(message.getEventType(), message.getEventData());

        boolean success = dispatcher.dispatch(event, message.getId());

        if (success) {
            markAsProcessedFn.accept(message.getId());
            logger.debug("Processed inbox message {} successfully", message.getId());
            return Result.success(null);
        } else {
            markAsPermanentlyFailedFn.accept(message.getId(), "One or more handlers failed");
            logger.warn("One or more handlers failed for message {}", message.getId());
            return Result.failure(new ErrorDetail("HANDLER_FAILURE", "One or more handlers failed"));
        }
    }
}
