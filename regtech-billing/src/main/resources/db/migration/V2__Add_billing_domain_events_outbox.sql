-- Add billing domain events outbox table for reliable event delivery
-- This migration adds the outbox pattern table for domain events

-- Billing Domain Events table (Outbox Pattern)
CREATE TABLE billing_domain_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(255),
    source_module VARCHAR(50) NOT NULL,
    target_module VARCHAR(50),
    event_data TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT uk_billing_domain_events_event_id UNIQUE (event_id),
    CONSTRAINT chk_billing_domain_events_status CHECK (processing_status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED', 'DEAD_LETTER')),
    CONSTRAINT chk_billing_domain_events_retry_count CHECK (retry_count >= 0)
);

-- Performance indexes for outbox processing
CREATE INDEX idx_billing_domain_events_processing_status ON billing_domain_events(processing_status);
CREATE INDEX idx_billing_domain_events_created_at ON billing_domain_events(created_at);
CREATE INDEX idx_billing_domain_events_correlation_id ON billing_domain_events(correlation_id);
CREATE INDEX idx_billing_domain_events_target_module ON billing_domain_events(target_module);
CREATE INDEX idx_billing_domain_events_event_type ON billing_domain_events(event_type);

-- Composite index for finding pending events efficiently
CREATE INDEX idx_billing_domain_events_pending_processing ON billing_domain_events(processing_status, created_at) 
WHERE processing_status = 'PENDING';

-- Composite index for finding retryable failed events
CREATE INDEX idx_billing_domain_events_retryable_failed ON billing_domain_events(processing_status, retry_count, created_at) 
WHERE processing_status = 'FAILED';

-- Index for finding stale processing events
CREATE INDEX idx_billing_domain_events_stale_processing ON billing_domain_events(processing_status, created_at) 
WHERE processing_status = 'PROCESSING';