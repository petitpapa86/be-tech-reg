# Migration Execution Guide - V6 Event Tables Schema Fix

## Quick Summary

The application is failing because the database schema (created by V4 migration) doesn't match the current JPA entities. Migration V6 fixes this by recreating the tables with the correct structure.

## Pre-Migration Checklist

- [ ] **Backup your database** (especially if you have production data)
- [ ] Stop the application
- [ ] Check current Flyway migration status
- [ ] Review the V6 migration file

## Step 1: Check Current Migration Status

```bash
mvn flyway:info
```

You should see V4 and V5 as applied. V6 should be pending.

## Step 2: Backup Data (if needed)

If you have important data in these tables, backup first:

```sql
-- Backup outbox_messages
CREATE TABLE outbox_messages_backup AS SELECT * FROM public.outbox_messages;

-- Backup inbox_messages
CREATE TABLE inbox_messages_backup AS SELECT * FROM public.inbox_messages;

-- Backup sagas
CREATE TABLE sagas_backup AS SELECT * FROM public.sagas;
```

## Step 3: Run the Migration

### Option A: Using Maven

```bash
mvn flyway:migrate
```

### Option B: Using Spring Boot

```bash
mvn spring-boot:run
```

The migration will run automatically on startup if `spring.flyway.enabled=true`.

### Option C: Manual SQL Execution

If you prefer to run manually:

```bash
psql -U your_username -d your_database -f regtech-app/src/main/resources/db/migration/common/V6__Migrate_event_processing_tables_schema.sql
```

## Step 4: Verify the Migration

Run the verification script:

```bash
psql -U your_username -d your_database -f verify-event-tables-schema.sql
```

Expected output should show:

### outbox_messages columns:
- id (character varying, 36)
- type (character varying, 255)
- content (text)
- status (character varying, 50)
- occurred_on_utc (timestamp)
- processed_on_utc (timestamp)
- retry_count (integer)
- next_retry_time (timestamp)
- last_error (text)
- dead_letter_time (timestamp)
- updated_at (timestamp)

### inbox_messages columns:
- id (character varying, 36)
- version (bigint)
- event_id (character varying, 100)
- event_type (character varying, 100)
- event_data (text)
- received_at (timestamp)
- processed_at (timestamp)
- processing_status (character varying, 50)
- aggregate_id (character varying, 255)
- error_message (text)
- retry_count (integer)
- next_retry_at (timestamp)

### sagas columns:
- saga_id (character varying, 255)
- saga_type (character varying, 255)
- status (character varying, 50)
- started_at (timestamp)
- completed_at (timestamp)
- timeout_at (timestamp)
- saga_data (text)
- processed_events (text)
- pending_commands (text)
- version (bigint)
- created_at (timestamp)
- updated_at (timestamp)

## Step 5: Test the Application

Start the application and test:

### Test 1: Outbox Pattern (User Registration)
```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!",
    "bankId": "your-bank-id"
  }'
```

Check logs for:
- ✅ No "aggregate_type" constraint violations
- ✅ Event saved to outbox_messages
- ✅ Event processed successfully

### Test 2: Inbox Pattern (Integration Events)
Check that incoming events are processed without errors.

### Test 3: Saga Pattern (Subscription Creation)
```bash
curl -X POST http://localhost:8080/api/v1/billing/subscriptions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -d '{
    "planId": "plan-id",
    "paymentMethodId": "pm-id"
  }'
```

Check logs for:
- ✅ Saga created successfully
- ✅ Saga state persisted
- ✅ No schema errors

## Step 6: Monitor Logs

Watch for any errors:

```bash
tail -f regtech-app-run.log | grep -i "error\|exception"
```

## Troubleshooting

### Issue: Migration fails with "relation already exists"

**Solution:** The migration uses `DROP TABLE IF EXISTS`, so this shouldn't happen. If it does:

```sql
DROP TABLE public.outbox_messages CASCADE;
DROP TABLE public.inbox_messages CASCADE;
DROP TABLE public.sagas CASCADE;
```

Then re-run the migration.

### Issue: Application still shows old schema errors

**Solution:** 
1. Verify migration was applied: `SELECT * FROM flyway_schema_history WHERE version = '6';`
2. Check table structure: `\d public.outbox_messages`
3. Restart the application

### Issue: Data loss after migration

**Solution:** Restore from backup:

```sql
INSERT INTO public.outbox_messages SELECT * FROM outbox_messages_backup;
-- (Note: You'll need to transform the data to match the new schema)
```

## Rollback (if needed)

If you need to rollback:

1. Stop the application
2. Delete the V6 migration record:
   ```sql
   DELETE FROM flyway_schema_history WHERE version = '6';
   ```
3. Restore the old schema (run V4 migration manually)
4. Restore data from backups

## Success Criteria

✅ All three tables have correct schema
✅ No constraint violation errors in logs
✅ User registration works
✅ Events are processed successfully
✅ Sagas execute without errors

## Next Steps

After successful migration:
1. Monitor application for 24 hours
2. Remove backup tables (if created)
3. Update any documentation referencing old schema
4. Consider adding schema validation tests

## Support

If you encounter issues:
1. Check the logs in `regtech-app-run.log`
2. Review `OUTBOX_SCHEMA_MIGRATION_FIX.md` for detailed analysis
3. Verify entity-schema alignment using JPA validation
