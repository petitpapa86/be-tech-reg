-- Verification script for event processing tables schema
-- Run this after applying V6 migration to verify the schema is correct

-- Check outbox_messages table structure
SELECT 
    'outbox_messages' as table_name,
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_schema = 'public' 
  AND table_name = 'outbox_messages'
ORDER BY ordinal_position;

-- Check inbox_messages table structure
SELECT 
    'inbox_messages' as table_name,
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_schema = 'public' 
  AND table_name = 'inbox_messages'
ORDER BY ordinal_position;

-- Check sagas table structure
SELECT 
    'sagas' as table_name,
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_schema = 'public' 
  AND table_name = 'sagas'
ORDER BY ordinal_position;

-- Check indexes
SELECT 
    tablename,
    indexname,
    indexdef
FROM pg_indexes 
WHERE schemaname = 'public' 
  AND tablename IN ('outbox_messages', 'inbox_messages', 'sagas')
ORDER BY tablename, indexname;

-- Expected columns for outbox_messages:
-- id, type, content, status, occurred_on_utc, processed_on_utc, 
-- retry_count, next_retry_time, last_error, dead_letter_time, updated_at

-- Expected columns for inbox_messages:
-- id, version, event_id, event_type, event_data, received_at, processed_at,
-- processing_status, aggregate_id, error_message, retry_count, next_retry_at

-- Expected columns for sagas:
-- saga_id, saga_type, status, started_at, completed_at, timeout_at,
-- saga_data, processed_events, pending_commands, version, created_at, updated_at
