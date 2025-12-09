-- V2__create_core_event_tables.sql
-- Create core event processing tables in public schema
-- These tables support the event-driven architecture and saga pattern

-- Outbox Messages table - stores domain events for reliable publishing
CREATE TABLE IF NOT EXISTS public.outbox_messages (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_outbox_messages_processed ON public.outbox_messages(processed);
CREATE INDEX IF NOT EXISTS idx_outbox_messages_created_at ON public.outbox_messages(created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_messages_aggregate ON public.outbox_messages(aggregate_type, aggregate_id);

-- Inbox Messages table - stores incoming events for idempotent processing
CREATE TABLE IF NOT EXISTS public.inbox_messages (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_inbox_messages_processed ON public.inbox_messages(processed);
CREATE INDEX IF NOT EXISTS idx_inbox_messages_received_at ON public.inbox_messages(received_at);
CREATE INDEX IF NOT EXISTS idx_inbox_messages_message_id ON public.inbox_messages(message_id);

-- Event Processing Failures table - stores failed event processing attempts
CREATE TABLE IF NOT EXISTS public.event_processing_failures (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    error_message TEXT NOT NULL,
    stack_trace TEXT,
    failed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_retry_at TIMESTAMP,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_event_failures_resolved ON public.event_processing_failures(resolved);
CREATE INDEX IF NOT EXISTS idx_event_failures_failed_at ON public.event_processing_failures(failed_at);
CREATE INDEX IF NOT EXISTS idx_event_failures_event_type ON public.event_processing_failures(event_type);

-- Sagas table - stores saga orchestration state
CREATE TABLE IF NOT EXISTS public.sagas (
    id VARCHAR(255) PRIMARY KEY,
    saga_type VARCHAR(255) NOT NULL,
    current_step VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sagas_status ON public.sagas(status);
CREATE INDEX IF NOT EXISTS idx_sagas_saga_type ON public.sagas(saga_type);
CREATE INDEX IF NOT EXISTS idx_sagas_created_at ON public.sagas(created_at);

-- Comments for documentation
COMMENT ON TABLE public.outbox_messages IS 'Stores domain events for reliable publishing using the transactional outbox pattern';
COMMENT ON TABLE public.inbox_messages IS 'Stores incoming events for idempotent processing using the inbox pattern';
COMMENT ON TABLE public.event_processing_failures IS 'Stores failed event processing attempts for monitoring and retry';
COMMENT ON TABLE public.sagas IS 'Stores saga orchestration state for distributed transactions';
