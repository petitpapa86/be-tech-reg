package com.bcbs239.regtech.core.saga;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommandDispatcher {
     private final ApplicationEventPublisher eventPublisher;

    public void dispatch(SagaCommand command) {
        eventPublisher.publishEvent(new GenericEvent<>(command, command::commandType));
    }
}
