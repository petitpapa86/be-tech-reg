-- V6__Migrate_event_processing_tables_schema.sql
-- Migrate event processing tables to match current entity structures
-- This fixes schema mismatches causing constraint violations

-- ============================================================================
-- 1. OUTBOX MESSAGES TABLE
-- ============================================================================
-- Drop the old table (if you have important data, backup first!)
DROP TABLE IF EXISTS public.outbox_messages CASCADE;

-- Recreate with the correct schema matching OutboxMessageEntity
CREATE TABLE public.outbox_messages (
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
    updated_at TIMESTAMP NOT NULL
);

-- Create indexes for performance
CREATE INDEX idx_outbox_messages_status ON public.outbox_messages(status);
CREATE INDEX idx_outbox_messages_occurred_on_utc ON public.outbox_messages(occurred_on_utc);
CREATE INDEX idx_outbox_messages_type ON public.outbox_messages(type);

COMMENT ON TABLE public.outbox_messages IS 'Stores domain events for reliable publishing using the transactional outbox pattern (updated schema)';

-- ============================================================================
-- 2. INBOX MESSAGES TABLE
-- ============================================================================
DROP TABLE IF EXISTS public.inbox_messages CASCADE;

-- Recreate with the correct schema matching InboxMessageEntity
CREATE TABLE public.inbox_messages (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT,
    event_id VARCHAR(100) UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    event_data TEXT NOT NULL,
    received_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    processing_status VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(255),
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_inbox_messages_status_received ON public.inbox_messages(processing_status, received_at);
CREATE INDEX idx_inbox_messages_event_type ON public.inbox_messages(event_type);
CREATE INDEX idx_inbox_messages_aggregate_id ON public.inbox_messages(aggregate_id);
CREATE INDEX idx_inbox_messages_event_id ON public.inbox_messages(event_id);

COMMENT ON TABLE public.inbox_messages IS 'Stores incoming events for idempotent processing using the inbox pattern (updated schema)';

-- ============================================================================
-- 3. SAGAS TABLE
-- ============================================================================
DROP TABLE IF EXISTS public.sagas CASCADE;

-- Recreate with the correct schema matching SagaEntity
CREATE TABLE public.sagas (
    saga_id VARCHAR(255) PRIMARY KEY,
    saga_type VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    timeout_at TIMESTAMP,
    saga_data TEXT,
    processed_events TEXT,
    pending_commands TEXT,
    version BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create indexes for performance
CREATE INDEX idx_sagas_status ON public.sagas(status);
CREATE INDEX idx_sagas_saga_type ON public.sagas(saga_type);
CREATE INDEX idx_sagas_created_at ON public.sagas(created_at);

COMMENT ON TABLE public.sagas IS 'Stores saga orchestration state for distributed transactions (updated schema)';
