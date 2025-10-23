package com.bcbs239.regtech.core.saga;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SagaCommand record.
 * Demonstrates functional programming principles:
 * - Immutable data structures (records)
 * - Pure data carriers
 */
class SagaCommandTest {

    @Test
    void constructor_shouldCreateCommandWithAllFields() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        String commandType = "TestCommand";
        Map<String, Object> payload = Map.of("key", "value", "number", 42);
        Instant createdAt = Instant.now();

        // When
        SagaCommand command = new SagaCommand(sagaId, commandType, payload, createdAt);

        // Then
        assertThat(command.getSagaId()).isEqualTo(sagaId);
        assertThat(command.commandType()).isEqualTo(commandType);
        assertThat(command.payload()).isEqualTo(payload);
        assertThat(command.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void constructor_shouldHandleEmptyPayload() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        String commandType = "EmptyCommand";
        Map<String, Object> emptyPayload = Map.of();
        Instant createdAt = Instant.now();

        // When
        SagaCommand command = new SagaCommand(sagaId, commandType, emptyPayload, createdAt);

        // Then
        assertThat(command.getSagaId()).isEqualTo(sagaId);
        assertThat(command.commandType()).isEqualTo(commandType);
        assertThat(command.payload()).isEmpty();
        assertThat(command.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void constructor_shouldHandleNullPayload() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        String commandType = "NullPayloadCommand";
        Instant createdAt = Instant.now();

        // When
        SagaCommand command = new SagaCommand(sagaId, commandType, null, createdAt);

        // Then
        assertThat(command.getSagaId()).isEqualTo(sagaId);
        assertThat(command.commandType()).isEqualTo(commandType);
        assertThat(command.payload()).isNull();
        assertThat(command.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void equals_shouldWorkCorrectly() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        String commandType = "TestCommand";
        Map<String, Object> payload = Map.of("key", "value");
        Instant createdAt = Instant.parse("2023-01-01T10:00:00Z");

        SagaCommand command1 = new SagaCommand(sagaId, commandType, payload, createdAt);
        SagaCommand command2 = new SagaCommand(sagaId, commandType, payload, createdAt);
        SagaCommand command3 = new SagaCommand(SagaId.of("different-id"), commandType, payload, createdAt);

        // Then
        assertThat(command1).isEqualTo(command2);
        assertThat(command1).isNotEqualTo(command3);
        assertThat(command1).isNotEqualTo(null);
        assertThat(command1).isNotEqualTo("not-a-command");
    }

    @Test
    void hashCode_shouldWorkCorrectly() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        String commandType = "TestCommand";
        Map<String, Object> payload = Map.of("key", "value");
        Instant createdAt = Instant.parse("2023-01-01T10:00:00Z");

        SagaCommand command1 = new SagaCommand(sagaId, commandType, payload, createdAt);
        SagaCommand command2 = new SagaCommand(sagaId, commandType, payload, createdAt);

        // Then
        assertThat(command1.hashCode()).isEqualTo(command2.hashCode());
    }

    @Test
    void toString_shouldIncludeAllFields() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        String commandType = "TestCommand";
        Map<String, Object> payload = Map.of("key", "value");
        Instant createdAt = Instant.parse("2023-01-01T10:00:00Z");

        SagaCommand command = new SagaCommand(sagaId, commandType, payload, createdAt);

        // When
        String toString = command.toString();

        // Then
        assertThat(toString).contains("SagaCommand");
        assertThat(toString).contains("test-saga-id");
        assertThat(toString).contains("TestCommand");
        assertThat(toString).contains("2023-01-01T10:00:00Z");
    }

    @Test
    void recordAccessors_shouldWork() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        String commandType = "TestCommand";
        Map<String, Object> payload = Map.of("action", "process", "priority", 1);
        Instant createdAt = Instant.now();

        // When
        SagaCommand command = new SagaCommand(sagaId, commandType, payload, createdAt);

        // Then
        assertThat(command.getSagaId()).isEqualTo(sagaId);
        assertThat(command.commandType()).isEqualTo(commandType);
        assertThat(command.payload()).hasSize(2);
        assertThat(command.payload().get("action")).isEqualTo("process");
        assertThat(command.payload().get("priority")).isEqualTo(1);
        assertThat(command.createdAt()).isEqualTo(createdAt);
    }
}