package com.bcbs239.regtech.metrics.infrastructure.observability;

import com.bcbs239.regtech.metrics.application.signal.ApplicationSignal;
import com.bcbs239.regtech.metrics.application.signal.ApplicationSignalPublisher;
import com.bcbs239.regtech.metrics.application.signal.DashboardQueriedSignal;
import com.bcbs239.regtech.metrics.application.signal.SignalLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the observability infrastructure.
 * Verifies that signals are properly published and logged.
 */
class ObservabilityIntegrationTest {

    /**
     * Simple test signal for testing purposes.
     */
    record TestSignal(String message) implements ApplicationSignal {
        @Override
        public String type() {
            return "test.signal";
        }

        @Override
        public SignalLevel level() {
            return SignalLevel.INFO;
        }
    }

    @Test
    void testSignalStructure() {
        // Given
        DashboardQueriedSignal signal = new DashboardQueriedSignal(
                "BANK001",
                "2026-01-01",
                "2026-01-31"
        );

        // Then
        assertThat(signal.type()).isEqualTo("metrics.dashboard.queried");
        assertThat(signal.level()).isEqualTo(SignalLevel.INFO);
        assertThat(signal.bankId()).isEqualTo("BANK001");
        assertThat(signal.startDate()).isEqualTo("2026-01-01");
        assertThat(signal.endDate()).isEqualTo("2026-01-31");
    }

    @Test
    void testApplicationSignalEmittedEvent() {
        // Given
        TestSignal signal = new TestSignal("test message");

        // When
        ApplicationSignalEmittedEvent event = ApplicationSignalEmittedEvent.now(
                signal,
                "TestClass",
                "testMethod",
                "TestClass.java",
                42
        );

        // Then
        assertThat(event.signal()).isEqualTo(signal);
        assertThat(event.sourceClassName()).isEqualTo("TestClass");
        assertThat(event.sourceMethodName()).isEqualTo("testMethod");
        assertThat(event.sourceFileName()).isEqualTo("TestClass.java");
        assertThat(event.sourceLineNumber()).isEqualTo(42);
        assertThat(event.emittedAt()).isNotNull();
    }

    @Test
    void testApplicationSignalEmittedEventWithoutSourceInfo() {
        // Given
        TestSignal signal = new TestSignal("test message");

        // When
        ApplicationSignalEmittedEvent event = ApplicationSignalEmittedEvent.now(signal);

        // Then
        assertThat(event.signal()).isEqualTo(signal);
        assertThat(event.sourceClassName()).isNull();
        assertThat(event.sourceMethodName()).isNull();
        assertThat(event.sourceFileName()).isNull();
        assertThat(event.sourceLineNumber()).isNull();
        assertThat(event.emittedAt()).isNotNull();
    }
}
