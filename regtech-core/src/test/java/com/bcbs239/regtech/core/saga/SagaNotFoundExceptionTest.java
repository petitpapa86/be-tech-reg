package com.bcbs239.regtech.core.saga;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SagaNotFoundExceptionTest {

    @Test
    void constructor_shouldCreateExceptionWithCorrectMessage() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");

        // When
        SagaNotFoundException exception = new SagaNotFoundException(sagaId);

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo("Saga not found with id: test-saga-id");
    }

    @Test
    void constructor_shouldHandleDifferentSagaIds() {
        // Given
        SagaId sagaId = SagaId.of("another-saga-id");

        // When
        SagaNotFoundException exception = new SagaNotFoundException(sagaId);

        // Then
        assertThat(exception.getMessage()).isEqualTo("Saga not found with id: another-saga-id");
    }
}