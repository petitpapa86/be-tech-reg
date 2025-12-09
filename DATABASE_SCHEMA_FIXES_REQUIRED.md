# Database Schema Fixes Required

## Overview

Your application has **TWO critical database schema mismatches** that are preventing it from running correctly. Both need to be fixed before the application can function properly.

## Issue 1: Outbox Messages Schema Mismatch ⚠️ CRITICAL

**Status:** Migration created (V6), NOT YET APPLIED

**Error:**
```
ERRORE: il valore nullo nella colonna "aggregate_type" della relazione "outbox_messages" viola il vincolo non nullo
```

**Problem:** The `public.outbox_messages` table schema doesn't match the `OutboxMessageEntity` JPA entity.

**Solution:** Apply V6 migration

**Documentation:** See `APPLY_MIGRATION_NOW.md`

**Quick Apply:**
```cmd
REM Windows CMD
apply-v6-migration.bat

REM Windows PowerShell
.\apply-v6-migration.ps1
```

---

## Issue 2: Subscriptions Schema Mismatch ⚠️ CRITICAL

**Status:** Migration created (V21), NOT YET APPLIED

**Error:**
```
ERRORE: il valore nullo nella colonna "plan_id" della relazione "subscriptions" viola il vincolo non nullo
```

**Problem:** The `billing.subscriptions` table schema doesn't match the `SubscriptionEntity` JPA entity.

**Solution:** Apply V21 migration

**Documentation:** See `SUBSCRIPTIONS_SCHEMA_FIX.md`

**Quick Apply:**
```cmd
REM Windows CMD
apply-v21-migration.bat

REM Windows PowerShell
.\apply-v21-migration.ps1
```

---

## Recommended Fix Order

Apply migrations in this order:

### Step 1: Fix Outbox Messages (V6)
```cmd
apply-v6-migration.bat
```

This fixes the event processing infrastructure that's needed for inter-module communication.

### Step 2: Fix Subscriptions (V21)
```cmd
apply-v21-migration.bat
```

This fixes the billing module's subscription creation during user registration.

### Step 3: Restart Application
```cmd
mvnw spring-boot:run
```

### Step 4: Test User Registration

Register a new user and verify:
1. User is created in IAM module
2. Outbox message is published
3. Billing account is created
4. Subscription is created with STARTER tier
5. No errors in logs

---

## What Each Migration Does

### V6 Migration (Outbox Messages)

**Affects 3 tables:**
- `public.outbox_messages` - Drops and recreates with correct schema
- `public.inbox_messages` - Drops and recreates with correct schema
- `public.sagas` - Drops and recreates with correct schema

**Schema Changes:**
```sql
-- OLD (V4 - incorrect)
CREATE TABLE outbox_messages (
    aggregate_type VARCHAR(255) NOT NULL,  -- ❌ Doesn't exist in entity
    aggregate_id VARCHAR(255) NOT NULL,    -- ❌ Doesn't exist in entity
    event_type VARCHAR(255) NOT NULL,      -- ❌ Doesn't exist in entity
    ...
);

-- NEW (V6 - correct)
CREATE TABLE outbox_messages (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(255) NOT NULL,            -- ✅ Matches entity
    content TEXT NOT NULL,                 -- ✅ Matches entity
    status VARCHAR(20) NOT NULL,           -- ✅ Matches entity
    occurred_on_utc TIMESTAMP NOT NULL,    -- ✅ Matches entity
    ...
);
```

### V21 Migration (Subscriptions)

**Affects 1 table:**
- `billing.subscriptions` - Drops and recreates with correct schema

**Schema Changes:**
```sql
-- OLD (V20 - incorrect)
CREATE TABLE billing.subscriptions (
    plan_id VARCHAR(100) NOT NULL,         -- ❌ Doesn't exist in entity
    current_period_start TIMESTAMP,        -- ❌ Doesn't exist in entity
    current_period_end TIMESTAMP,          -- ❌ Doesn't exist in entity
    cancel_at_period_end BOOLEAN,          -- ❌ Doesn't exist in entity
    ...
);

-- NEW (V21 - correct)
CREATE TABLE billing.subscriptions (
    tier VARCHAR(20) NOT NULL,             -- ✅ Matches entity (enum)
    start_date DATE NOT NULL,              -- ✅ Matches entity
    end_date DATE,                         -- ✅ Matches entity
    billing_account_id VARCHAR(36),        -- ✅ Now nullable
    ...
);
```

---

## Data Loss Warning ⚠️

**Both migrations will DELETE existing data** in the affected tables:

- **V6:** Deletes all outbox_messages, inbox_messages, and sagas
- **V21:** Deletes all subscriptions (and cascades to invoices)

This is acceptable for development environments but would require a data migration strategy for production.

---

## Manual Application (Alternative)

If the scripts don't work, you can apply migrations manually:

### Using DBeaver/pgAdmin:

1. Open your database client
2. Connect to your PostgreSQL database
3. Open and execute:
   - `regtech-app/src/main/resources/db/migration/common/V6__Migrate_outbox_messages_schema.sql`
   - `regtech-app/src/main/resources/db/migration/billing/V21__Update_subscriptions_schema.sql`
