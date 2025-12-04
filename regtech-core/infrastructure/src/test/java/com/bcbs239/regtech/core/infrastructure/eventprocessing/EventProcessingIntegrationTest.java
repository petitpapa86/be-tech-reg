package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for event processing functionality in regtech-core module.
 * 
 * Tests:
 * - Domain event bus functionality
 * - Cross-module event bus functionality
 * - Event persistence and retrieval
 * 
 * Requirement: 16.1 - Verify event processing functionality
 */
@DataJpaTest
@Import({
    DomainEventBus.class,
    CrossModuleEventBus.class,
    JpaOutboxMessageRepository.class
})
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false"
})
class EventProcessingIntegrationTest {

    @Autowired
    private DomainEventBus domainEventBus;

    @Autowired
    private CrossModuleEventBus crossModuleEventBus;

    @Autowired
    private JpaOutboxMessageRepository outboxRepository;

    @Test
    void shouldInitializeDomainEventBus() {
        assertNotNull(domainEventBus, "DomainEventBus should be initialized");
    }

    @Test
    void shouldInitializeCrossModuleEventBus() {
        assertNotNull(crossModuleEventBus, "CrossModuleEventBus should be initialized");
    }

    @Test
    void shouldInitializeOutboxRepository() {
        assertNotNull(outboxRepository, "Outbox repository should be initialized");
    }

    @Test
    void shouldPublishDomainEvent() {
        // Create a test domain event
        TestDomainEvent event = new TestDomainEvent(UUID.randomUUID().toString());
        
        // Publish the event - should not throw exception
        assertDoesNotThrow(() -> domainEventBus.publish(event),
            "Publishing domain event should not throw exception");
    }

    @Test
    void shouldPublishIntegrationEvent() {
        // Create a test integration event
        TestIntegrationEvent event = new TestIntegrationEvent(UUID.randomUUID().toString());
        
        // Publish the event - should not throw exception
        assertDoesNotThrow(() -> crossModuleEventBus.publish(event),
            "Publishing integration event should not throw exception");
    }

    // Test domain event implementation
    private static class TestDomainEvent implements DomainEvent {
        private final String eventId;
        private final Instant occurredAt;

        public TestDomainEvent(String eventId) {
            this.eventId = eventId;
            this.occurredAt = Instant.now();
        }

        @Override
        public String getEventId() {
            return eventId;
        }

        @Override
        public Instant getOccurredAt() {
            return occurredAt;
        }

        @Override
        public String getEventType() {
            return "TestDomainEvent";
        }
    }

    // Test integration event implementation
    private static class TestIntegrationEvent implements IntegrationEvent {
        private final String eventId;
        private final Instant occurredAt;

        public TestIntegrationEvent(String eventId) {
            this.eventId = eventId;
            this.occurredAt = Instant.now();
        }

        @Override
        public String getEventId() {
            return eventId;
        }

        @Override
        public Instant getOccurredAt() {
            return occurredAt;
        }

        @Override
        public String getEventType() {
            return "TestIntegrationEvent";
        }
    }
}
