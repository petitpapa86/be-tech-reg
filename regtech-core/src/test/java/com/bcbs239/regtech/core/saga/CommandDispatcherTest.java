package com.bcbs239.regtech.core.saga;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommandDispatcherTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CommandDispatcher commandDispatcher;

    @Test
    void dispatch_shouldPublishCommandAsApplicationEvent() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        String commandType = "TestCommand";
        Map<String, Object> payload = Map.of("key", "value");
        Instant createdAt = Instant.now();
        SagaCommand command = new SagaCommand(sagaId, commandType, payload, createdAt);

        // When
        commandDispatcher.dispatch(command);

        // Then
        verify(eventPublisher).publishEvent(command);
    }
}