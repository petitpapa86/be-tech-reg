package com.bcbs239.regtech.core.saga;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SagaStartedEventTest {

    @Test
    void constructor_shouldCreateEventWithCorrectFields() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        String sagaType = "TestSaga";
        Instant occurredAt = Instant.now();

        // When
        SagaStartedEvent event = new SagaStartedEvent(sagaId, sagaType, () -> occurredAt);

        // Then
        assertThat(event.getSagaId()).isEqualTo(sagaId);
        assertThat(event.eventType()).isEqualTo("SagaStarted");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(event.getSagaType()).isEqualTo(sagaType);
    }

    @Test
    void constructor_shouldHandleDifferentSagaTypes() {
        // Given
        SagaId sagaId = SagaId.of("another-saga-id");
        String sagaType = "AnotherSaga";
        Instant occurredAt = Instant.parse("2023-01-01T10:00:00Z");

        // When
        SagaStartedEvent event = new SagaStartedEvent(sagaId, sagaType, () -> occurredAt);

        // Then
        assertThat(event.getSagaId()).isEqualTo(sagaId);
        assertThat(event.eventType()).isEqualTo("SagaStarted");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(event.getSagaType()).isEqualTo(sagaType);
    }

    @Test
    void equals_shouldWorkCorrectly() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        String sagaType = "TestSaga";
        Instant occurredAt = Instant.now();

        // When
        SagaStartedEvent event1 = new SagaStartedEvent(sagaId, sagaType, () -> occurredAt);
        SagaStartedEvent event2 = new SagaStartedEvent(sagaId, sagaType, () -> occurredAt);
        SagaStartedEvent event3 = new SagaStartedEvent(SagaId.of("different-id"), sagaType, () -> occurredAt);

        // Then
        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
    }

    @Test
    void hashCode_shouldWorkCorrectly() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        String sagaType = "TestSaga";
        Instant occurredAt = Instant.now();

        // When
        SagaStartedEvent event1 = new SagaStartedEvent(sagaId, sagaType, () -> occurredAt);
        SagaStartedEvent event2 = new SagaStartedEvent(sagaId, sagaType, () -> occurredAt);

        // Then
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void toString_shouldIncludeAllFields() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        String sagaType = "TestSaga";
        Instant occurredAt = Instant.parse("2023-01-01T10:00:00Z");

        // When
        SagaStartedEvent event = new SagaStartedEvent(sagaId, sagaType, () -> occurredAt);

        // Then
        assertThat(event.toString()).contains("test-saga-id");
        assertThat(event.toString()).contains("SagaStarted");
        assertThat(event.toString()).contains("TestSaga");
    }
}