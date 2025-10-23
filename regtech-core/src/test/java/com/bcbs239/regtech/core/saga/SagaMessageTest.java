package com.bcbs239.regtech.core.saga;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SagaMessageTest {

    @Test
    void sagaMessage_shouldImplementDomainEvent() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        Instant occurredAt = Instant.now();

        // When - using SagaStartedEvent as concrete implementation
        SagaStartedEvent event = new SagaStartedEvent(sagaId, "TestSaga", () -> occurredAt);

        // Then
        assertThat(event.eventType()).isEqualTo("SagaStarted");
        assertThat(event.getSagaId()).isEqualTo(sagaId);
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void equalsAndHashCode_shouldWorkCorrectly() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        Instant occurredAt = Instant.parse("2023-01-01T10:00:00Z");

        SagaStartedEvent event1 = new SagaStartedEvent(sagaId, "TestSaga", () -> occurredAt);
        SagaStartedEvent event2 = new SagaStartedEvent(sagaId, "TestSaga", () -> occurredAt);
        SagaStartedEvent event3 = new SagaStartedEvent(SagaId.of("different-id"), "TestSaga", () -> occurredAt);

        // Then
        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        assertThat(event1.hashCode()).isNotEqualTo(event3.hashCode());
    }

    @Test
    void toString_shouldIncludeAllFields() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        Instant occurredAt = Instant.parse("2023-01-01T10:00:00Z");

        // When
        SagaStartedEvent event = new SagaStartedEvent(sagaId, "TestSaga", () -> occurredAt);

        // Then
        String toString = event.toString();
        assertThat(toString).contains("sagaId=SagaId[id=test-saga-id]");
        assertThat(toString).contains("occurredAt=2023-01-01T10:00:00Z");
        assertThat(toString).contains("eventType=SagaStarted");
    }
}