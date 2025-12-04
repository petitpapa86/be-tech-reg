# URGENT: Apply V6 Migration to Fix aggregate_type Error

## The Problem
Your database still has the OLD schema. Flyway didn't run V6 automatically. You need to apply it manually.

## Quick Fix (Choose ONE method)

### Method 1: Using DBeaver / pgAdmin (EASIEST)

1. Open DBeaver or pgAdmin
2. Connect to your `regtech` database
3. Open a new SQL editor
4. Copy and paste this SQL:

```sql
-- V6__Migrate_event_processing_tables_schema.sql
-- Migrate event processing tables to match current entity structures

-- ============================================================================
-- 1. OUTBOX MESSAGES TABLE
-- ============================================================================
DROP TABLE IF EXISTS public.outbox_messages CASCADE;

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

CREATE INDEX idx_outbox_messages_status ON public.outbox_messages(status);
CREATE INDEX idx_outbox_messages_occurred_on_utc ON public.outbox_messages(occurred_on_utc);
CREATE INDEX idx_outbox_messages_type ON public.outbox_messages(type);

COMMENT ON TABLE public.outbox_messages IS 'Stores domain events for reliable publishing using the transactional outbox pattern (updated schema)';

-- ============================================================================
-- 2. INBOX MESSAGES TABLE
-- ============================================================================
DROP TABLE IF EXISTS public.inbox_messages CASCADE;

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

CREATE INDEX idx_inbox_messages_status_received ON public.inbox_messages(processing_status, received_at);
CREATE INDEX idx_inbox_messages_event_type ON public.inbox_messages(event_type);
CREATE INDEX idx_inbox_messages_aggregate_id ON public.inbox_messages(aggregate_id);
CREATE INDEX idx_inbox_messages_event_id ON public.inbox_messages(event_id);

COMMENT ON TABLE public.inbox_messages IS 'Stores incoming events for idempotent processing using the inbox pattern (updated schema)';

-- ============================================================================
-- 3. SAGAS TABLE
-- ============================================================================
DROP TABLE IF EXISTS public.sagas CASCADE;

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

CREATE INDEX idx_sagas_status ON public.sagas(status);
CREATE INDEX idx_sagas_saga_type ON public.sagas(saga_type);
CREATE INDEX idx_sagas_created_at ON public.sagas(created_at);

COMMENT ON TABLE public.sagas IS 'Stores saga orchestration state for distributed transactions (updated schema)';
```

5. Execute the SQL (F5 or Execute button)
6. Then run this to update Flyway history:

```sql
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
VALUES (
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history), 
    '6', 
    'Migrate event processing tables schema', 
    'SQL', 
    'V6__Migrate_outbox_messages_schema.sql', 
    0, 
    'postgres', 
    0, 
    true
);
```

7. Restart your application
8. Test user registration - it should work!

### Method 2: Using Maven Flyway Plugin

If you have PostgreSQL connection configured in pom.xml:

```bash
cd regtech-app
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/regtech -Dflyway.user=postgres -Dflyway.password=YOUR_PASSWORD
```

### Method 3: Let Spring Boot Do It (if Flyway is enabled)

Check your `application.yml` - make sure:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

Then restart the app. But based on your logs, this didn't work automatically.

## Verification

After applying the migration, verify with:

```sql
-- Check table structure
\d public.outbox_messages

-- Should show columns: id, type, content, status, occurred_on_utc, etc.
-- NOT: aggregate_type, aggregate_id, event_type
```

## Why This Happened

Flyway should have run V6 automatically, but it didn't. Possible reasons:
1. Flyway might be looking in the wrong location
2. The migration file might not have been detected
3. There might be a checksum mismatch

## After Migration

Once applied, your user registration will work without the `aggregate_type` error!
