# Event Processing Tables Schema Migration Fix

## Problem

The application was failing with the following error:

```
java.sql.BatchUpdateException: Batch entry 0 insert into public.outbox_messages 
(content,dead_letter_time,last_error,next_retry_time,occurred_on_utc,processed_on_utc,
retry_count,status,type,updated_at,id) values (...) was aborted: 
ERRORE: il valore nullo nella colonna "aggregate_type" della relazione "outbox_messages" 
viola il vincolo non nullo
```

## Root Cause

**Schema Mismatch** between the database migrations (V4) and current JPA entities. The V4 migration created an outdated schema that doesn't match the evolved entity structure.

### Affected Tables

1. **outbox_messages** - Transactional outbox pattern
2. **inbox_messages** - Inbox pattern for idempotent event processing
3. **sagas** - Saga orchestration state

## Detailed Analysis

### 1. Outbox Messages Table

### Old Schema (V4__create_core_event_tables.sql)
- `id` (BIGSERIAL)
- `aggregate_type` (VARCHAR, NOT NULL) ❌
- `aggregate_id` (VARCHAR, NOT NULL) ❌
- `event_type` (VARCHAR, NOT NULL) ❌
- `payload` (TEXT)
- `created_at` (TIMESTAMP)
- `processed_at` (TIMESTAMP)
- `processed` (BOOLEAN)

### Current Entity (OutboxMessageEntity.java)
- `id` (VARCHAR UUID)
- `type` (VARCHAR, NOT NULL) ✓
- `content` (TEXT, NOT NULL) ✓
- `status` (ENUM, NOT NULL) ✓
- `occurred_on_utc` (TIMESTAMP, NOT NULL) ✓
- `processed_on_utc` (TIMESTAMP) ✓
- `retry_count` (INTEGER) ✓
- `next_retry_time` (TIMESTAMP) ✓
- `last_error` (TEXT) ✓
- `dead_letter_time` (TIMESTAMP) ✓
- `updated_at` (TIMESTAMP, NOT NULL) ✓

### 2. Inbox Messages Table

**Old Schema (V4):**
- `id` (BIGSERIAL)
- `message_id` (VARCHAR, UNIQUE)
- `event_type` (VARCHAR)
- `payload` (TEXT)
- `received_at` (TIMESTAMP)
- `processed_at` (TIMESTAMP)
- `processed` (BOOLEAN)

**Current Entity (InboxMessageEntity):**
- `id` (VARCHAR UUID)
- `version` (BIGINT)
- `event_id` (VARCHAR, UNIQUE)
- `event_type` (VARCHAR)
- `event_data` (TEXT)
- `received_at` (TIMESTAMP)
- `processed_at` (TIMESTAMP)
- `processing_status` (ENUM)
- `aggregate_id` (VARCHAR)
- `error_message` (TEXT)
- `retry_count` (INTEGER)
- `next_retry_at` (TIMESTAMP)

### 3. Sagas Table

**Old Schema (V4):**
- `id` (VARCHAR)
- `saga_type` (VARCHAR)
- `current_step` (VARCHAR)
- `status` (VARCHAR)
- `payload` (TEXT)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)
- `completed_at` (TIMESTAMP)

**Current Entity (SagaEntity):**
- `saga_id` (VARCHAR)
- `saga_type` (VARCHAR)
- `status` (ENUM)
- `started_at` (TIMESTAMP)
- `completed_at` (TIMESTAMP)
- `timeout_at` (TIMESTAMP)
- `saga_data` (TEXT)
- `processed_events` (TEXT)
- `pending_commands` (TEXT)
- `version` (BIGINT)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

## Solution

Created migration **V6__Migrate_event_processing_tables_schema.sql** that:

1. Drops all three old tables (`outbox_messages`, `inbox_messages`, `sagas`)
2. Recreates them with correct schemas matching current entities
3. Adds proper indexes for performance
4. Updates table comments

## Migration Steps

1. **Backup any existing data** (if needed)
2. Run the migration:
   ```bash
   mvn flyway:migrate
   ```
3. Verify the schema:
   ```sql
   \d public.outbox_messages
   ```

## Expected Result

After migration, all three tables will match their respective entities:

### Outbox Messages
```sql
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
```

### Inbox Messages
```sql
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
```

### Sagas
```sql
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
```

## Testing

After applying the migration, test with:

1. **Outbox Pattern**: User registration (triggers `UserRegisteredEvent`)
2. **Inbox Pattern**: Process incoming integration events
3. **Saga Pattern**: Create subscription (triggers saga orchestration)
4. Check processing logs for all three patterns
5. Verify no constraint violations

## Notes

- This is a **breaking migration** that drops all three tables
- If you have production data, create a data migration script first
- The old schema (V4) appears to be from an earlier design iteration
- All modules using event-driven patterns will now work correctly
- The migration is idempotent (uses DROP IF EXISTS)

## Why This Happened

The V4 migration was likely created early in development and never updated as the entity models evolved. This is a common issue in projects where:
- Database migrations are created before finalizing entity design
- Entity models evolve but migrations aren't updated
- Different team members work on migrations vs entities

## Prevention

To prevent this in the future:
1. Generate migrations from entities (use tools like Flyway or Liquibase with JPA)
2. Add integration tests that verify schema matches entities
3. Review migrations when entities change
4. Use schema validation in tests (`spring.jpa.hibernate.ddl-auto=validate`)

## Related Files

- `regtech-app/src/main/resources/db/migration/common/V4__create_core_event_tables.sql` (old schema)
- `regtech-app/src/main/resources/db/migration/common/V6__Migrate_event_processing_tables_schema.sql` (new migration)
- `regtech-core/infrastructure/src/main/java/com/bcbs239/regtech/core/infrastructure/eventprocessing/OutboxMessageEntity.java`
- `regtech-core/infrastructure/src/main/java/com/bcbs239/regtech/core/infrastructure/eventprocessing/InboxMessageEntity.java`
- `regtech-core/infrastructure/src/main/java/com/bcbs239/regtech/core/infrastructure/saga/SagaEntity.java`
- `regtech-core/domain/src/main/java/com/bcbs239/regtech/core/domain/outbox/OutboxMessage.java`
