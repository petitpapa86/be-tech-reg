package com.bcbs239.regtech.core.sagav2;

import com.bcbs239.regtech.core.saga.AbstractSaga;
import com.bcbs239.regtech.core.saga.SagaClosures;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaCommand;
import com.bcbs239.regtech.core.saga.SagaMessage;
import com.bcbs239.regtech.core.saga.SagaStatus;

import java.time.Instant;
import java.util.Map;

public class TestSaga extends AbstractSaga<String> {

    private static final SagaClosures.TimeoutScheduler NOOP_SCHEDULER = new SagaClosures.TimeoutScheduler() {
        @Override
        public void schedule(String key, long delayMillis, Runnable task) {
            // no-op for tests
        }

        @Override
        public void cancel(String key) {
            // no-op for tests
        }
    };

    // Constructor used by JpaSagaRepository.fromEntity via reflection
    public TestSaga(SagaId id, String data) {
        super(id, "TestSaga", data, NOOP_SCHEDULER);
        initializeForTests();
    }

    // Constructor used by SagaManager.startSaga (SagaId, data, timeoutScheduler)
    public TestSaga(SagaId id, String data, SagaClosures.TimeoutScheduler timeoutScheduler) {
        super(id, "TestSaga", data, timeoutScheduler);
        initializeForTests();
    }

    private void initializeForTests() {
        // Dispatch an initialization command when saga is created
        dispatchCommand(new SagaCommand(getId(), "InitializeTestSaga", Map.of(), Instant.now()));

        // Register event handler for TestEvent
        onEvent(com.bcbs239.regtech.core.saga.SagaMessage.class, event -> {
            if ("ProcessingSuccessful".equals(event.eventType())) {
                complete();
            }
        });
    }

    @Override
    protected void updateStatus() {
        // no-op for tests
    }

    @Override
    protected void compensate() {
        // no-op for tests
    }
}
