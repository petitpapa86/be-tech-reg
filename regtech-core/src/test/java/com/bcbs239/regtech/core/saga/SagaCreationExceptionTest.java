package com.bcbs239.regtech.core.saga;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SagaCreationExceptionTest {

    @Test
    void constructor_shouldCreateExceptionWithMessageAndCause() {
        // Given
        String message = "Failed to create saga";
        Throwable cause = new RuntimeException("Root cause");

        // When
        SagaCreationException exception = new SagaCreationException(message, cause);

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void constructor_shouldHandleNullCause() {
        // Given
        String message = "Failed to create saga";

        // When
        SagaCreationException exception = new SagaCreationException(message, null);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }
}