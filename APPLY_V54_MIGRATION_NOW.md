# Apply V54 Migration - Add version Column for Optimistic Locking

## Overview

This migration adds the `version` column to the `riskcalculation.batches` table to enable optimistic locking for the BatchEntity. This prevents concurrent modification conflicts when multiple processes attempt to update the same batch record simultaneously.

## Migration Details

**Migration File**: `regtech-app/src/main/resources/db/migration/riskcalculation/V54__Add_version_to_batches.sql`

**Changes**:
1. Adds `version BIGINT` column to `riskcalculation.batches` table
2. Sets default value to 0 for existing rows
3. Adds column comment documenting its purpose for optimistic locking

**Requirements**: JPA Optimistic Locking Implementation

## Prerequisites

Before running this migration, ensure:

1. **PostgreSQL is running** and accessible
2. **Database connection** is properly configured in `regtech-app/src/main/resources/application.yml`
3. **Flyway is configured** and has successfully run previous migrations (V50-V53)
4. You have **database admin privileges** to alter tables
5. **BatchEntity has been updated** with `@Version` annotation (already completed)

## How to Apply

### Option 1: Using PowerShell Script (Recommended for Windows)

```powershell
.\apply-v54-migration.ps1
```

### Option 2: Using Batch Script (Windows)

```cmd
apply-v54-migration.bat
```

### Option 3: Using Maven Directly

```bash
mvn flyway:migrate -Dflyway.schemas=riskcalculation
```

### Option 4: Manual SQL Execution

If you prefer to run the migration manually:

```sql
-- Connect to your PostgreSQL database
\c your_database_name

-- Run the migration
ALTER TABLE riskcalculation.batches
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN riskcalculation.batches.version IS 'Version field for optimistic locking';
```

## Verification

After applying the migration, verify it was successful:

### 1. Check Flyway Schema History

```sql
SELECT version, description, installed_on, success
FROM flyway_schema_history
WHERE version = '54';
```

Expected result:
```
version | description                  | installed_on        | success
--------|------------------------------|---------------------|--------
54      | Add version to batches       | 2024-12-11 ...      | true
```

### 2. Verify Column Exists

```sql
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'riskcalculation'
  AND table_name = 'batches'
  AND column_name = 'version';
```

Expected result:
```
column_name | data_type | is_nullable | column_default
------------|-----------|-------------|---------------
version     | bigint    | NO          | 0
```

### 3. Verify Entity Configuration

Ensure the BatchEntity.java has been updated with:

```java
@Version
@Column(name = "version", nullable = false)
private Long version;
```

And in the `@PrePersist` method:
```java
version = 0L;
```

## Rollback (If Needed)

If you need to rollback this migration:

```sql
-- Remove the version column
ALTER TABLE riskcalculation.batches DROP COLUMN version;

-- Update flyway_schema_history to mark as undone
UPDATE flyway_schema_history
SET success = false
WHERE version = '54';
```

**Note**: Rollback should only be performed if no optimistic locking has occurred yet.

## Impact

- **Performance**: Minimal impact, version field is lightweight
- **Concurrency**: Prevents lost updates in concurrent batch processing scenarios
- **Compatibility**: Fully backward compatible, existing code continues to work
- **Storage**: Adds 8 bytes per row to the batches table</content>
<parameter name="filePath">C:\Users\alseny\Desktop\react projects\regtech\APPLY_V54_MIGRATION_NOW.md