4. Update Flyway history for each migration:
   ```sql
   -- For V6
   INSERT INTO flyway_schema_history (
       installed_rank, version, description, type, script, 
       checksum, installed_by, installed_on, execution_time, success
   ) VALUES (
       (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
       '6', 'Migrate outbox messages schema', 'SQL', 
       'V6__Migrate_outbox_messages_schema.sql',
       NULL, CURRENT_USER, CURRENT_TIMESTAMP, 0, true
   );
   
   -- For V21
   INSERT INTO flyway_schema_history (
       installed_rank, version, description, type, script, 
       checksum, installed_by, installed_on, execution_time, success
   ) VALUES (
       (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
       '21', 'Update subscriptions schema', 'SQL', 
       'V21__Update_subscriptions_schema.sql',
       NULL, CURRENT_USER, CURRENT_TIMESTAMP, 0, true
   );
   ```

### Using psql:

```bash
# Set password
set PGPASSWORD=your_password

# Apply V6
psql -h localhost -p 5432 -U postgres -d regtech_db -f "regtech-app/src/main/resources/db/migration/common/V6__Migrate_outbox_messages_schema.sql"

# Apply V21
psql -h localhost -p 5432 -U postgres -d regtech_db -f "regtech-app/src/main/resources/db/migration/billing/V21__Update_subscriptions_schema.sql"

# Update Flyway history (run the INSERT statements above)
```

---

## Verification

After applying both migrations:

### 1. Check Flyway History
```sql
SELECT version, description, installed_on, success
FROM flyway_schema_history
WHERE version IN ('6', '21')
ORDER BY installed_rank;
```

Expected output:
```
version | description                      | installed_on        | success
--------|----------------------------------|---------------------|--------
6       | Migrate outbox messages schema   | 2025-12-04 ...      | t
21      | Update subscriptions schema      | 2025-12-04 ...      | t
```

### 2. Check Outbox Messages Schema
```sql
\d public.outbox_messages
```

Should show columns: id, type, content, status, occurred_on_utc, processed_on_utc, retry_count, next_retry_time, last_error, dead_letter_time, updated_at

### 3. Check Subscriptions Schema
```sql
\d billing.subscriptions
```

Should show columns: id, billing_account_id, stripe_subscription_id, tier, status, start_date, end_date, created_at, updated_at, version

### 4. Test Application
```cmd
mvnw spring-boot:run
```

Check logs for:
- ✅ No schema mismatch errors
- ✅ Application starts successfully
- ✅ All modules initialize correctly

### 5. Test User Registration

Use your API client (Postman, curl, etc.) to register a new user:
```bash
POST /api/v1/auth/register
{
  "email": "test@example.com",
  "password": "SecurePass123!",
  "bankId": "your-bank-id",
  "paymentMethodId": "pm_test_123"
}
```

Check database:
```sql
-- Check user created
SELECT * FROM iam.users WHERE email = 'test@example.com';

-- Check outbox message published
SELECT * FROM public.outbox_messages ORDER BY occurred_on_utc DESC LIMIT 1;

-- Check billing account created
SELECT * FROM billing.billing_accounts ORDER BY created_at DESC LIMIT 1;

-- Check subscription created
SELECT * FROM billing.subscriptions ORDER BY created_at DESC LIMIT 1;
```

---

## Troubleshooting

### "psql: command not found"
Install PostgreSQL client tools or use DBeaver/pgAdmin instead.

### "Connection refused"
Check that PostgreSQL is running and credentials in `.env` are correct.

### "Migration already applied"
Check `flyway_schema_history` table. If migration is already recorded but schema is wrong, you may need to manually fix the schema or delete the Flyway history entry and reapply.

### "Foreign key constraint violation"
Some tables may have foreign keys to the tables being dropped. The migrations use CASCADE to handle this, but if you have custom tables referencing these, you may need to drop them first.

---

## Files Created

### Migrations
- `regtech-app/src/main/resources/db/migration/common/V6__Migrate_outbox_messages_schema.sql`
- `regtech-app/src/main/resources/db/migration/billing/V21__Update_subscriptions_schema.sql`

### Documentation
- `APPLY_MIGRATION_NOW.md` - V6 migration guide
- `SUBSCRIPTIONS_SCHEMA_FIX.md` - V21 migration guide
- `DATABASE_SCHEMA_FIXES_REQUIRED.md` - This file (overview)
- `OUTBOX_SCHEMA_MIGRATION_FIX.md` - Technical analysis of V6
- `MIGRATION_EXECUTION_GUIDE.md` - General migration guide

### Scripts
- `apply-v6-migration.bat` - Windows batch script for V6
- `apply-v6-migration.ps1` - PowerShell script for V6
- `apply-v6-migration.sh` - Linux/Mac script for V6
- `apply-v21-migration.bat` - Windows batch script for V21
- `apply-v21-migration.ps1` - PowerShell script for V21

### Verification
- `verify-event-tables-schema.sql` - SQL queries to verify schemas

---

## Summary

You have **two critical schema mismatches** that must be fixed:

1. **V6 Migration** - Fixes outbox_messages, inbox_messages, sagas tables
2. **V21 Migration** - Fixes subscriptions table

**Quick Fix (Windows):**
```cmd
apply-v6-migration.bat
apply-v21-migration.bat
mvnw spring-boot:run
```

Both migrations are ready to apply. Choose your preferred method (scripts or manual SQL) and apply them in order.
