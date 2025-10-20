-- V1__Create_Inbox_Tables.sql
-- Create inbox tables for reliable integration event processing

-- Create inbox_messages table
CREATE TABLE regtech.inbox_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    occurred_on_utc TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_on_utc TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED')),
    error_message TEXT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on status for efficient querying
CREATE INDEX idx_inbox_messages_status ON regtech.inbox_messages(status);
CREATE INDEX idx_inbox_messages_occurred_on ON regtech.inbox_messages(occurred_on_utc);

-- Create inbox_message_consumers table for idempotency
CREATE TABLE regtech.inbox_message_consumers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inbox_message_id UUID NOT NULL REFERENCES regtech.inbox_messages(id) ON DELETE CASCADE,
    handler_name VARCHAR(500) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(inbox_message_id, handler_name)
);

-- Create index for efficient lookups
CREATE INDEX idx_inbox_consumers_message_id ON regtech.inbox_message_consumers(inbox_message_id);
CREATE INDEX idx_inbox_consumers_handler ON regtech.inbox_message_consumers(handler_name);

-- Add comments
COMMENT ON TABLE regtech.inbox_messages IS 'Stores incoming integration events for reliable processing';
COMMENT ON TABLE regtech.inbox_message_consumers IS 'Tracks which handlers have processed which inbox messages for idempotency';