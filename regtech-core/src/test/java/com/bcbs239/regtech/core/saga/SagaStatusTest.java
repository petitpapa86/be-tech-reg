package com.bcbs239.regtech.core.saga;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SagaStatus enum.
 * Demonstrates functional programming principles:
 * - Pure data types (enums)
 * - Pattern matching readiness
 */
class SagaStatusTest {

    @Test
    void enumValues_shouldContainAllExpectedStatuses() {
        // When
        SagaStatus[] values = SagaStatus.values();

        // Then
        assertThat(values).hasSize(5);
        assertThat(values).containsExactlyInAnyOrder(
                SagaStatus.STARTED,
                SagaStatus.IN_PROGRESS,
                SagaStatus.COMPLETED,
                SagaStatus.FAILED,
                SagaStatus.COMPENSATING
        );
    }

    @Test
    void started_shouldBeDefined() {
        // When
        SagaStatus status = SagaStatus.STARTED;

        // Then
        assertThat(status).isNotNull();
        assertThat(status.name()).isEqualTo("STARTED");
    }

    @Test
    void inProgress_shouldBeDefined() {
        // When
        SagaStatus status = SagaStatus.IN_PROGRESS;

        // Then
        assertThat(status).isNotNull();
        assertThat(status.name()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void completed_shouldBeDefined() {
        // When
        SagaStatus status = SagaStatus.COMPLETED;

        // Then
        assertThat(status).isNotNull();
        assertThat(status.name()).isEqualTo("COMPLETED");
    }

    @Test
    void failed_shouldBeDefined() {
        // When
        SagaStatus status = SagaStatus.FAILED;

        // Then
        assertThat(status).isNotNull();
        assertThat(status.name()).isEqualTo("FAILED");
    }

    @Test
    void compensating_shouldBeDefined() {
        // When
        SagaStatus status = SagaStatus.COMPENSATING;

        // Then
        assertThat(status).isNotNull();
        assertThat(status.name()).isEqualTo("COMPENSATING");
    }

    @Test
    void valueOf_shouldWorkForAllStatuses() {
        // When & Then
        assertThat(SagaStatus.valueOf("STARTED")).isEqualTo(SagaStatus.STARTED);
        assertThat(SagaStatus.valueOf("IN_PROGRESS")).isEqualTo(SagaStatus.IN_PROGRESS);
        assertThat(SagaStatus.valueOf("COMPLETED")).isEqualTo(SagaStatus.COMPLETED);
        assertThat(SagaStatus.valueOf("FAILED")).isEqualTo(SagaStatus.FAILED);
        assertThat(SagaStatus.valueOf("COMPENSATING")).isEqualTo(SagaStatus.COMPENSATING);
    }

    @Test
    void toString_shouldReturnName() {
        // When & Then
        assertThat(SagaStatus.STARTED.toString()).isEqualTo("STARTED");
        assertThat(SagaStatus.IN_PROGRESS.toString()).isEqualTo("IN_PROGRESS");
        assertThat(SagaStatus.COMPLETED.toString()).isEqualTo("COMPLETED");
        assertThat(SagaStatus.FAILED.toString()).isEqualTo("FAILED");
        assertThat(SagaStatus.COMPENSATING.toString()).isEqualTo("COMPENSATING");
    }
}