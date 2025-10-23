package com.bcbs239.regtech.core.saga;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SagaId record.
 * Demonstrates functional programming principles:
 * - Immutable data structures (records)
 * - Pure functions for creation
 * - Validation at construction time
 */
class SagaIdTest {

    @Test
    void generate_shouldCreateUniqueIds() {
        // When
        SagaId id1 = SagaId.generate();
        SagaId id2 = SagaId.generate();

        // Then
        assertThat(id1).isNotNull();
        assertThat(id2).isNotNull();
        assertThat(id1.id()).isNotEqualTo(id2.id());
        assertThat(id1.id()).hasSize(36); // UUID length
        assertThat(id2.id()).hasSize(36);
    }

    @Test
    void of_shouldCreateSagaIdFromString() {
        // Given
        String idString = "test-saga-id-123";

        // When
        SagaId sagaId = SagaId.of(idString);

        // Then
        assertThat(sagaId).isNotNull();
        assertThat(sagaId.id()).isEqualTo(idString);
    }

    @Test
    void constructor_shouldValidateNonNullId() {
        // When & Then
        assertThatThrownBy(() -> new SagaId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SagaId cannot be null or empty");
    }

    @Test
    void constructor_shouldValidateNonEmptyId() {
        // When & Then
        assertThatThrownBy(() -> new SagaId(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SagaId cannot be null or empty");
    }

    @Test
    void constructor_shouldValidateNonBlankId() {
        // When & Then
        assertThatThrownBy(() -> new SagaId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SagaId cannot be null or empty");
    }

    @Test
    void id_shouldReturnCorrectValue() {
        // Given
        String expectedId = "expected-id-value";

        // When
        SagaId sagaId = new SagaId(expectedId);

        // Then
        assertThat(sagaId.id()).isEqualTo(expectedId);
    }

    @Test
    void equals_shouldWorkCorrectly() {
        // Given
        SagaId id1 = SagaId.of("same-id");
        SagaId id2 = SagaId.of("same-id");
        SagaId id3 = SagaId.of("different-id");

        // Then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1).isNotEqualTo(id3);
        assertThat(id1).isNotEqualTo(null);
        assertThat(id1).isNotEqualTo("not-a-saga-id");
    }

    @Test
    void hashCode_shouldWorkCorrectly() {
        // Given
        SagaId id1 = SagaId.of("same-id");
        SagaId id2 = SagaId.of("same-id");
        SagaId id3 = SagaId.of("different-id");

        // Then
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        assertThat(id1.hashCode()).isNotEqualTo(id3.hashCode());
    }

    @Test
    void toString_shouldReturnId() {
        // Given
        String idValue = "test-id-123";
        SagaId sagaId = SagaId.of(idValue);

        // When
        String toString = sagaId.toString();

        // Then
        assertThat(toString).contains(idValue);
    }
}