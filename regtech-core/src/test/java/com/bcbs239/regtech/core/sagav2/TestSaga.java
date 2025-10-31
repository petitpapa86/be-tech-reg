package com.bcbs239.regtech.core.sagav2;

import com.bcbs239.regtech.core.saga.AbstractSaga;
import com.bcbs239.regtech.core.saga.SagaClosures;
import com.bcbs239.regtech.core.saga.SagaId;

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

