-- V2__create_core_event_tables.sql
-- Create core event processing tables in core schema
-- These tables support the event-driven architecture and saga pattern

-- Outbox Messages table - stores domain events for reliable publishing
CREATE TABLE IF NOT EXISTS core.outbox_messages (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    occurred_on_utc TIMESTAMP NOT NULL,
    processed_on_utc TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_time TIMESTAMP,
    last_error TEXT,
    dead_letter_time TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_messages_status ON core.outbox_messages(status);
CREATE INDEX IF NOT EXISTS idx_outbox_messages_occurred_on_utc ON core.outbox_messages(occurred_on_utc);
CREATE INDEX IF NOT EXISTS idx_outbox_messages_type ON core.outbox_messages(type);

-- Inbox Messages table - stores incoming events for idempotent processing
CREATE TABLE IF NOT EXISTS core.inbox_messages (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT,
    event_id VARCHAR(100) UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    event_data TEXT NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    aggregate_id VARCHAR(255),
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inbox_messages_status_received ON core.inbox_messages(processing_status, received_at);
CREATE INDEX IF NOT EXISTS idx_inbox_messages_event_type ON core.inbox_messages(event_type);
CREATE INDEX IF NOT EXISTS idx_inbox_messages_aggregate_id ON core.inbox_messages(aggregate_id);
CREATE INDEX IF NOT EXISTS idx_inbox_messages_event_id ON core.inbox_messages(event_id);

-- Event Processing Failures table - stores failed event processing attempts
CREATE TABLE IF NOT EXISTS core.event_processing_failures (
    id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(500) NOT NULL,
    event_payload TEXT NOT NULL,
    metadata TEXT,
    error_message TEXT,
    error_stacktrace TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMP,
    last_error_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_event_failures_retry ON core.event_processing_failures(next_retry_at, status);
CREATE INDEX IF NOT EXISTS idx_event_failures_status ON core.event_processing_failures(status);

-- Sagas table - stores saga orchestration state
CREATE TABLE IF NOT EXISTS core.sagas (
    id VARCHAR(255) PRIMARY KEY,
    saga_type VARCHAR(255) NOT NULL,
    current_step VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sagas_status ON core.sagas(status);
CREATE INDEX IF NOT EXISTS idx_sagas_saga_type ON core.sagas(saga_type);
CREATE INDEX IF NOT EXISTS idx_sagas_created_at ON core.sagas(created_at);

-- Comments for documentation
COMMENT ON TABLE core.outbox_messages IS 'Stores domain events for reliable publishing using the transactional outbox pattern';
COMMENT ON TABLE core.inbox_messages IS 'Stores incoming events for idempotent processing using the inbox pattern';
COMMENT ON TABLE core.event_processing_failures IS 'Stores failed event processing attempts for monitoring and retry';
COMMENT ON TABLE core.sagas IS 'Stores saga orchestration state for distributed transactions';
