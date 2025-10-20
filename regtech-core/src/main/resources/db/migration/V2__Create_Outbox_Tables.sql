-- V2__Create_Outbox_Tables.sql
-- Create outbox tables for reliable domain event publishing

-- Create outbox_messages table
CREATE TABLE regtech.outbox_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    occurred_on_utc TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_on_utc TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED')),
    error_message TEXT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on status for efficient querying
CREATE INDEX idx_outbox_messages_status ON regtech.outbox_messages(status);
CREATE INDEX idx_outbox_messages_occurred_on ON regtech.outbox_messages(occurred_on_utc);

-- Add comments
COMMENT ON TABLE regtech.outbox_messages IS 'Stores outgoing domain events for reliable publishing';