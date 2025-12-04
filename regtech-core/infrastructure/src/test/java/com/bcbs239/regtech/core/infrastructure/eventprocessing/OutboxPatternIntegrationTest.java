package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for outbox pattern implementation in regtech-core module.
 * 
 * Tests:
 * - Outbox message persistence
 * - Outbox message retrieval
 * - Outbox message status transitions
 * 
 * Requirement: 16.1 - Test outbox pattern implementation
 */
@DataJpaTest
@Import({JpaOutboxMessageRepository.class})
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false"
})
class OutboxPatternIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaOutboxMessageRepository outboxRepository;

    @Autowired
    private OutboxMessageRepository springDataRepository;

    @Test
    void shouldPersistOutboxMessage() {
        // Create an outbox message
        OutboxMessageEntity message = createTestOutboxMessage();
        
        // Persist the message
        OutboxMessageEntity saved = springDataRepository.save(message);
        entityManager.flush();
        
        // Verify the message was persisted
        assertNotNull(saved.getId(), "Outbox message should have an ID after persistence");
        assertEquals(message.getEventType(), saved.getEventType(), 
            "Event type should match");
        assertEquals(message.getPayload(), saved.getPayload(), 
            "Payload should match");
    }

    @Test
    void shouldRetrieveOutboxMessageById() {
        // Create and persist an outbox message
        OutboxMessageEntity message = createTestOutboxMessage();
        OutboxMessageEntity saved = springDataRepository.save(message);
        entityManager.flush();
        entityManager.clear();
        
        // Retrieve the message
        OutboxMessageEntity retrieved = springDataRepository.findById(saved.getId()).orElse(null);
        
        // Verify the message was retrieved
        assertNotNull(retrieved, "Outbox message should be retrievable by ID");
        assertEquals(saved.getId(), retrieved.getId(), "IDs should match");
        assertEquals(saved.getEventType(), retrieved.getEventType(), 
            "Event types should match");
    }

    @Test
    void shouldFindPendingOutboxMessages() {
        // Create and persist multiple outbox messages with different statuses
        OutboxMessageEntity pending1 = createTestOutboxMessage();
        pending1.setStatus(OutboxMessageStatus.PENDING);
        springDataRepository.save(pending1);
        
        OutboxMessageEntity pending2 = createTestOutboxMessage();
        pending2.setStatus(OutboxMessageStatus.PENDING);
        springDataRepository.save(pending2);
        
        OutboxMessageEntity processed = createTestOutboxMessage();
        processed.setStatus(OutboxMessageStatus.PROCESSED);
        springDataRepository.save(processed);
        
        entityManager.flush();
        entityManager.clear();
        
        // Retrieve pending messages
        List<OutboxMessageEntity> pendingMessages = 
            springDataRepository.findByStatusOrderByCreatedAtAsc(OutboxMessageStatus.PENDING);
        
        // Verify only pending messages are retrieved
        assertEquals(2, pendingMessages.size(), 
            "Should retrieve exactly 2 pending messages");
        assertTrue(pendingMessages.stream()
            .allMatch(m -> m.getStatus() == OutboxMessageStatus.PENDING),
            "All retrieved messages should have PENDING status");
    }

    @Test
    void shouldUpdateOutboxMessageStatus() {
        // Create and persist an outbox message
        OutboxMessageEntity message = createTestOutboxMessage();
        message.setStatus(OutboxMessageStatus.PENDING);
        OutboxMessageEntity saved = springDataRepository.save(message);
        entityManager.flush();
        entityManager.clear();
        
        // Update the status
        OutboxMessageEntity retrieved = springDataRepository.findById(saved.getId()).orElseThrow();
        retrieved.setStatus(OutboxMessageStatus.PROCESSED);
        retrieved.setProcessedAt(Instant.now());
        springDataRepository.save(retrieved);
        entityManager.flush();
        entityManager.clear();
        
        // Verify the status was updated
        OutboxMessageEntity updated = springDataRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OutboxMessageStatus.PROCESSED, updated.getStatus(), 
            "Status should be updated to PROCESSED");
        assertNotNull(updated.getProcessedAt(), 
            "Processed timestamp should be set");
    }

    @Test
    void shouldHandleFailedOutboxMessages() {
        // Create and persist an outbox message
        OutboxMessageEntity message = createTestOutboxMessage();
        message.setStatus(OutboxMessageStatus.PENDING);
        OutboxMessageEntity saved = springDataRepository.save(message);
        entityManager.flush();
        entityManager.clear();
        
        // Mark as failed
        OutboxMessageEntity retrieved = springDataRepository.findById(saved.getId()).orElseThrow();
        retrieved.setStatus(OutboxMessageStatus.FAILED);
        retrieved.setProcessedAt(Instant.now());
        retrieved.setErrorMessage("Test error message");
        springDataRepository.save(retrieved);
        entityManager.flush();
        entityManager.clear();
        
        // Verify the failure was recorded
        OutboxMessageEntity failed = springDataRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OutboxMessageStatus.FAILED, failed.getStatus(), 
            "Status should be FAILED");
        assertNotNull(failed.getErrorMessage(), 
            "Error message should be set");
        assertEquals("Test error message", failed.getErrorMessage(), 
            "Error message should match");
    }

    private OutboxMessageEntity createTestOutboxMessage() {
        OutboxMessageEntity message = new OutboxMessageEntity();
        message.setEventId(UUID.randomUUID().toString());
        message.setEventType("TestEvent");
        message.setPayload("{\"test\": \"data\"}");
        message.setStatus(OutboxMessageStatus.PENDING);
        message.setCreatedAt(Instant.now());
        return message;
    }
}
