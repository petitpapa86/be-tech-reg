package com.bcbs239.regtech.core.infrastructure.saga;

import com.bcbs239.regtech.core.domain.saga.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for saga management in regtech-core module.
 * 
 * Tests:
 * - Saga persistence
 * - Saga retrieval
 * - Saga status transitions
 * 
 * Requirement: 16.1 - Verify saga management
 */
@DataJpaTest
@Import({JpaSagaRepository.class})
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false"
})
class SagaManagementIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaSagaRepository sagaRepository;

    @Test
    void shouldPersistSagaSnapshot() {
        // Create a saga snapshot
        SagaSnapshot snapshot = createTestSagaSnapshot();
        
        // Persist the snapshot
        var result = sagaRepository.save(snapshot);
        entityManager.flush();
        
        // Verify the snapshot was persisted
        assertTrue(result.isSuccess(), "Saga snapshot should be persisted successfully");
    }

    @Test
    void shouldRetrieveSagaSnapshotById() {
        // Create and persist a saga snapshot
        SagaSnapshot snapshot = createTestSagaSnapshot();
        sagaRepository.save(snapshot);
        entityManager.flush();
        entityManager.clear();
        
        // Retrieve the snapshot
        var maybeSnapshot = sagaRepository.load(snapshot.getSagaId());
        
        // Verify the snapshot was retrieved
        assertTrue(maybeSnapshot.isPresent(), "Saga snapshot should be retrievable by ID");
        SagaSnapshot retrieved = maybeSnapshot.getValue();
        assertEquals(snapshot.getSagaId(), retrieved.getSagaId(), "Saga IDs should match");
        assertEquals(snapshot.getSagaType(), retrieved.getSagaType(), "Saga types should match");
        assertEquals(snapshot.getStatus(), retrieved.getStatus(), "Saga statuses should match");
    }

    @Test
    void shouldUpdateSagaStatus() {
        // Create and persist a saga snapshot
        SagaSnapshot snapshot = createTestSagaSnapshot();
        sagaRepository.save(snapshot);
        entityManager.flush();
        entityManager.clear();
        
        // Create an updated snapshot with different status
        SagaSnapshot updatedSnapshot = new SagaSnapshot(
            snapshot.getSagaId(),
            snapshot.getSagaType(),
            SagaStatus.COMPLETED,
            snapshot.getStartedAt(),
            snapshot.getSagaData(),
            snapshot.getProcessedEvents(),
            snapshot.getPendingCommands(),
            Instant.now()
        );
        
        // Update the snapshot
        var result = sagaRepository.save(updatedSnapshot);
        entityManager.flush();
        entityManager.clear();
        
        // Verify the update was successful
        assertTrue(result.isSuccess(), "Saga snapshot update should be successful");
        
        // Retrieve and verify the updated snapshot
        var maybeUpdated = sagaRepository.load(snapshot.getSagaId());
        assertTrue(maybeUpdated.isPresent(), "Updated saga snapshot should be retrievable");
        SagaSnapshot retrieved = maybeUpdated.getValue();
        assertEquals(SagaStatus.COMPLETED, retrieved.getStatus(), 
            "Saga status should be updated to COMPLETED");
        assertNotNull(retrieved.getCompletedAt(), 
            "Completed timestamp should be set");
    }

    @Test
    void shouldHandleNonExistentSaga() {
        // Try to load a non-existent saga
        SagaId nonExistentId = SagaId.generate();
        var maybeSnapshot = sagaRepository.load(nonExistentId);
        
        // Verify the result is empty
        assertTrue(maybeSnapshot.isEmpty(), 
            "Loading non-existent saga should return empty result");
    }

    @Test
    void shouldPersistSagaWithProcessedEvents() {
        // Create a saga snapshot with processed events
        SagaSnapshot snapshot = new SagaSnapshot(
            SagaId.generate(),
            "TestSaga",
            SagaStatus.IN_PROGRESS,
            Instant.now(),
            "{\"testData\": \"value\"}",
            "[\"Event1\", \"Event2\"]",
            "[]",
            null
        );
        
        // Persist the snapshot
        var result = sagaRepository.save(snapshot);
        entityManager.flush();
        entityManager.clear();
        
        // Verify the snapshot was persisted with events
        assertTrue(result.isSuccess(), "Saga snapshot with events should be persisted");
        var maybeSnapshot = sagaRepository.load(snapshot.getSagaId());
        assertTrue(maybeSnapshot.isPresent(), "Saga snapshot should be retrievable");
        SagaSnapshot retrieved = maybeSnapshot.getValue();
        assertEquals("[\"Event1\", \"Event2\"]", retrieved.getProcessedEvents(), 
            "Processed events should be persisted");
    }

    @Test
    void shouldPersistSagaWithPendingCommands() {
        // Create a saga snapshot with pending commands
        SagaSnapshot snapshot = new SagaSnapshot(
            SagaId.generate(),
            "TestSaga",
            SagaStatus.IN_PROGRESS,
            Instant.now(),
            "{\"testData\": \"value\"}",
            "[]",
            "[{\"commandType\": \"TestCommand\"}]",
            null
        );
        
        // Persist the snapshot
        var result = sagaRepository.save(snapshot);
        entityManager.flush();
        entityManager.clear();
        
        // Verify the snapshot was persisted with commands
        assertTrue(result.isSuccess(), "Saga snapshot with commands should be persisted");
        var maybeSnapshot = sagaRepository.load(snapshot.getSagaId());
        assertTrue(maybeSnapshot.isPresent(), "Saga snapshot should be retrievable");
        SagaSnapshot retrieved = maybeSnapshot.getValue();
        assertEquals("[{\"commandType\": \"TestCommand\"}]", retrieved.getPendingCommands(), 
            "Pending commands should be persisted");
    }

    private SagaSnapshot createTestSagaSnapshot() {
        return new SagaSnapshot(
            SagaId.generate(),
            "TestSaga",
            SagaStatus.STARTED,
            Instant.now(),
            "{\"testData\": \"value\"}",
            "[]",
            "[]",
            null
        );
    }
}
